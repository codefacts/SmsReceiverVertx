package sms.receiver;

import static sms.receiver.Helper.validation;
import static sms.receiver.Helper.validationHttp;

/**
 * Created by shahadat on 3/8/16.
 */
public enum MyRespCodes {

    SMS_PROCESS_SUCCESSFULL(Helper.success(), "sms.process.success", Helper.successHttp()),
    INVALID_SMS_VALIDATION_ERROR(validation(), "invalid.sms.validation.error", validationHttp());

    private final int code;
    private final String messageCode;
    private final int httpResponseCode;

    MyRespCodes(int code, String messageCode, int httpResponseCode) {
        this.code = code;
        this.messageCode = messageCode;
        this.httpResponseCode = httpResponseCode;
    }

    public int code() {
        return code;
    }

    public String messageCode() {
        return messageCode;
    }

    public int httpResponseCode() {
        return httpResponseCode;
    }
}

class Helper {
    static private int validation = 300010000;
    static private int success = 200010000;
    final static private int validationHttp = 300;
    final static private int successHttp = 200;

    static int validation() {
        return validation++;
    }

    static int validationHttp() {
        return validationHttp;
    }

    public static int success() {
        return success++;
    }

    public static int successHttp() {
        return successHttp;
    }
}
