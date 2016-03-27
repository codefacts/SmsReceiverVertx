package sms.receiver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sms.receiver.service.*;
import sms.receiver.service.callreview.SmsServiceCR;
import sms.receiver.service.callreview.SmsTransformAndValidate;

/**
 * Created by shahadat on 3/7/16.
 */
public class MainVerticle extends AbstractVerticle {
    public static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
    private final SmsServerLifecycle smsServerLifecycle = new SmsServerLifecycle();

    @Override
    public void start() throws Exception {

        final JDBCClient jdbcClient = JDBCClient.createShared(vertx, App.loadConfig().getJsonObject("database"));

        createTablesIfNotExists(jdbcClient);

        smsServerLifecycle.startSmsServer();

        new SmsService(vertx).start();

        registerEventHandlers(jdbcClient);
    }

    private void createTablesIfNotExists(JDBCClient jdbcClient) {
        new DbInitializer().createTables(jdbcClient)
            .then(v -> LOGGER.info("DB Initialized successfully."))
            .error(e -> LOGGER.error("FAILED: DB initialization", e));
    }

    private void registerEventHandlers(JDBCClient jdbcClient) {
        EventBus bus = vertx.eventBus();

        SmsTransformAndValidate smsTransformAndValidate = new SmsTransformAndValidate(vertx);
        SmsProcessor smsProcessor = new SmsProcessor(jdbcClient, smsTransformAndValidate);
        bus.consumer(MyEvents.SMS_RECEIVED, smsProcessor::smsReceived);
        bus.consumer(MyEvents.SMS_DELETED, smsProcessor::smsDeleted);
        bus.consumer(MyEvents.SMS_SENT, smsProcessor::smsSent);

        SmsServiceCR smsServiceCR = new SmsServiceCR();
        bus.consumer(MyEvents.VALIDATION_SUCCESSFULL, smsServiceCR::validationSuccessfull);
        bus.consumer(MyEvents.VALIDATION_FAILED, smsServiceCR::validationFailed);
    }

    @Override
    public void stop() throws Exception {
        if (smsServerLifecycle != null) smsServerLifecycle.stop();
    }
}
