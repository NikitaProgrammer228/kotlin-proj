package com.wit.witsdk.sensor.modular.connector.modular.ch340usb;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.wit.sdk.R;
import com.wit.witsdk.ch34x.CH34xUARTDriver;
import com.wit.witsdk.ch34x.utils.UsbUtils;
import com.wit.witsdk.sensor.modular.connector.modular.ch340usb.exceptions.Ch340USBException;
import com.wit.witsdk.observer.interfaces.Observer;
import com.wit.witsdk.observer.interfaces.Observerable;
import com.wit.witsdk.observer.role.ObserverServer;

import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import cn.wch.ch34xuartdriver.CH34xUARTDriver;

/**
 * usb ch340 连接
 *
 * @author huangyajun
 * @date 2022/6/22 9:25
 */
public class Ch340USB implements Observerable {

    /**
     * usb权限
     */
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";

    private static final int ACCESS_PERMISSION = 1;

    /**
     * 单例实例
     */
    private static Ch340USB instance;

    // 现在的上下文
    public Context context;

    // 需要将CH34x的驱动类写在APP类下面，使得帮助类的生命周期与整个应用程序的生命周期是相同的
    public CH34xUARTDriver driver;

    // 观察服务
    private ObserverServer observerServer = new ObserverServer();

    // 读取数据线程
    private Thread readThread;

    // 当前波特率
    private int baud = 115200;

    private static Date lastResume = new Date();

    // 支持的波特率
    public static List<Integer> baudList = new ArrayList<Integer>();

    static {
        baudList.add(4800);
        baudList.add(9600);
        baudList.add(19200);
        baudList.add(38400);
        baudList.add(57600);
        baudList.add(115200);
        baudList.add(230400);
        baudList.add(460800);
        baudList.add(921600);
    }

    /**
     * 检查手机是否支持USB
     *
     * @author huangyajun
     * @date 2022/10/13 19:42
     */
    public static boolean usbFeatureSupported(Context context) {
        return UsbUtils.usbFeatureSupported(context);
    }

    /**
     * 初始化实例
     *
     * @author huangyajun
     * @date 2022/4/27 8:52
     */
    public static void initInstance(Context ctx) throws Exception {

        // 如果还没有申请权限就报错
        if (usbFeatureSupported(ctx) == false) {
            throw new Exception("手机不支持USB");
        }

        if (instance == null) {
            instance = new Ch340USB(ctx);
        }
    }

    /**
     * 获得实例
     *
     * @author huangyajun
     * @date 2022/4/27 8:52
     */
    public static Ch340USB getInstance() throws Ch340USBException {
        if (instance == null) {
            throw new Ch340USBException("无法获得实例，未初始化usb驱动");
        } else {
            return instance;
        }
    }

    /**
     * 构造
     *
     * @author huangyajun
     * @date 2022/4/27 18:42
     */
    private Ch340USB(Context ctx) {
        context = ctx;
        driver = new CH34xUARTDriver((UsbManager) context.getSystemService(Context.USB_SERVICE), context, ACTION_USB_PERMISSION);
    }

    /**
     * 获得实例
     *
     * @author huangyajun
     * @date 2022/4/27 8:52
     */
    public static void onResume() throws Ch340USBException {

        Date date = new Date();
        // 防止一直重新打开
        if (date.getTime() - lastResume.getTime() < 500) {
            return;
        }
        lastResume = date;

        if (instance != null) {
            instance.reOpen();
        } else {
            throw new Ch340USBException("未初始化实例");
        }
    }

    /**
     * 得到波特率对应的值
     *
     * @author huangyajun
     * @date 2022/4/27 8:52
     */
    public static int indexOfBaud(int baud) {
        return baudList.indexOf(baud);
    }

