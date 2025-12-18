package com.wit.witsdk.sensor.modular.device.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.wit.witsdk.R;
import com.wit.witsdk.broadcast.InvokeMethodBroadcastReceiver;
import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.device.DeviceModelManager;
import com.wit.witsdk.sensor.modular.device.constant.DeviceChangeType;
import com.wit.witsdk.sensor.modular.device.exceptions.OpenDeviceException;
import com.wit.witsdk.sensor.modular.searcher.entity.SearcherOption;
import com.wit.witsdk.sensor.modular.searcher.interfaces.AbsSearcher;
import com.wit.witsdk.sensor.modular.searcher.interfaces.ISearchLogObserver;
import com.wit.witsdk.sensor.modular.searcher.roles.BluetoothSearcher;
import com.wit.witsdk.sensor.modular.searcher.roles.UsbWitSearcher;
import com.wit.witsdk.sensor.modular.searcher.roles.WifiSearcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 设备服务
 *
 * @author huangyajun
 * @date 2022/5/7 15:05
 */
public class DeviceService extends Service implements ISearchLogObserver, AbsSearcher.FindDeviceListener {

    // 广播接收
    public static final String BROADCAST_SERVER = "DeviceService.BROADCAST_SERVER";

    public static final String BROADCAST_CLIENT = "DeviceService.BROADCAST_CLIENT";

    public static final String TAG = "DeviceService";

    // 蓝牙传感器搜索器
    private BluetoothSearcher bluetoothSearcher = new BluetoothSearcher(this);

    // usb设备搜索器
    private UsbWitSearcher usbWitSearcher = new UsbWitSearcher(this);

    // wifi传感器搜索器
    private WifiSearcher wifiSearcher = new WifiSearcher(this);

    // 设备模型管理器
    private DeviceModelManager deviceModelManager = DeviceModelManager.getInstance();

    // 是否搜索中
    private boolean searching = false;

    // 广播接收器
    private BroadcastReceiver deviceServiceBroadcastReceiver = new InvokeMethodBroadcastReceiver(this, BROADCAST_SERVER, "action");

