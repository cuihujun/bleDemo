package cc.ble.bledemo.module;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.polidea.rxandroidble.RxBleDeviceServices;

import java.util.List;
import java.util.UUID;

import cc.ble.bledemo.module.imp.CscCallbacks;


public class Csc extends BleDevice<CscCallbacks> {
    private static final String TAG = "Csc";
    public int wheelPerimeter = 1123;// mm
    private Long lastWheelRevolutions = null;
    private Integer lastWheelEventTime = null;
    private Integer lastCrankCount = null;
    private Integer lastCrankTime = null;
    public static final UUID UUID_SERVICE_CSC = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_C_CSC = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic mCscCharacteristic;

    public Csc(Context context, String macAddress) {
        super(context, macAddress);
    }

    @Override
    protected void onDiscoverServices(RxBleDeviceServices services) {
        BluetoothGattService service = getBluetoothGattService(services, UUID_SERVICE_CSC);
        if (service != null) {
            mCscCharacteristic = service.getCharacteristic(UUID_C_CSC);
        }
        addNotify(UUID_C_CSC);

        super.onDiscoverServices(services);
    }

    public static BluetoothGattService getBluetoothGattService(RxBleDeviceServices services, UUID uuid) {
        List<BluetoothGattService> bluetoothGattServices = services.getBluetoothGattServices();
        for (BluetoothGattService service : bluetoothGattServices) {
            if (service.getUuid().equals(uuid)) {
                return service;
            }
        }
        return null;
    }

    @Override
    protected void onNotificationReceived(UUID uuid, byte[] bytes) {
        if (mCscCharacteristic != null) {
            mCscCharacteristic.setValue(bytes);
            try {
                dealData(mCscCharacteristic);
            } catch (Exception e) {
                Log.e(TAG, "onNotificationReceived: ", e);
            }
        }
    }

    private void dealData(BluetoothGattCharacteristic ch) {
        if (ch == null || ch.getValue() == null || ch.getValue().length == 0) {
            return;
        }

        Float Speed = null;
        Float Crank = null;

        int offset = 0;
        int flag = ch.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        offset++;

        Long CumulativeWheelRevolutions = null;
        Integer WheelEventTime = null;
        if ((flag & 0x01) != 0) {
            CumulativeWheelRevolutions = ch.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT32,
                    offset) & 0x00000000ffffffffL;
            offset += 4;

            WheelEventTime = ch.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT16,
                    offset);
            offset += 2;

            if (lastWheelRevolutions != null
                    && lastWheelEventTime != null
                    && (!WheelEventTime.equals(lastWheelEventTime))) {
                Speed = (getRec(CumulativeWheelRevolutions,
                        lastWheelRevolutions) * (wheelPerimeter / 1000f))
                        / (getRec(WheelEventTime, lastWheelEventTime) / 1024f)
                        * 3.6f;
            } else {
                Speed = 0f;
            }
            Log.i("KKKK", "speed = " + Speed);
            if (mBleCallback != null)
                mBleCallback.OnSpeed(Speed, WheelEventTime, CumulativeWheelRevolutions);

            lastWheelRevolutions = CumulativeWheelRevolutions;
            lastWheelEventTime = WheelEventTime;
        }

        Integer CumulativeCrankRevolutions = null;
        Integer CrankEventTime = null;
        if ((flag & 0x02) != 0) {
            CumulativeCrankRevolutions = ch.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT16,
                    offset);
            offset += 2;

            CrankEventTime = ch.getIntValue(
                    BluetoothGattCharacteristic.FORMAT_UINT16,
                    offset);
            offset += 2;

            boolean isRePeatData = false;
            if (lastCrankTime != null && CrankEventTime.intValue() == lastCrankTime.intValue()) {
                isRePeatData = true;
            }

            if (lastCrankCount != null && lastCrankTime != null
                    && (CrankEventTime.intValue() != lastCrankTime.intValue())) {
                Crank = 1f * getRec(CumulativeCrankRevolutions,
                        lastCrankCount)
                        / (getRec(CrankEventTime, lastCrankTime) / 1024f / 60f);
            } else {
                Crank = 0f;
            }

            if (mBleCallback != null)
                mBleCallback.OnCrank(Crank, CrankEventTime, CumulativeCrankRevolutions, isRePeatData);

            lastCrankCount = CumulativeCrankRevolutions;
            lastCrankTime = CrankEventTime;
        }
    }

    private long getRec(Long t, Long lastT) {
        return t >= lastT ? (t - lastT) : (t - lastT + 4294967296L);
    }

    private int getRec(Integer t, Integer lastT) {
        return t >= lastT ? (t - lastT) : (t - lastT + 65536);
    }
}
