package sms.receiver.service.callreview;

import com.google.common.collect.ImmutableList;
import io.crm.pipelines.transformation.JsonTransformationPipeline;
import io.crm.pipelines.transformation.Transform;
import io.crm.pipelines.transformation.impl.json.object.ConverterTransformation;
import io.crm.pipelines.transformation.impl.json.object.PlainArrayToObject;
import io.crm.pipelines.transformation.impl.json.object.StringTrimmer;
import io.crm.pipelines.validator.ValidationPipeline;
import io.crm.pipelines.validator.ValidationResult;
import io.crm.pipelines.validator.Validator;
import io.crm.pipelines.validator.composer.JsonObjectValidatorComposer;
import io.crm.promise.Promises;
import io.crm.util.Util;
import io.crm.web.util.Convert;
import io.crm.web.util.Converters;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import sms.receiver.App;
import sms.receiver.MyEvents;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by shahadat on 3/8/16.
 */
public class SmsTransformAndValidate {
    private static final java.lang.String SMS_FORMAT = "sms_format";
    private static final java.lang.String ORIGINATOR = "originator";
    private static final java.lang.String VALIDATIN_ERROR = "VALIDATIN_ERROR";
    private static final String RECIPIENT = "recipient";
    private static final java.lang.String TEXT = "text";
    private static final String VALIDATION_RESULTS = "validationResults";
    private static final java.lang.String DELIMITER = "sms_delimiter";
    private final JsonTransformationPipeline transformationPipeline;
    private final ValidationPipeline<JsonObject> validationPipeline;
    private final PlainArrayToObject plainArrayToObject;
    private final Vertx vertx;

    {

        List<Map<String, Object>> list = App.loadConfig().getJsonArray(SMS_FORMAT, Util.EMPTY_JSON_ARRAY).getList();

        plainArrayToObject = new PlainArrayToObject(list.stream()
            .map(m -> m.keySet().stream().findFirst().get()).collect(Collectors.toList()));


        HashMap<String, Function<Object, Object>> map = new HashMap<>();
        list.stream().map(m -> m.entrySet().stream().findAny().get())
            .forEach(e -> map.put(e.getKey(),
                ValueTypes.valueOf(e.getValue().toString().toUpperCase()).converter));


        ConverterTransformation converterTransformation = new ConverterTransformation(map);

        ImmutableList.Builder<Transform<JsonObject, JsonObject>> builder = ImmutableList.<Transform<JsonObject, JsonObject>>builder();
        builder
            .add(new StringTrimmer(null, null))
            .add(converterTransformation);
        transformationPipeline = new JsonTransformationPipeline(builder.build());
    }

    {
//        List<Map<String, Object>> list = App.loadConfig().getJsonArray(SMS_FORMAT, Util.EMPTY_JSON_ARRAY).getList();

        List<Validator<JsonObject>> validatorList = new JsonObjectValidatorComposer(new ArrayList<>(), App.getMessageBundle())
            .field("BR_NAME",
                fieldValidatorComposer -> fieldValidatorComposer.notNullEmptyOrWhiteSpace())
            .field("MOBILE",
                fieldValidatorComposer1 -> fieldValidatorComposer1.notNullEmptyOrWhiteSpace().phone())
            .field("AGE",
                fieldValidatorComposer2 -> fieldValidatorComposer2.integerType().range(18, 100))
            .getValidatorList();

        validationPipeline = new ValidationPipeline<>(validatorList);
    }

    public SmsTransformAndValidate(Vertx vertx) {
        this.vertx = vertx;
    }

