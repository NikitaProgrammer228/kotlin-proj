package com.wit.witsdk.sensor.modular.connector.roles;


import android.util.Log;

import com.wit.witsdk.sensor.modular.connector.entity.BluetoothSPPOption;
import com.wit.witsdk.sensor.modular.connector.enums.ConnectStatus;
import com.wit.witsdk.sensor.modular.connector.enums.ConnectType;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException;
import com.wit.witsdk.sensor.modular.connector.modular.ch340usb.Ch340USB;
import com.wit.witsdk.sensor.modular.connector.modular.ch340usb.exceptions.Ch340USBException;
import com.wit.witsdk.observer.interfaces.Observer;
import com.wit.witsdk.observer.interfaces.Observerable;
import com.wit.witsdk.observer.role.ObserverServer;
import com.wit.witsdk.sensor.modular.connector.entity.BluetoothBLEOption;
import com.wit.witsdk.sensor.modular.connector.entity.Ch340UsbOption;
import com.wit.witsdk.sensor.modular.connector.entity.ConnectConfig;
import com.wit.witsdk.sensor.modular.connector.entity.UdpOption;
import com.wit.witsdk.sensor.modular.connector.exceptions.ConnectConfigException;
import com.wit.witsdk.sensor.modular.connector.exceptions.ConnectOpenException;
import com.wit.witsdk.sensor.modular.connector.interfaces.IWitCoreConnect;
import com.wit.witsdk.sensor.modular.connector.enums.*;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothSPP;
import com.wit.witsdk.sensor.modular.connector.modular.udp.UdpServer;
import com.wit.witsdk.sensor.modular.connector.modular.udp.UdpServerPool;
import com.wit.witsdk.utils.StringUtils;

import java.net.SocketException;

/**
 * 核心连接器
 *
 * @author huangyajun
 * @date 2022/4/21 10:20
 */
public class WitCoreConnect implements IWitCoreConnect, Observerable, Observer {

    // ch340Usb
    private Ch340USB ch340Usb;

    // spp蓝牙
    private BluetoothSPP bluetoothSPP;

    // ble蓝牙
    private BluetoothBLE bluetoothBLE;

    // udp连接
    private UdpServer udpServer;

    // 连接类型
    private ConnectType connectType = ConnectType.UDP;

    // 连接状态
    private ConnectStatus connectStatus = ConnectStatus.Closed;

    // 连接配置
    private ConnectConfig config = new ConnectConfig();

    // 观察服务
    private ObserverServer observerServer = new ObserverServer();


    /**
     * 打开设备
     *
     * @author huangyajun
     * @date 2022/4/25 15:48
     */
    @Override
    public void open() throws ConnectConfigException, ConnectOpenException, SocketException, Ch340USBException {
        if (config == null) {
            throw new ConnectConfigException("连接参数不能为空");
        }

        // 检查配置
        checkConfig();

        // BLE蓝牙连接时
        if (connectType.getCode() == ConnectType.BluetoothBLE.getCode()) {
            bluetoothBLEOpen();
        } else if (connectType.getCode() == ConnectType.CH340USB.getCode()) {
            ch340UsbOpen();
        } else if (connectType.getCode() == ConnectType.UDP.getCode()) {
            udpOpen();
        } else if (connectType.getCode() == ConnectType.BluetoothSPP.getCode()) {
            bluetoothSPPOpen();
        }else {
            throw new ConnectConfigException("没有连接类型");
        }

        // 设置为已经连接状态
        connectStatus = ConnectStatus.Opened;
    }

    /**
     * 打开ble蓝牙
     *
     * @author huangyajun
     * @date 2022/4/28 16:21
     */
    private void bluetoothBLEOpen() throws ConnectOpenException {

        BluetoothBLEOption bluetoothBLEOption = config.getBluetoothBLEOption();

        WitBluetoothManager bluetoothManager = null;
        try {
            bluetoothManager = WitBluetoothManager.getInstance();
        } catch (BluetoothBLEException e) {
            e.printStackTrace();
        }

        bluetoothBLE = bluetoothManager.getBluetoothBLE(bluetoothBLEOption.getMac());

        if (bluetoothBLE != null) {
            bluetoothBLE.registerObserver(this);
            bluetoothBLE.connect(bluetoothBLEOption.getMac());
        } else {
            throw new ConnectOpenException("无法打开此蓝牙设备");
        }

    }

