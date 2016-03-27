package sms.receiver.service.callreview;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

/**
 * Created by shahadat on 3/9/16.
 */
public class SmsServiceCR {

    public void validationSuccessfull(Message<JsonObject> message) {

    }

    public void validationFailed(Message<JsonObject> message) {

    }

    public static void main(String... args) throws Exception {

        final int maxNum = 5;

        Observable<Integer> generator = Observable.range(1, maxNum).doOnNext(v -> System.out.println(
            Thread.currentThread().getName() + ": Generated:    " + v));

        Observable<Integer> shiftedUp = generator.subscribeOn(Schedulers.computation()).map(v -> v).doOnNext(v -> System.out.println(
            Thread.currentThread().getName() + ": Shifted Up:   " + v));

        Observable<Integer> shiftedDn = shiftedUp.observeOn(Schedulers.io()).map(v -> v).doOnNext(v -> System.out.println("" +
            Thread.currentThread().getName() + ": Shifted Dn:   " + v));

        new Thread(() -> {

            shiftedDn.subscribe(v -> System.out.println(
                Thread.currentThread().getName() + ": RECEIVED:     " + v + "\n"));

        }).start();

        Thread.sleep(5 * 1000);
    }
}
