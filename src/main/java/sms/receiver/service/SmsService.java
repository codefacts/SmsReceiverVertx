package sms.receiver.service;

import io.crm.promise.Promises;
import io.crm.promise.intfs.Defer;
import io.crm.promise.intfs.Promise;
import io.crm.util.SimpleCounter;
import io.crm.util.Util;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smslib.*;
import org.smslib.crypto.AESKey;
import org.smslib.modem.SerialModemGateway;
import sms.receiver.App;
import sms.receiver.MyEvents;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by shahadat on 3/7/16.
 */
public class SmsService {
    public static final Logger LOGGER = LoggerFactory.getLogger(SmsService.class);
    private static final InboundMessage[] EMPTY_ARRAY = new InboundMessage[]{};

    public static final String ACTION = "ACTION";
    public static final String SEND = "SEND";

    private final Vertx vertx;

    public SmsService(Vertx vertx) {
        this.vertx = vertx;
    }

    public SmsService start() {
        vertx.setTimer(1, tid -> loop());
        return this;
    }

    public void loop() {

        InboundMessage[] messages = EMPTY_ARRAY;

        try {

            messages = read();

        } catch (Exception ex) {
            LOGGER.error("ERROR READING MESSAGE.", ex);
        }

        if (messages.length == 0) {
            vertx.setTimer(1, tid -> loop());
        } else {
            processMessages(messages)
                .complete(p -> vertx.setTimer(1, tid -> loop()));
        }
    }

    private Promise<List<Promise<InboundMessage>>> processMessages(InboundMessage[] messages) {

        return Promises.from(messages)
            .map(this::toJson)
            .mapToPromise(list -> Util.<JsonObject>sendAllAndWaitAll(vertx.eventBus(), MyEvents.SMS_RECEIVED, list)
                .then(v -> {
                    try {
                        dispatch(v);
                    } catch (Exception ex) {
                        LOGGER.error("ERROR IN SMS REPLY DISPATCHING", ex);
                    }
                })
                .mapToPromise(p -> Promises.allComplete(delete(messages, list))));
    }

    private void dispatch(List<Promise<Message<JsonObject>>> promiseList) {
        List<JsonObject> list = promiseList.stream()
            .map(p -> p.get())
            .filter(m -> m.headers().get(ACTION).equals(SEND))
            .map(Message::body)
            .collect(Collectors.toList());
        send(list);
    }

    private List<JsonObject> toJson(InboundMessage[] messages) {
        return Arrays.asList(messages).stream()
            .map(msg -> toJson(msg))
            .collect(Collectors.toList());
    }

    private JsonObject toJson(InboundMessage msg) {
        return
            new JsonObject()
                .put("endsWithMultiChar", msg.getEndsWithMultiChar())
                .put("memIndex", msg.getMemIndex())
                .put("memLocation", msg.getMemLocation())
                .put("mpMaxNo", msg.getMpMaxNo())
                .put("mpMemIndex", msg.getMpMemIndex())
                .put("mpRefNo", msg.getMpRefNo())
                .put("mpSeqNo", msg.getMpSeqNo())
                .put("originator", msg.getOriginator())
                .put("pduUserData", msg.getPduUserData())
                .put("pduUserDataHeader", msg.getPduUserDataHeader())
                .put("smscNumber", msg.getSmscNumber())
                .put("date", msg.getDate())
                .put("DCSMessageClass", msg.getDCSMessageClass().name())
                .put("dstPort", msg.getDstPort())
                .put("encoding", msg.getEncoding().name())
                .put("gatewayId", msg.getGatewayId())
                .put("msgId", msg.getId())
                .put("messageId", msg.getMessageId())
                .put("srcPort", msg.getSrcPort())
                .put("text", msg.getText())
                .put("type", msg.getType().name())
                .put("uuid", msg.getUuid())
            ;
    }

    private void send(List<JsonObject> list) {
        List<OutboundMessage> messages = list.stream().map(js -> {
            OutboundMessage message = new OutboundMessage();
            message.setRecipient(js.getString("recipient"));
            message.setText(js.getString("text"));
            return message;
        }).collect(Collectors.toList());

        sendMessages(messages, list)
            .then(msgList -> {
                SimpleCounter counter = new SimpleCounter(0);
                msgList.forEach(msg -> Util.send(vertx.eventBus(), MyEvents.SMS_SENT, list.get(counter.counter++)));
            });

    }