    /**
     * 打开设置波特率的提示框
     *
     * @author huangyajun
     * @date 2022/11/3 10:34
     */
    public void openSetBaudDialog() {
        String[] s = new String[baudList.size()];
        for (int i = 0; i < baudList.size(); i++) {
            Integer integer = baudList.get(i);
            s[i] = integer.intValue() + "";
        }

        final int[] iBaud = {5};
        ((Activity) context).runOnUiThread(() -> {
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.select_baud_rate))
                    .setSingleChoiceItems(s, 5, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            iBaud[0] = i;
                        }
                    })
                    .setPositiveButton(context.getString(R.string.Ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            setBaud(Integer.parseInt(s[iBaud[0]]));
                        }
                    })
                    .setNegativeButton(context.getString(R.string.Cancel), null)
                    .show();
        });
    }

    /**
     * 设置波特率
     *
     * @author huangyajun
     * @date 2022/4/27 8:52
     */
    public void setBaud(int baud) {
//        int iBaudRate = indexOfBaud(baud);
        // 设置串口配置
        driver.setConfig(baud, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
    }

    /**
     * 初始化串口
     *
     * @author huangyajun
     * @date 2022/4/27 8:53
     */
    public boolean serialPortInit() {

        // check the system whether support USB HOST or not
        int retval = driver.ResumeUsbList();
        switch (retval) {
            case -1:
                driver.closeDevice();
                break;
            case 0:
                //对串口设备进行初始化操作
                if (!driver.UartInit()) {
                    return false;
                }
                if (driver.isConnected()) setBaud(115200);
                // 开启读取数据线程
                startRead();
                Toast.makeText(context, context.getString(R.string.USB_has_been_detected), Toast.LENGTH_LONG).show();
                return true;
            default:
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("未授予USB连接权限");
                builder.setMessage("确认退出吗？");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                });
                builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                    }
                });
                builder.show();
                break;
        }
        return false;
    }

    /**
     * 写出数据
     *
     * @author huangyajun
     * @date 2022/4/27 11:25
     */
    public void write(byte[] byteSend) throws Ch340USBException {

        if (driver == null) {
            throw new Ch340USBException("无法发送数据，未初始化usb驱动");
        }

        if (driver.isConnected()) {
            try {
                driver.writeData(byteSend, byteSend.length);
            } catch (IOException e) {
                e.printStackTrace();
                throw new Ch340USBException("无法发送数据，发送失败");
            }
        } else {
            throw new Ch340USBException("无法发送数据，未连接USB");
        }
    }

    /**
     * 开始读取usb的内容
     *
     * @author huangyajun
     * @date 2022/4/27 11:13
     */
    private void startRead() {
        if (readThread != null) return;
        if (!driver.isConnected()) return;
        readThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (driver.isConnected()) {
                int length = driver.readData(buffer, 4096);
                if (length > 0) {
//                    String msg = new String(buffer, 0, length);
                    // Log.d("ch340USB", msg);
                    notifyObserver(Arrays.copyOf(buffer, length));
                }
                try {
                    Thread.sleep(20);
                } catch (Exception err) {
                }
            }
            Log.d("ch340USB", "USB已经断开");
            // 开始重新连接
            //startReOpenThread();

        });
        readThread.start();
    }

    /**
     * 重新连接
     *
     * @author huangyajun
     * @date 2022/4/27 18:15
     */
    public void reOpen() {

        Activity activity = (Activity) context;

        if (!driver.isConnected()) {

            Log.d("ch340USB", "正在尝试重新连接");

            activity.runOnUiThread(() -> {
                int retval = driver.ResumeUsbPermission();

                if (retval == 0) {
                    readThread = null;
                    serialPortInit();

                    Toast.makeText(activity, context.getString(R.string.USB_connection_successful),
                            Toast.LENGTH_SHORT).show();
                } else if (retval == -2) {
                    Toast.makeText(activity, "获取权限失败!",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 是否连接中
     *
     * @author huangyajun
     * @date 2022/5/20 19:27
     */
    public boolean isConnected() {
        return driver.isConnected();
    }

    /**
     * 开启重新连接线程
     *
     * @author huangyajun
     * @date 2022/4/27 17:56
     */
    private void startReOpenThread() {
        // 重新连接
        // disconnect();
        Thread reOpenTh = new Thread(() -> {

            Activity activity = (Activity) context;

            while (!driver.isConnected()) {
                reOpen();
            }
        });
        reOpenTh.start();

    }

    /**
     * 关闭连接
     *
     * @author huangyajun
     * @date 2022/4/27 11:14
     */
    public void disconnect() {
        // 没有观察者了就彻底关闭
        if (observerServer.observerSize() == 0) {
            driver.closeDevice();
            readThread = null;
        }
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
        observerServer.notifyObserver(data);
    }

}