    public JsonObject processSms(JsonObject sms) {
        return Promises.from(parse(sms))
            .map(json -> {

                {
                    List<ValidationResult> validationResults = validationPipeline.validate(json);

                    if (validationResults != null && validationResults.size() > 0) {

                        Util.publish(vertx.eventBus(), MyEvents.VALIDATION_FAILED,
                            new JsonArray(validationResults));

                        return
                            new JsonObject()
                                .put(RECIPIENT, json.getString(ORIGINATOR))
                                .put(TEXT, validationErrorMessage(json, validationResults))
                            ;
                    }
                }


                {
                    Util.publish(vertx.eventBus(), MyEvents.VALIDATION_SUCCESSFULL, json);

                    return
                        new JsonObject()
                            .put(RECIPIENT, json.getString(ORIGINATOR))
                            .put(TEXT, validationSuccessMessage(sms.getString(TEXT)))
                        ;
                }

            }).getOrElse(Util.EMPTY_JSON_OBJECT);
    }

    private String validationSuccessMessage(String text) {
//        return App.getMessageBundle().translate(MyRespCodes.SMS_PROCESS_SUCCESSFULL.messageCode(), json);
        return "Your sms submit successful. Your sms: " + text;
    }

    private String validationErrorMessage(JsonObject json, List<ValidationResult> validationResults) {

        String msg = "Your sms submit failed. " + "\n" + "Errors: ";

        return msg + validationResults.stream()
            .map(v -> App.getMessageBundle().translate(App.messageCode(v.getErrorCode()), v.toJson()))
            .collect(Collectors.joining("\n"));
    }

    private JsonObject parse(JsonObject sms) {
        JsonObject json = new JsonObject();
        json.put(ORIGINATOR, sms.getString(ORIGINATOR));

        String string = sms.getString(TEXT);

        String delimiter = App.loadConfig().getString(DELIMITER, "*");

        String[] split = string.split("\\" + delimiter + "");

//        List<Map<String, Object>> list = App.loadConfig().getJsonArray(SMS_FORMAT, Util.EMPTY_JSON_ARRAY).getList();

        return Promises.from(Arrays.asList(split))
            .map(splt -> plainArrayToObject.transform(new JsonArray(splt)))
            .map(jsn -> jsn.put(ORIGINATOR, sms.getString(ORIGINATOR)))
            .map(jsn -> transformationPipeline.transform(jsn)).get()
            ;
    }

    public static void main(String... args) {
//        JsonObject jsonObject = new SmsTransformAndValidate(Vertx.vertx()).processSms(
//            new JsonObject()
//                .put("originator", "01951883412")
//                .put("text", "Sohan Ahmed*01951883412asdfsadf*45*15-May-2015*Yes")
//        );
//        System.out.println(jsonObject);

        Observable<Integer> range = Observable.range(1, 10);

        PublishSubject<Integer> subject = PublishSubject.create();

        subject
            .map(new Func1<Integer, Integer>() {
                private int c = 1;

                @Override
                public Integer call(Integer s) {
                    if (s == 5 && c < 5) {
                        System.out.println("THROWING EXCEPTION: " + c++);
                        throw new RuntimeException("OK");
                    } else return s;
                }
            })
            .map(s -> s * 10)
            .map(s -> s)
            .doOnNext(System.out::println)
            .count()
//            .onErrorResumeNext(
//                range
//                    .map(s -> s * 10)
//                    .map(s -> s)
//                    .count())
            .retry(10)
            .subscribe(s -> System.out.println("ON_NEXT: " + s), e -> System.err.println("ON_ERROR: " + e))
        ;

        range.subscribe(subject);

//        range.subscribe(s -> System.out.println("ON_NEXT_2: " + s), e -> System.err.println("ON_ERROR_2: " + e));
    }

    enum ValueTypes {
        S(s -> s),
        N(val -> Double.parseDouble(val.toString())),
        I(val -> new Double(val.toString()).intValue()),
        D(val -> Double.parseDouble(val.toString())),
        B(val -> Util.apply(val.toString().toUpperCase(), s -> s.equals("Y") || s.equals("YES") ? true : false)),
        DT(val -> Util.toIsoString(Util.parseDate(val.toString())));


        private final Function<Object, Object> converter;

        ValueTypes(Function<Object, Object> converter) {
            this.converter = converter;
        }
    }

}
