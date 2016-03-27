package sms.receiver.service;

import io.crm.promise.intfs.Promise;
import io.crm.util.ExceptionUtil;
import io.crm.web.util.WebUtils;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;
import sms.receiver.service.callreview.SmsTransformAndValidate;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by shahadat on 3/7/16.
 */
public class SmsProcessor {
    public static final Logger LOGGER = LoggerFactory.getLogger(SmsProcessor.class);
    private final JDBCClient jdbcClient;
    private final SmsTransformAndValidate smsTransformAndValidate;

    public SmsProcessor(JDBCClient jdbcClient, SmsTransformAndValidate smsTransformAndValidate) {
        this.jdbcClient = jdbcClient;
        this.smsTransformAndValidate = smsTransformAndValidate;
    }

    public void smsReceived(Message<JsonObject> message) {
        JsonObject sms = message.body();
        save(sms)
            .error(e -> LOGGER.error("ERROR INSERTING INTO SMS_INBOX: " + sms, e))
            .complete(p -> {
                JsonObject jsonObject = smsTransformAndValidate.processSms(sms);
                message.reply(jsonObject);
            })
            .error(e -> ExceptionUtil.fail(message, e))
        ;
    }

    private Promise<UpdateResult> save(JsonObject sms) {
        String fields = sms.getMap().keySet().stream().collect(Collectors.joining(", "));
        String quetes = sms.stream().map(v -> "?").collect(Collectors.joining(", "));
        return WebUtils.update("insert into sms_inbox (" + fields + ") values (" + quetes + ")", jdbcClient);
    }

    public void smsDeleted(Message<JsonObject> message) {
        LOGGER.info("DELETED: " + message.body().encode());
    }

    public void smsSent(Message<JsonObject> message) {
        LOGGER.info("SENT: " + message.body().encode());
    }

    private static Observable<String> chain(Observable<String> observable) {
        return observable
            .map(s -> {
                if ("3".equals(s)) throw new RuntimeException("OK");
                return s;
            });
    }

    public static void main(String... args) {
        Observable<String> observable = Observable.from(Arrays.asList("1", "2", "3", "4", "5"));
        PublishSubject<String> subject = PublishSubject.create();

        observable.subscribe(subject);

        chain(subject)
            .onErrorResumeNext(chain(subject))
            .subscribe(
                count -> System.out.println("COuNT: " + count),
                e -> System.err.println("ERROR: " + e.getMessage()))
        ;
        observable.subscribe(s -> {
        }, e -> System.err.println("CAUGHT: " + e));
    }
}



