    /**
     * 启动时
     *
     * @author huangyajun
     * @date 2022/5/9 20:38
     */
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_SERVER);
        registerReceiver(deviceServiceBroadcastReceiver, intentFilter);
        Log.d(TAG, "设备服务启动了");

        bluetoothSearcher.registerSearchLogObserver(this);
        bluetoothSearcher.addOnFindDeviceListener(this);
        usbWitSearcher.registerSearchLogObserver(this);
        usbWitSearcher.addOnFindDeviceListener(this);
        wifiSearcher.registerSearchLogObserver(this);
        wifiSearcher.addOnFindDeviceListener(this);
    }

    /**
     * 销毁时
     *
     * @author huangyajun
     * @date 2022/5/9 20:49
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(deviceServiceBroadcastReceiver);
        bluetoothSearcher.removeSearchLogObserver(this);
        usbWitSearcher.removeSearchLogObserver(this);
        wifiSearcher.removeSearchLogObserver(this);
        deviceModelManager.clearAllDeviceModel();
    }

    /**
     * 再次启动时
     *
     * @author huangyajun
     * @date 2022/5/9 20:38
     */
    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 避免service被注销
        flags = START_STICKY;
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 绑定时
     *
     * @author huangyajun
     * @date 2022/5/9 20:39
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 开始扫描
     *
     * @author huangyajun
     * @date 2022/5/9 20:40
     */
    public synchronized void actionStartScan(Bundle extras) {
        Log.d(TAG, "开始扫描");
        // 清除所有设备
        deviceModelManager.clearAllDeviceModel();
        // 如果正在搜索当中就不再次开启搜索
        if (searching) {
            return;
        }

        // 开始搜索usb传感器
        Thread thread1 = new Thread(() -> {
            usbWitSearcher.beginStart(new SearcherOption());
        });
        thread1.start();

        // 开始搜索蓝牙传感器
        Thread thread2 = new Thread(() -> {
            bluetoothSearcher.beginStart(new SearcherOption());
        });
        thread2.start();

        // 开始搜索WiFi传感器
        Thread thread3 = new Thread(() -> {
            wifiSearcher.beginStart(new SearcherOption());
        });
        thread3.start();

        searching = true;
        // 通知已经开始搜索了
        actionWhetherSearching(new Bundle());
    }

    /**
     * 设备列表信息改变时
     *
     * @author huangyajun
     * @date 2022/5/9 20:58
     */
    public void callActionDeviceChange(String deviceName, DeviceChangeType type, String message) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_CLIENT);
        intent.putExtra("method", "actionDeviceChange");
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("changeType", type);
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    /**
     * 停止扫描
     *
     * @author huangyajun
     * @date 2022/5/9 20:41
     */
    public void actionStopScan(Bundle extras) {
        Log.d(TAG, "结束扫描");
        // 停止搜索器
        bluetoothSearcher.stop();
        usbWitSearcher.stop();
        wifiSearcher.stop();
        searching = false;
        // 通知结束搜索了
        actionWhetherSearching(new Bundle());
    }

    /**
     * 是否正在搜索中
     *
     * @author huangyajun
     * @date 2022/5/9 20:58
     */
    public void actionWhetherSearching(Bundle extras) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_CLIENT);
        intent.putExtra("method", "actionWhetherSearching");
        intent.putExtra("searching", searching);
        sendBroadcast(intent);
    }

    /**
     * 开启设备
     *
     * @author huangyajun
     * @date 2022/5/9 20:41
     */
    public void actionOpenDevice(Bundle extras) {
        String deviceName = extras.getString("deviceName");
        // 加入连接列表
//        openDeviceList.add(deviceName);
        // 设备id
        openDevice(deviceName);
    }

    /**
     * 打开设备
     *
     * @author huangyajun
     * @date 2023/3/9 18:09
     */
    private void openDevice(String deviceName) {
        DeviceModel deviceModel = deviceModelManager.getDeviceModel(deviceName);
        if (deviceModel != null && deviceModel.isOpen() == false) {
            try {
                callActionDeviceChange(deviceName, DeviceChangeType.CONNECTING, getString(R.string.connecting));
                deviceModel.openDevice();
                callActionDeviceChange(deviceName, DeviceChangeType.CONNECTED, getString(R.string.connected));
                // 连接成功
            } catch (OpenDeviceException e) {
                e.printStackTrace();
                // 连接失败
                callActionDeviceChange(deviceName, DeviceChangeType.CONNECTION_FAIL, getString(R.string.connection_fail));
            }
        }
    }

    /**
     * 关闭设备
     *
     * @author huangyajun
     * @date 2022/5/9 20:42
     */
    public void actionCloseDevice(Bundle extras) {
        String deviceName = extras.getString("deviceName");
        closeDevice(deviceName);
    }

    /**
     * 关闭设备
     *
     * @author huangyajun
     * @date 2023/3/9 18:10
     */
    private void closeDevice(String deviceName) {

        DeviceModel deviceModel = deviceModelManager.getDeviceModel(deviceName);
        if (deviceModel != null) {
            try {
                deviceModel.closeDevice();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 通知设备发生了改变
        callActionDeviceChange(deviceName, DeviceChangeType.DISCONNECTED, getString(R.string.disconnected));
    }

    /**
     * 当有搜索日志时
     *
     * @author huangyajun
     * @date 2022/5/10 13:57
     */
    @Override
    public void update(String log, Object... args) {

    }

    /**
     * 当找到设备时
     *
     * @author huangyajun
     * @date 2022/5/10 14:16
     */
    @Override
    public void onFindDevice(DeviceModel deviceModel) {
        // 通知设备发生了改变
        callActionDeviceChange(deviceModel.getDeviceName(), DeviceChangeType.FOUND_DEVICE, getString(R.string.found_device));
    }
}