    /**
     * 打开经典蓝牙
     *
     * @author huangyajun
     * @date 2022/6/17 9:27
     */
    private void bluetoothSPPOpen() throws ConnectOpenException {
        BluetoothSPPOption bluetoothSPPOption = config.getBluetoothSPPOption();
        WitBluetoothManager bluetoothManager = null;
        try {
            bluetoothManager = WitBluetoothManager.getInstance();
        } catch (BluetoothBLEException e) {
            e.printStackTrace();
        }

        bluetoothSPP = bluetoothManager.getBluetoothSPP(bluetoothSPPOption.getMac());

        if (bluetoothSPP != null) {
            bluetoothSPP.registerObserver(this);
            bluetoothSPP.connect(bluetoothSPPOption.getMac());
        } else {
            throw new ConnectOpenException("无法打开此蓝牙设备");
        }
    }

    /**
     * 打开ch340 usb设备
     *
     * @author huangyajun
     * @date 2022/4/26 20:02
     */
    private void ch340UsbOpen() throws Ch340USBException {
        //
        Ch340UsbOption ch340UsbOption = config.getCh340UsbOption();

        if (ch340Usb == null) {
            ch340Usb = Ch340USB.getInstance();
        }

        //ch340Usb.setBaud(ch340UsbOption.getBaud());
        ch340Usb.openSetBaudDialog();
        ch340Usb.reOpen();
        ch340Usb.removeObserver(this);
        ch340Usb.registerObserver(this);
    }

    /**
     * 打开udp连接
     *
     * @author huangyajun
     * @date 2022/4/25 19:48
     */
    private void udpOpen() throws SocketException {
        UdpOption udpOption = config.getUdpOption();
        int revPort = udpOption.getRevPort();
        int sendPort = udpOption.getSendPort();

        if (udpServer != null) {
            udpServer.close();
            udpServer = null;
        }

        // InetAddress ip = udpOption.getIp();
        udpServer = UdpServerPool.createUdpServer(revPort, sendPort);
        // 监听udp的数据
        udpServer.removeObserver(this);
        udpServer.registerObserver(this);
    }

    /**
     * 检查连接配置
     *
     * @author huangyajun
     * @date 2022/4/25 15:55
     */
    private void checkConfig() throws ConnectConfigException {

        if (connectType.getCode() == ConnectType.BluetoothBLE.getCode()) {
            // BLE蓝牙连接时
            BluetoothBLEOption bluetoothBLEOption = config.getBluetoothBLEOption();
            if (StringUtils.isBlank(bluetoothBLEOption.getMac())) {
                throw new ConnectConfigException("bluetoothBLEOption 缺少mac地址");
            }
        } else if (connectType.getCode() == ConnectType.CH340USB.getCode()) {
            //  USB-CH340
            Ch340UsbOption ch340UsbOption = config.getCh340UsbOption();

            if (ch340UsbOption.getBaud() < 0 || ch340UsbOption.getBaud() > 921600) {
                throw new ConnectConfigException("ch340UsbOption 波特率不在范围内");
            }
        } else if (connectType.getCode() == ConnectType.UDP.getCode()) {
            // UDP连接时
            UdpOption udpOption = config.getUdpOption();

            if (udpOption == null) {
                throw new ConnectConfigException("UDP连接配置不能为空");
            }

            if (udpOption.getRevPort() < 0 || udpOption.getRevPort() > 65535) {
                throw new ConnectConfigException("UDP 接收端口不能为空");
            }

            if (udpOption.getSendPort() < 0 || udpOption.getSendPort() > 65535) {
                throw new ConnectConfigException("UDP 发送端口不能为空");
            }

        } else if (connectType.getCode() == ConnectType.BluetoothSPP.getCode()) {
            // 经典蓝牙连接时
            BluetoothSPPOption bluetoothSPPOption = config.getBluetoothSPPOption();
            if (StringUtils.isBlank(bluetoothSPPOption.getMac())) {
                throw new ConnectConfigException("bluetoothSPPOption 缺少mac地址");
            }
        } else {
            throw new ConnectConfigException("没有连接类型");
        }
    }


