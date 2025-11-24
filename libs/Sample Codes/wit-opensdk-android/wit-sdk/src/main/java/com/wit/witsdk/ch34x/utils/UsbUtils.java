package com.wit.witsdk.ch34x.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * usb工具类
 *
 * @author huangyajun
 * @date 2022/10/13 19:52
 */
public class UsbUtils {

    private static Object lockObj = new Object();

    private static String TAG = "usb";

    /**
     * 检查手机是否支持USB
     *
     * @author huangyajun
     * @date 2022/10/13 19:42
     */
    public static boolean usbFeatureSupported(Context mContext) {
        boolean bool = mContext.getPackageManager().hasSystemFeature(
                "android.hardware.usb.host");
        return bool;
    }

    /**
     * 获得USB权限
     *
     * @author huangyajun
     * @date 2022/6/22 21:38
     */
    public static int ResumeUsbPermission(Context mContext, String ACTION_USB_PERMISSION, ArrayList<String> DeviceNum) {

        // 申请权限
        UsbManager mUsbmanager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        PendingIntent mPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        HashMap<String, UsbDevice> deviceList = mUsbmanager.getDeviceList();
        Log.i("usbsize", "size=" + deviceList.size());
        if (deviceList.isEmpty()) {
            Log.e(TAG, "没有USB设备");
            return -1;
        } else {
            Iterator<UsbDevice> localIterator = deviceList.values().iterator();
            while (localIterator.hasNext()) {
                UsbDevice localUsbDevice = localIterator.next();
                for (int i = 0; i < DeviceNum.size(); ++i) {
                    if (String.format("%04x:%04x",
                            Integer.valueOf(localUsbDevice.getVendorId()),
                            Integer.valueOf(localUsbDevice.getProductId())).equals(DeviceNum.get(i))) {
                        if (!mUsbmanager.hasPermission(localUsbDevice)) {
                            Log.e(TAG, "hasPermission is not");
                            synchronized (lockObj) {
                                mUsbmanager.requestPermission(localUsbDevice, mPendingIntent);
                                return -2;
                            }
                        }
                        return 0;
                    }
                }
            }
            return -1;
        }
    }


}
