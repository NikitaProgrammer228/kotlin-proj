package com.wit.witsdk.sensor.modular.connector.entity;

/**
 * 连接配置
 *
 * @author huangyajun
 * @date 2022/4/21 10:29
 */
public class ConnectConfig {


    private Ch340UsbOption ch340UsbOption = new Ch340UsbOption();

    private UdpOption udpOption = new UdpOption();

    private BluetoothBLEOption bluetoothBLEOption = new BluetoothBLEOption();

    private BluetoothSPPOption bluetoothSPPOption = new BluetoothSPPOption();


    public Ch340UsbOption getCh340UsbOption() {
        return ch340UsbOption;
    }

    public void setCh340UsbOption(Ch340UsbOption ch340UsbOption) {
        this.ch340UsbOption = ch340UsbOption;
    }

    public UdpOption getUdpOption() {
        return udpOption;
    }

    public void setUdpOption(UdpOption udpOption) {
        this.udpOption = udpOption;
    }

    public BluetoothBLEOption getBluetoothBLEOption() {
        return bluetoothBLEOption;
    }

    public void setBluetoothBLEOption(BluetoothBLEOption bluetoothBLEOption) {
        this.bluetoothBLEOption = bluetoothBLEOption;
    }

    public BluetoothSPPOption getBluetoothSPPOption() {
        return bluetoothSPPOption;
    }

    public void setBluetoothSPPOption(BluetoothSPPOption bluetoothSPPOption) {
        this.bluetoothSPPOption = bluetoothSPPOption;
    }
}
