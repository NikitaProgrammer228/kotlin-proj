package com.wit.witsdk.sensor.modular.searcher.roles;

import android.content.Context;

import com.wit.witsdk.sensor.context.ProductModelManager;
import com.wit.witsdk.sensor.entity.WitProductOption;
import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.connector.enums.ConnectType;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothSPP;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver;
import com.wit.witsdk.sensor.modular.connector.roles.WitCoreConnect;
import com.wit.witsdk.sensor.modular.device.entity.DeviceOption;
import com.wit.witsdk.sensor.modular.device.utils.DeviceModelFactory;
import com.wit.witsdk.sensor.modular.searcher.interfaces.AbsSearcher;

import java.util.Arrays;

/**
 * 蓝牙感器搜索
 *
 * @author huangyajun
 * @date 2022/5/7 14:41
 */
public class BluetoothSearcher extends AbsSearcher implements IBluetoothFoundObserver {

    public BluetoothSearcher(Context context) {
        super(context);
    }

    /**
     * 开始搜索
     *
     * @author huangyajun
     * @date 2022/5/7 14:44
     */
    @Override
    protected void start() {
        try {
            WitBluetoothManager.DeviceNameFilter = Arrays.asList("HC", "WT", "Reb");
//            WitBluetoothManager.DeviceNameFilter = Arrays.asList("Reb");
            WitBluetoothManager bluetoothManager = WitBluetoothManager.getInstance();
            bluetoothManager.registerObserver(this);
            // 开始搜索
            bluetoothManager.startDiscovery();
            // 只要在搜索中就不退出
            while (bluetoothManager.isDiscovering()) {
                Thread.sleep(100);
            }

        } catch (BluetoothBLEException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止搜索
     *
     * @author huangyajun
     * @date 2022/5/7 14:44
     */
    @Override
    public void stop() {
        try {
            WitBluetoothManager bluetoothManager = WitBluetoothManager.getInstance();
            // 移除监听
            bluetoothManager.removeObserver(this);
            // 停止搜索
            bluetoothManager.stopDiscovery();
        } catch (BluetoothBLEException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是不是在搜索中
     *
     * @author huangyajun
     * @date 2022/5/7 14:44
     */
    @Override
    public boolean isSearching() {
        return false;
    }

    /**
     * 当找到ble蓝牙传感器时
     *
     * @author huangyajun
     * @date 2022/5/7 14:44
     */
    @Override
    public void onFoundBle(BluetoothBLE bluetoothBLE) {
        invokeSearchLog("找到蓝牙5.0传感器", "");
        
        // 创建设备模型
        WitProductOption currentSensor = ProductModelManager.getCurrentProduct();
        DeviceOption deviceOption = currentSensor.getDeviceOption();
        DeviceModel deviceModel = DeviceModelFactory.createDevice(bluetoothBLE.getName() + "(" + bluetoothBLE.getMac() + ")", deviceOption);

        // 创建连接器
        WitCoreConnect witCoreConnect = new WitCoreConnect();
        witCoreConnect.setConnectType(ConnectType.BluetoothBLE);
        witCoreConnect.getConfig().getBluetoothBLEOption().setMac(bluetoothBLE.getMac());
        deviceModel.setCoreConnect(witCoreConnect);
        deviceModel.setDeviceData("Mac", bluetoothBLE.getMac());
        addDevice(deviceModel);
    }

    /**
     * 当找到经典蓝牙传感器时
     *
     * @author huangyajun
     * @date 2022/5/7 14:44
     */
    @Override
    public void onFoundSPP(BluetoothSPP bluetoothSPP) {
        invokeSearchLog("找到经典蓝牙传感器", "");

        // 创建设备模型
        WitProductOption currentSensor = ProductModelManager.getCurrentProduct();
        DeviceOption deviceOption = currentSensor.getDeviceOption();
        DeviceModel deviceModel = DeviceModelFactory.createDevice(bluetoothSPP.getName() + "(" + bluetoothSPP.getMac() + ")", deviceOption);

        // 创建连接器
        WitCoreConnect witCoreConnect = new WitCoreConnect();
        witCoreConnect.setConnectType(ConnectType.BluetoothSPP);
        witCoreConnect.getConfig().getBluetoothSPPOption().setMac(bluetoothSPP.getMac());
        deviceModel.setCoreConnect(witCoreConnect);
        deviceModel.setDeviceData("Mac", bluetoothSPP.getMac());
        addDevice(deviceModel);
    }

    @Override
    public void onFoundDual(BluetoothBLE bluetoothBLE) {
        invokeSearchLog("找到双模传感器", "");

        // 创建设备模型
        WitProductOption currentSensor = ProductModelManager.getCurrentProduct();
        DeviceOption deviceOption = currentSensor.getDeviceOption();
        DeviceModel deviceModel = DeviceModelFactory.createDevice(bluetoothBLE.getName() + "(" + bluetoothBLE.getMac() + ")", deviceOption);

        // 创建连接器
        WitCoreConnect witCoreConnect = new WitCoreConnect();
        witCoreConnect.setConnectType(ConnectType.BluetoothBLE);
        witCoreConnect.getConfig().getBluetoothBLEOption().setMac(bluetoothBLE.getMac());
        deviceModel.setCoreConnect(witCoreConnect);
        deviceModel.setDeviceData("Mac", bluetoothBLE.getMac());
        addDevice(deviceModel);
    }
}
