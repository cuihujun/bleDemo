package cc.ble.bledemo.module.imp;

public interface CscCallbacks extends BleCallback {
    void OnSpeed(float speed, int wheelTime, long wheelRevolution);

    void OnCrank(float crank, int crankTime, int crankRevolution, boolean isRePeatData);
}