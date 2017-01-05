package cc.ble.bledemo.module.imp;

public interface BleCallback {
    void onConnected();

    void onDisconnected();

    void onDeviceReady();
}