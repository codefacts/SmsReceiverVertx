package sms.receiver;

import io.crm.FailureCodes;
import io.crm.MessageBundle;
import io.crm.util.ExceptionUtil;
import io.crm.util.Util;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by shahadat on 3/7/16.
 */
public class App {
    public static final String MESSAGE_BUNDLE_JSON = "message-bundle.json";
    private static final JsonObject CONFIG = Util.loadConfig(App.class);
    private static final MessageBundle MESSAGE_BUNDLE = new MessageBundle(
        Util.searchFor(MESSAGE_BUNDLE_JSON, App.class));

    private static final Map<Integer, String> MESSAGE_CODES;

    public static void main(String[] args) {
        Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(10 * 60 * 1000)).deployVerticle(new MainVerticle());
    }

    public static JsonObject loadConfig() {
        return CONFIG;
    }

    public static MessageBundle getMessageBundle() {
        return MESSAGE_BUNDLE;
    }

    public static String messageCode(int errorCode) {
        return MESSAGE_CODES.get(errorCode);
    }

    static {
        MESSAGE_CODES = Arrays.asList(FailureCodes.values())
            .stream().collect(Collectors.toMap(v -> v.code(), v -> v.messageCode()));
        MESSAGE_CODES.putAll(Arrays.asList(MyRespCodes.values())
            .stream().collect(Collectors.toMap(v -> v.code(), v -> v.messageCode())));
    }
}