    /**
     * 是否打开的
     *
     * @author huangyajun
     * @date 2022/4/25 15:57
     */
    @Override
    public boolean isOpen() {
        return connectStatus.getCode() == ConnectStatus.Opened.getCode();
    }

    /**
     * 关闭设备
     *
     * @author huangyajun
     * @date 2022/4/25 19:15
     */
    @Override
    public void close() {

        if (udpServer != null) {
            udpServer.removeObserver(this);
            udpServer.close();
        }

        if (ch340Usb != null) {
            ch340Usb.removeObserver(this);
            ch340Usb.disconnect();
        }

        if (bluetoothBLE != null) {
            bluetoothBLE.removeObserver(this);
            bluetoothBLE.disconnect();
        }

        if (bluetoothSPP != null) {
            bluetoothSPP.removeObserver(this);
            bluetoothSPP.stop();
        }

        // 设置为已经连接状态
        connectStatus = ConnectStatus.Closed;
    }

    /**
     * 发送数据
     *
     * @author huangyajun
     * @date 2022/4/25 19:15
     */
    @Override
    public void sendData(byte[] data) throws Ch340USBException {
        // 蓝牙连接时
        if (connectType.getCode() == ConnectType.BluetoothBLE.getCode()) {
            bluetoothBLE.write(data);
        } else if (connectType.getCode() == ConnectType.BluetoothSPP.getCode()) {
            bluetoothSPP.write(data);
        } else if (connectType.getCode() == ConnectType.CH340USB.getCode()) {
            ch340Usb.write(data);
        } else if (connectType.getCode() == ConnectType.UDP.getCode()) {
            udpSend(data);
        }
    }

    /**
     * 使用udp发送数据
     *
     * @author huangyajun
     * @date 2022/4/25 19:39
     */
    private void udpSend(byte[] data) {
        Log.e("--", "Send" + (new String(data)));

        // UdpOption udpOption = config.getUdpOption();

        try {
            // DatagramPacket packetSend = new DatagramPacket(data, data.length, udpOption.getIp(), udpOption.getPort());
            udpServer.send(data);
            // Log.e("core Connect", "Send:" + String.format("%2x %2x %2x %2x %2x %2x %2x %2x %2x",data[0],data[1],data[2],data[3],data[4],data[5],data[6],data[7],data[8]));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("core Connect", " udp发送失败");
        }
    }

    @Override
    public boolean setConnectType(ConnectType connectType) {
        this.connectType = connectType;
        return false;
    }

    @Override
    public ConnectType getConnectType() {
        return connectType;
    }

    @Override
    public ConnectStatus getConnectStatus() {
        return null;
    }

    public ConnectConfig getConfig() {
        return config;
    }

    @Override
    public void update(byte[] data) {
        notifyObserver(data);
    }

    @Override
    public void registerObserver(Observer o) {
        observerServer.registerObserver(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observerServer.removeObserver(o);
    }

    @Override
    public void notifyObserver(byte[] data) {

        Thread thread = new Thread(() -> {
            observerServer.notifyObserver(data);
        });
        thread.start();
    }

    public Ch340USB getCh340Usb() {
        return ch340Usb;
    }

    public BluetoothSPP getBluetoothSPP() {
        return bluetoothSPP;
    }

    public BluetoothBLE getBluetoothBLE() {
        return bluetoothBLE;
    }

    public UdpServer getUdpServer() {
        return udpServer;
    }
}
