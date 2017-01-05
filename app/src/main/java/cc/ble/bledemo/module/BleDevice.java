package cc.ble.bledemo.module;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.util.Log;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cc.ble.bledemo.module.imp.BleCallback;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;


public class BleDevice<T extends BleCallback> {
    protected T mBleCallback;

    private static final String TAG = "BleDevice";

    private Observable<RxBleConnection> mConnectionObservable;

    public BleDevice(Context context, String macAddress) {
        Context mContext = context.getApplicationContext();

        RxBleDevice mRxBleDevice = RxBleClient.create(mContext).getBleDevice(macAddress);

        mConnectionObservable = mRxBleDevice
                .establishConnection(context, false)
                .subscribeOn(AndroidSchedulers.mainThread())
                .compose(new ConnectionSharingAdapter());

        mRxBleDevice.observeConnectionStateChanges()
                .subscribe(newState -> {
                    if (newState == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
                        Log.e(TAG, "state ---->: DISCONNECTED");
                    } else if (newState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                        Log.e(TAG, "state ---->: CONNECTED");
                    } else if (newState == RxBleConnection.RxBleConnectionState.CONNECTING) {
                        Log.e(TAG, "state ---->: CONNECTING");
                    } else if (newState == RxBleConnection.RxBleConnectionState.DISCONNECTING) {
                        Log.e(TAG, "state ---->: DISCONNECTING");
                    } else {
                        Log.e(TAG, "state ---->: other");
                    }
                });
    }

    public BleDevice setBleCallback(T mBleCallback) {
        this.mBleCallback = mBleCallback;
        return this;
    }

    /**
     * 启动
     */
    public final void start() {
        Log.i(TAG, "start: ");
        mConnectionObservable
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        Log.w(TAG, "call: start connect");
                    }
                })
                .doOnNext(rxBleConnection -> Log.w(TAG, "call: Connct Sucess"))
                .doOnNext(rxBleConnection -> Log.i(TAG, "call: start find service"))
                .compose(timeoutJustFirstEmit(20, TimeUnit.SECONDS))
                .flatMap(RxBleConnection::discoverServices)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(this::onDiscoverServices)
                .retryWhen(errors -> errors.flatMap((Func1<Throwable, Observable<?>>) error -> Observable.timer(5, TimeUnit.SECONDS)))
                .subscribe(rxBleConnection -> Log.i(TAG, "success"),
                        throwable -> Log.e(TAG, "failed" + throwable.getMessage(), throwable));
    }

    public <T> Observable.Transformer<T, T> timeoutJustFirstEmit(int timeout, TimeUnit timeUnit) {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> observable) {
                return observable
                        .timeout(() -> Observable.timer(timeout, timeUnit)
                                .cast(Object.class), aLong -> Observable.never())
                        .subscribeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    @CallSuper
    protected void onDiscoverServices(RxBleDeviceServices services) {

    }

    protected void addNotify(UUID uuid) {
        startNotifySubscription(uuid);
    }

    private Subscription startNotifySubscription(UUID uuid) {
        return mConnectionObservable
                .doOnSubscribe(() -> Log.w(TAG, "call: doOnSubscribe"))
                .subscribeOn(AndroidSchedulers.mainThread())
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(uuid))
                .doOnNext(observable -> notificationHasBeenSetUp(uuid, observable))
                .flatMap(notificationObservable -> notificationObservable)
                .subscribe(bytes -> onNotificationReceived(uuid, bytes), this::onNotificationSetupFailure);
    }


    public void notificationHasBeenSetUp(UUID uuid, Observable<byte[]> observable) {
        Log.i(TAG, "notificationHasBeenSetUp: -------------------------------------------------");
    }


    protected void onNotificationReceived(UUID uuid, byte[] bytes) {
        Log.i(TAG, "onNotificationReceived: " + "Change: ");
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        Log.e(TAG, "onNotificationSetupFailure: ###############################################", throwable);
    }
}
