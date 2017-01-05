package cc.ble.bledemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import cc.ble.bledemo.module.Csc;
import cc.ble.bledemo.module.imp.CscCallbacks;

public class MainActivity extends AppCompatActivity {
    String cscAddress = "FD:48:A3:59:AC:CF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startDevice();
    }

    void startDevice() {
        Csc csc = new Csc(this, cscAddress);
        csc.setBleCallback(new CscCallbacks() {
            @Override
            public void OnSpeed(float speed, int wheelTime, long wheelRevolution) {

            }

            @Override
            public void OnCrank(float crank, int crankTime, int crankRevolution, boolean isRePeatData) {

            }

            @Override
            public void onConnected() {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onDeviceReady() {

            }
        });
        csc.start();
    }
}
