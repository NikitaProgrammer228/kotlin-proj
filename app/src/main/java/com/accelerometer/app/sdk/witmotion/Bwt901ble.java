package com.accelerometer.app.sdk.witmotion;

import com.accelerometer.app.sdk.witmotion.components.Bwt901bleProcessor;
import com.accelerometer.app.sdk.witmotion.components.Bwt901bleResolver;
import com.accelerometer.app.sdk.witmotion.interfaces.IBwt901bleRecordObserver;
import com.wit.witsdk.api.interfaces.IAttitudeSensorApi;
import com.wit.witsdk.sensor.modular.connector.enums.ConnectType;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.sensor.modular.connector.roles.WitCoreConnect;
import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.device.exceptions.OpenDeviceException;
import com.wit.witsdk.sensor.modular.device.interfaces.IDeviceSendCallback;
import com.wit.witsdk.sensor.modular.device.interfaces.IListenKeyUpdateObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Обёртка для BLE-датчика WitMotion (адаптирована из примеров производителя).
 */
public class Bwt901ble implements IListenKeyUpdateObserver, IAttitudeSensorApi {

    private final DeviceModel deviceModel;
    private final BluetoothBLE bluetoothBLE;
    private final List<IBwt901bleRecordObserver> recordObservers = new ArrayList<>();

    public Bwt901ble(BluetoothBLE bluetoothBLE) {
        DeviceModel model = new DeviceModel(
                bluetoothBLE.getName() + "(" + bluetoothBLE.getMac() + ")",
                new Bwt901bleResolver(),
                new Bwt901bleProcessor(),
                "61_0"
        );
        WitCoreConnect witCoreConnect = new WitCoreConnect();
        witCoreConnect.setConnectType(ConnectType.BluetoothBLE);
        witCoreConnect.getConfig().getBluetoothBLEOption().setMac(bluetoothBLE.getMac());
        model.setCoreConnect(witCoreConnect);
        model.setDeviceData("Mac", bluetoothBLE.getMac());

        this.deviceModel = model;
        this.bluetoothBLE = bluetoothBLE;
    }

    public void open() throws OpenDeviceException {
        deviceModel.openDevice();
    }

    public void close() {
        deviceModel.closeDevice();
    }

    public boolean isOpen() {
        return deviceModel.isOpen();
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