    private Promise<List<OutboundMessage>> sendMessages(List<OutboundMessage> messages, List<JsonObject> list) {

        Defer<List<OutboundMessage>> defer = Promises.defer();

        vertx.setTimer(1, tid -> {
            try {
                Service.getInstance().sendMessages(messages);
                defer.complete(messages);
            } catch (TimeoutException | InterruptedException | IOException | GatewayException e) {

                LOGGER.error("ERROR SENDING MESSAGES: " + list, e);

                vertx.setTimer(1, tid1 -> {

                    try {
                        Service.getInstance().sendMessages(messages);
                        defer.complete(messages);
                    } catch (TimeoutException | InterruptedException | IOException | GatewayException e1) {

                        LOGGER.error("ERROR SENDING RETRY(1) MESSAGES: " + list, e1);

                        vertx.setTimer(1, tid2 -> {

                            try {
                                Service.getInstance().sendMessages(messages);
                                defer.complete(messages);
                            } catch (TimeoutException | InterruptedException | IOException | GatewayException e2) {

                                LOGGER.error("ERROR SENDING RETRY(2) MESSAGES: " + list, e2);

                                vertx.setTimer(1, tid3 -> {

                                    try {
                                        Service.getInstance().sendMessages(messages);
                                        defer.complete(messages);
                                    } catch (TimeoutException | InterruptedException | IOException | GatewayException e3) {

                                        LOGGER.error("ERROR SENDING RETRY(3) MESSAGES: " + list, e3);
                                        defer.fail(e3);
                                    }

                                });
                            }

                        });
                    }

                });
            }
        });

        return defer.promise();
    }

    private List<Promise<InboundMessage>> delete(InboundMessage[] messages, List<JsonObject> list) {
        SimpleCounter counter = new SimpleCounter(0);

        List<Promise<InboundMessage>> promiseList = new ArrayList<>();

        Arrays.asList(messages).forEach(msg -> {
            final JsonObject jsonMsg = list.get(counter.counter++);

            Promise<InboundMessage> promise = delete(msg)
                .then(m -> Util.publish(vertx.eventBus(), MyEvents.SMS_DELETED, jsonMsg));

            promiseList.add(promise);
        });

        return promiseList;
    }

    private Promise<InboundMessage> delete(InboundMessage msg) {
        Defer<InboundMessage> defer = Promises.defer();
        vertx.setTimer(1, tid -> {
            try {
                Service.getInstance().deleteMessage(msg);
                defer.complete(msg);
            } catch (TimeoutException | InterruptedException | IOException | GatewayException e) {
                LOGGER.error("ERROR DELETING MESSAGE: " + msg.getText(), e);


                vertx.setTimer(1, tid1 -> {

                    try {
                        Service.getInstance().deleteMessage(msg);
                        defer.complete(msg);
                    } catch (TimeoutException | InterruptedException | IOException | GatewayException e1) {
                        LOGGER.error("ERROR RETRYING (1) DELETING MESSAGE: " + msg.getText(), e1);


                        vertx.setTimer(1, tid2 -> {

                            try {
                                Service.getInstance().deleteMessage(msg);
                                defer.complete(msg);
                            } catch (TimeoutException | InterruptedException | IOException | GatewayException e2) {
                                LOGGER.error("ERROR RETRYING (2) DELETING MESSAGE: " + msg.getText(), e2);

                                vertx.setTimer(1, tid3 -> {

                                    try {
                                        Service.getInstance().deleteMessage(msg);
                                        defer.complete(msg);
                                    } catch (TimeoutException | InterruptedException | IOException | GatewayException e3) {
                                        LOGGER.error("ERROR RETRYING (3) DELETING MESSAGE: " + msg.getText(), e3);
                                        defer.fail(e3);
                                    }

                                });

                            }

                        });

                    }

                });

            }
        });
        return defer.promise();
    }

    private InboundMessage[] read() throws InterruptedException, TimeoutException, GatewayException, IOException {
        InboundMessage[] messages = Service.getInstance().readMessages(InboundMessage.MessageClasses.UNREAD);
        return messages;
    }
}
