package com.accelerometer.app.sdk.witmotion;

import com.accelerometer.app.sdk.witmotion.components.Bwt901bleProcessor;
import com.accelerometer.app.sdk.witmotion.components.Bwt901bleResolver;
import com.accelerometer.app.sdk.witmotion.interfaces.IBwt901bleRecordObserver;
import com.accelerometer.app.sdk.witmotion.interfaces.IListenKeyUpdateObserver;
import com.accelerometer.app.sdk.witmotion.interfaces.IAttitudeSensorApi;
import com.accelerometer.app.sdk.witmotion.model.DeviceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Обёртка для BLE-датчика WitMotion (адаптирована из примеров производителя).
 */
public class Bwt901ble implements IListenKeyUpdateObserver, IAttitudeSensorApi {

    private final DeviceModel deviceModel;
    private final BluetoothBLE bluetoothBLE;
    private final List<IBwt901bleRecordObserver> recordObservers = new ArrayList<>();

    public Bwt901ble(MockBluetoothDevice bluetoothDevice) {
        DeviceModel model = new DeviceModel(
                bluetoothDevice.getName() + "(" + bluetoothDevice.getMac() + ")",
                new Bwt901bleResolver(),
                new Bwt901bleProcessor(),
                "61_0"
        );
        model.setDeviceData("Mac", bluetoothDevice.getMac());

        this.deviceModel = model;
        this.bluetoothBLE = bluetoothDevice;
    }

    public void open() {
        deviceModel.openDevice();
    }

    public void close() {
        deviceModel.closeDevice();
    }

    public void sendData(byte[] data, IDeviceSendCallback callback, int waitTime, int repetition) {
        deviceModel.sendData(data, callback, waitTime, repetition);
    }

    public void sendProtocolData(byte[] data) {
        deviceModel.sendProtocolData(data);
    }

    public void sendProtocolData(byte[] data, int waitTime) {
        deviceModel.sendProtocolData(data, waitTime);
    }

    public void unlockReg() {
        sendProtocolData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x69, (byte) 0x88, (byte) 0xB5});
    }

    @Override
    public void saveReg() {
        sendProtocolData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00});
    }

    public void appliedCalibration() {
        sendProtocolData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00});
    }

    public void startFieldCalibration() {
        sendProtocolData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x07, (byte) 0x00});
    }

    public void endFieldCalibration() {
        sendProtocolData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x00, (byte) 0x00});
    }

    public void setReturnRate(byte rate) {
        sendProtocolData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x03, rate, (byte) 0x00});
    }

    public String getDeviceName() {
        return deviceModel.getDeviceName();
    }

    public String getMac() {
        return bluetoothBLE.getMac();
    }

    public String getDeviceData(String key) {
        return deviceModel.getDeviceData(key);
    }

    public void registerRecordObserver(IBwt901bleRecordObserver record) {
        deviceModel.registerListenKeyUpdateObserver(this);
        recordObservers.add(record);
    }

    public void removeRecordObserver(IBwt901bleRecordObserver record) {
        deviceModel.removeListenKeyUpdateObserver(this);
        recordObservers.remove(record);
    }

    @Override
    public void update(DeviceModel deviceModel) {
        deviceModel_OnListenKeyUpdate(deviceModel);
    }

    @Override
    public void deviceModel_OnListenKeyUpdate(DeviceModel deviceModel) {
        for (IBwt901bleRecordObserver recordObserver : recordObservers) {
            recordObserver.onRecord(this);
        }
    }
}


