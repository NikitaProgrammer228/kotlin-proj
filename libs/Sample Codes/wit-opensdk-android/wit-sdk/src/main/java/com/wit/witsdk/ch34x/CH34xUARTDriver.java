package com.wit.witsdk.ch34x;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.wit.witsdk.R;
import com.wit.witsdk.ch34x.utils.UsbUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * ch340串口驱动
 *
 * @author huangyajun
 * @date 2022/6/22 19:57
 */
public class CH34xUARTDriver {

    // logtag
    private static final String TAG = CH34xUARTDriver.class.getSimpleName();

    /**
     * usb权限
     */
    private String ACTION_USB_PERMISSION;

    private UsbManager mUsbmanager;
    private PendingIntent mPendingIntent;
    private UsbDevice mUsbDevice;
    private UsbInterface mInterface;
    private UsbEndpoint mCtrlPoint;
    private UsbEndpoint mBulkInPoint;
    private UsbEndpoint mBulkOutPoint;
    private UsbDeviceConnection mDeviceConnection;
    private Context mContext;
    //private String mString;
    private Object h = new Object();
    private Object i = new Object();
    public boolean READ_ENABLE = false;
    public boolean k = false;
    public read_thread readThread;

    private byte[] readBuffer; /* circular buffer */
    private byte[] usbdata;
    private int writeIndex;
    private int readIndex;
    private int readcount;
    private int totalBytes;
    private ArrayList<String> DeviceNum = new ArrayList();
    protected final Object ReadQueueLock = new Object();
    protected final Object WriteQueueLock = new Object();
    // private int DeviceCount;
    private int mBulkPacketSize;
    final int maxnumbytes = 65536;

    public int WriteTimeOutMillis;
    public int ReadTimeOutMillis;
    private int DEFAULT_TIMEOUT = 500;

    /**
     * 接收系统的usb广播
     *
     * @author huangyajun
     * @date 2022/10/13 19:45
     */
    private final BroadcastReceiver a = new BroadcastReceiver() {

        public final void onReceive(Context var1, Intent intent) {
            String intentAction = intent.getAction();

            // Toast.makeText(mContext, "aciton =" + intentAction, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "aciton =" + intentAction);

            if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intentAction)) {
                Log.e("CH34xAndroidDriver", "Step1!\n");

            } else if (ACTION_USB_PERMISSION.equals(intentAction)) {
                Log.e("CH34xAndroidDriver", "Step2!\n");
                Class var7 = CH34xUARTDriver.class;
                synchronized (CH34xUARTDriver.class) {
                    UsbDevice var9 = (UsbDevice) intent.getParcelableExtra("device");
                    if (intent.getBooleanExtra("permission", false)) {
                        openDevice(var9);
                    } else {
                        Log.d(TAG, "Deny USB Permission");
                        Log.d("CH34xAndroidDriver", "permission denied");
                    }

                }
            } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(intentAction)) {
                Log.e("CH34xAndroidDriver", "Step3!\n");
                UsbDevice var6;
                String var3 = (var6 = (UsbDevice) intent.getParcelableExtra("device")).getDeviceName();
                Log.e("CH34xAndroidDriver", var3);


                for (int var8 = 0; var8 < DeviceNum.size(); ++var8) {
                    if (String.format("%04x:%04x", var6.getVendorId(), var6.getProductId()).equals(DeviceNum.get(var8))) {
                        Toast.makeText(mContext, mContext.getString(R.string.The_usb_disconnect), Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Device disconnected");
                        closeDevice();
                    }
                }

            } else {
                Log.e("CH34xAndroidDriver", "......");
            }
        }
    };

    public CH34xUARTDriver(UsbManager manager, Context context, String usbPermission
    ) {
        super();
        ACTION_USB_PERMISSION = usbPermission;
        readBuffer = new byte[maxnumbytes];
        usbdata = new byte[1024];
        writeIndex = 0;
        readIndex = 0;

        mUsbmanager = manager;
        mContext = context;
        WriteTimeOutMillis = 10000;
        ReadTimeOutMillis = 10000;

        ArrayAddDevice("1a86:7523");
        ArrayAddDevice("1a86:5523");
        ArrayAddDevice("1a86:5512");
        ArrayAddDevice("1a86:e010");
    }

    private void ArrayAddDevice(String str) {
        DeviceNum.add(str);
    }

    public boolean setTimeOut(int WriteTimeOut, int ReadTimeOut) {
        WriteTimeOutMillis = WriteTimeOut;
        ReadTimeOutMillis = ReadTimeOut;
        return true;
    }

    /**
     * 打开usb设备
     *
     * @param mDevice
     */
    public synchronized void openUsbDevice(UsbDevice mDevice) {
        Object localObject;
        UsbInterface intf;
        if (mDevice == null) {
            return;
        }
        intf = getUsbInterface(mDevice);
        if ((mDevice != null) && (intf != null)) {
            localObject = this.mUsbmanager.openDevice(mDevice);
            if (localObject != null) {
                if (((UsbDeviceConnection) localObject).claimInterface(intf,
                        true)) {
                    this.mUsbDevice = mDevice;
                    this.mDeviceConnection = ((UsbDeviceConnection) localObject);
                    this.mInterface = intf;
                    if (!enumerateEndPoint(intf)) {
                        return;
                    }
                    Log.e(TAG, "Device Has Attached to Android");
                    if (!this.k) {
                        this.k = true;
                        readThread = new read_thread(mBulkInPoint,
                                mDeviceConnection);
                        readThread.start();
                    }
                    return;
                }
            }
        }

    }

    /**
     * 打开设备
     *
     * @author huangyajun
     * @date 2022/10/13 19:59
     */
    public synchronized void openDevice(UsbDevice mDevice) {
        if (mUsbmanager.hasPermission(mDevice)) {
            openUsbDevice(mDevice);
        } else {
            Log.e(TAG, "hasPermission is not");
        }
    }

    /**
     * 关闭设备
     *
     * @author huangyajun
     * @date 2022/10/13 19:59
     */
    public synchronized void closeDevice() {
        if (this.k) {
            this.k = false;
        }

        if (this.mDeviceConnection != null) {
            if (this.mInterface != null) {
                this.mDeviceConnection.releaseInterface(this.mInterface);
                this.mInterface = null;
            }

            this.mDeviceConnection.close();
        }

        if (this.mUsbDevice != null) {
            this.mUsbDevice = null;
        }

        if (this.mUsbmanager != null) {
            this.mUsbmanager = null;
        }

        if (READ_ENABLE == true) {
            this.mContext.unregisterReceiver(this.a);
            READ_ENABLE = false;
        }

    }


    /**
     * 遍历所有设备，并直接打开第一个设备
     *
     * @return
     */
    public int ResumeUsbList() {

        // 申请权限
        mUsbmanager = (UsbManager) mContext
                .getSystemService(Context.USB_SERVICE);

        mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(this.ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        HashMap<String, UsbDevice> deviceList = mUsbmanager.getDeviceList();
        Log.i("usbsize", "size=" + deviceList.size());
        if (deviceList.isEmpty()) {
            Log.e(TAG, "No Device Or Device Not Match");
            return -1;
        } else {
            Iterator<UsbDevice> localIterator = deviceList.values().iterator();
            while (localIterator.hasNext()) {
                UsbDevice localUsbDevice = localIterator.next();
                for (int i = 0; i < DeviceNum.size(); ++i) {
                    // Log.d(TAG, "DeviceCount is " + DeviceCount);
                    IntentFilter intentFilter;
                    (intentFilter = new IntentFilter(this.ACTION_USB_PERMISSION)).addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
                    this.mContext.registerReceiver(this.a, intentFilter);
                    READ_ENABLE = true;

                    // Log.d(TAG, "DeviceCount is " + DeviceCount);
                    if (String.format("%04x:%04x",
                            Integer.valueOf(localUsbDevice.getVendorId()),
                            Integer.valueOf(localUsbDevice.getProductId())).equals(DeviceNum.get(i))) {
                        if (mUsbmanager.hasPermission(localUsbDevice)) {
                            openUsbDevice(localUsbDevice);
                        } else {
                            Log.e(TAG, "hasPermission is not");
                            synchronized (this.a) {
                                this.mUsbmanager.requestPermission(localUsbDevice, this.mPendingIntent);
                            }
                        }
                        return 0;
                    }
                }
            }
            return -1;
        }
    }

    /**
     * 获取所有设备，并返回第一个找到的usbdevice
     *
     * @return
     */
    public UsbDevice EnumerateDevice() {
        mUsbmanager = (UsbManager) mContext
                .getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = mUsbmanager.getDeviceList();
        if (deviceList.isEmpty()) {
            Log.e(TAG, "No Device Or Device Not Match");
            return null;
        }

        Iterator<UsbDevice> localIterator = deviceList.values().iterator();
        while (localIterator.hasNext()) {
            UsbDevice localUsbDevice = localIterator.next();
            for (int i = 0; i < DeviceNum.size(); ++i) {
                if (String.format("%04x:%04x", new Object[]{
                        Integer.valueOf(localUsbDevice
                                .getVendorId()),
                        Integer.valueOf(localUsbDevice
                                .getProductId())}).equals(
                        DeviceNum.get(i))) {
                    return localUsbDevice;

                } else {
                    Log.d(TAG, "String.format not match");
                }
            }

        }

        return null;
    }

    /**
     * 设备是否连接
     *
     * @return
     */
    public boolean isConnected() {
        return (this.mUsbDevice != null) && (this.mInterface != null)
                && (this.mDeviceConnection != null);
    }

    /**
     * 获取usb设备
     *
     * @return
     */
    protected UsbDevice getUsbDevice() {
        return this.mUsbDevice;
    }

    /**
     * Performs a control transaction on endpoint zero for this device. The
     * direction of the transfer is determined by the request type. If
     * requestType & {@link UsbConstants#USB_ENDPOINT_DIR_MASK} is
     * {@link UsbConstants#USB_DIR_OUT}, then the transfer is a write, and if it
     * is {@link UsbConstants#USB_DIR_IN}, then the transfer is a read.
     *
     * @param request request ID for this transaction
     * @param value   value field for this transaction
     * @param index   index field for this transaction
     * @return length of data transferred (or zero) for success, or negative
     * value for failure
     * <p>
     * public int controlTransfer(int requestType, int request, int
     * value, int index, byte[] buffer, int length, int timeout)
     */

    public int Uart_Control_Out(int request, int value, int index) {
        int retval = 0;
        retval = mDeviceConnection.controlTransfer(UsbType.USB_TYPE_VENDOR
                        | UsbType.USB_RECIP_DEVICE | UsbType.USB_DIR_OUT, request,
                value, index, null, 0, DEFAULT_TIMEOUT);

        return retval;
    }

    public int Uart_Control_In(int request, int value, int index,
                               byte[] buffer, int length) {
        int retval = 0;
        retval = mDeviceConnection.controlTransfer(UsbType.USB_TYPE_VENDOR
                        | UsbType.USB_RECIP_DEVICE | UsbType.USB_DIR_IN, request,
                value, index, buffer, length, DEFAULT_TIMEOUT);
        return retval;
    }

    private int Uart_Set_Handshake(int control) {
        return Uart_Control_Out(UartCmd.VENDOR_MODEM_OUT, ~control, 0);
    }

    public int Uart_Tiocmset(int set, int clear) {
        int control = 0;
        if ((set & UartModem.TIOCM_RTS) == UartModem.TIOCM_RTS) {
            control |= UartIoBits.UART_BIT_RTS;
        }
        if ((set & UartModem.TIOCM_DTR) == UartModem.TIOCM_DTR) {
            control |= UartIoBits.UART_BIT_DTR;
        }
        if ((clear & UartModem.TIOCM_RTS) == UartModem.TIOCM_RTS) {
            control &= ~UartIoBits.UART_BIT_RTS;
        }
        if ((clear & UartModem.TIOCM_DTR) == UartModem.TIOCM_DTR) {
            control &= ~UartIoBits.UART_BIT_DTR;
        }

        return Uart_Set_Handshake(control);
    }

    /**
     * 初始化串口
     *
     * @author huangyajun
     * @date 2022/10/13 20:09
     */
    public boolean UartInit() {
        int ret;
        int size = 8;
        byte[] buffer = new byte[size];
        Uart_Control_Out(UartCmd.VENDOR_SERIAL_INIT, 0x0000, 0x0000);
        ret = Uart_Control_In(UartCmd.VENDOR_VERSION, 0x0000, 0x0000, buffer, 2);
        if (ret < 0) {
            return false;
        }
        Uart_Control_Out(UartCmd.VENDOR_WRITE, 0x1312, 0xD982);
        Uart_Control_Out(UartCmd.VENDOR_WRITE, 0x0f2c, 0x0004);
        ret = Uart_Control_In(UartCmd.VENDOR_READ, 0x2518, 0x0000, buffer, 2);
        if (ret < 0) {
            return false;
        }
        Uart_Control_Out(UartCmd.VENDOR_WRITE, 0x2727, 0x0000);
        Uart_Control_Out(UartCmd.VENDOR_MODEM_OUT, 0x00ff, 0x0000);
        return true;
    }

    /**
     * 设置配置
     *
     * @author huangyajun
     * @date 2022/10/13 20:09
     */
    public boolean setConfig(int baudRate, byte dataBit, byte stopBit,
                             byte parity, byte flowControl) {
        int value = 0;
        int index = 0;
        char valueHigh = 0, valueLow = 0, indexHigh = 0, indexLow = 0;
        switch (parity) {
            case 0:
                /* NONE */
                valueHigh = 0x00;
                break;
            case 1:
                /* ODD */
                valueHigh |= 0x08;
                break;
            case 2:
                /* Even */
                valueHigh |= 0x18;
                break;
            case 3:
                /* Mark */
                valueHigh |= 0x28;
                break;
            case 4:
                /* Space */
                valueHigh |= 0x38;
                break;
            default:
                /* None */
                valueHigh = 0x00;
                break;
        }

        if (stopBit == 2) {
            valueHigh |= 0x04;
        }

        switch (dataBit) {
            case 5:
                valueHigh |= 0x00;
                break;
            case 6:
                valueHigh |= 0x01;
                break;
            case 7:
                valueHigh |= 0x02;
                break;
            case 8:
                valueHigh |= 0x03;
                break;
            default:
                valueHigh |= 0x03;
                break;
        }

        valueHigh |= 0xc0;
        valueLow = 0x9c;

        value |= valueLow;
        value |= (int) (valueHigh << 8);

        switch (baudRate) {
            case 50:
                indexLow = 0;
                indexHigh = 0x16;
                break;
            case 75:
                indexLow = 0;
                indexHigh = 0x64;
                break;
            case 110:
                indexLow = 0;
                indexHigh = 0x96;
                break;
            case 135:
                indexLow = 0;
                indexHigh = 0xa9;
                break;
            case 150:
                indexLow = 0;
                indexHigh = 0xb2;
                break;
            case 300:
                indexLow = 0;
                indexHigh = 0xd9;
                break;
            case 600:
                indexLow = 1;
                indexHigh = 0x64;
                break;
            case 1200:
                indexLow = 1;
                indexHigh = 0xb2;
                break;
            case 1800:
                indexLow = 1;
                indexHigh = 0xcc;
                break;
            case 2400:
                indexLow = 1;
                indexHigh = 0xd9;
                break;
            case 4800:
                indexLow = 2;
                indexHigh = 0x64;
                break;
            case 9600:
                indexLow = 2;
                indexHigh = 0xb2;
                break;
            case 19200:
                indexLow = 2;
                indexHigh = 0xd9;
                break;
            case 38400:
                indexLow = 3;
                indexHigh = 0x64;
                break;
            case 57600:
                indexLow = 3;
                indexHigh = 0x98;
                break;
            case 115200:
                indexLow = 3;
                indexHigh = 0xcc;
                break;
            case 230400:
                indexLow = 3;
                indexHigh = 0xe6;
                break;
            case 460800:
                indexLow = 3;
                indexHigh = 0xf3;
                break;
            case 500000:
                indexLow = 3;
                indexHigh = 0xf4;
                break;
            case 921600:
                indexLow = 7;
                indexHigh = 0xf3;
                break;
            case 1000000:
                indexLow = 3;
                indexHigh = 0xfa;
                break;
            case 2000000:
                indexLow = 3;
                indexHigh = 0xfd;
                break;
            case 3000000:
                indexLow = 3;
                indexHigh = 0xfe;
                break;
            default: // default baudRate "9600"
                indexLow = 2;
                indexHigh = 0xb2;
                break;
        }

        index |= 0x88 | indexLow;
        index |= (int) (indexHigh << 8);

        Uart_Control_Out(UartCmd.VENDOR_SERIAL_INIT, value, index);
        if (flowControl == 1) {
            Uart_Tiocmset(UartModem.TIOCM_DTR | UartModem.TIOCM_RTS, 0x00);
        }
        return true;
    }

    /**
     * 读取数据
     *
     * @author huangyajun
     * @date 2022/10/13 20:08
     */
    public int readData(byte[] data, int length) {
        synchronized (h) {
            int mLen;
            /* should be at least one byte to read */
            if ((length < 1) || (totalBytes == 0)) {
                mLen = 0;
                return mLen;
            }

            /* check for max limit */
            if (length > totalBytes) {
                length = totalBytes;
            }

            /* update the number of bytes available */
            totalBytes -= length;

            mLen = length;

            /* copy to the user buffer */
            for (int count = 0; count < length; count++) {
                data[count] = readBuffer[readIndex];
                readIndex++;
                /*
                 * shouldnt read more than what is there in the buffer, so no need
                 * to check the overflow
                 */
                readIndex %= maxnumbytes;
            }
            return mLen;
        }
    }

    /**
     * 读取数据
     *
     * @author huangyajun
     * @date 2022/10/13 20:08
     */
    public int readData(char[] data, int length) {
        int mLen;

        /* should be at least one byte to read */
        if ((length < 1) || (totalBytes == 0)) {
            mLen = 0;
            return mLen;
        }

        /* check for max limit */
        if (length > totalBytes) {
            length = totalBytes;
        }

        /* update the number of bytes available */
        totalBytes -= length;

        mLen = length;

        /* copy to the user buffer */
        for (int count = 0; count < length; count++) {
            data[count] = (char) readBuffer[readIndex];
            readIndex++;
            /*
             * shouldnt read more than what is there in the buffer, so no need
             * to check the overflow
             */
            readIndex %= maxnumbytes;
        }
        return mLen;
    }

    /**
     * 写出数据
     *
     * @author huangyajun
     * @date 2022/10/13 20:07
     */
    public int writeData(byte[] buf, int length) throws IOException {
        int mLen = 0;
        mLen = writeData(buf, length, this.WriteTimeOutMillis);
        if (mLen < 0) {
            throw new IOException("Expected Write Actual Bytes");
        }
        return mLen;
    }

    /**
     * 写出数据
     *
     * @author huangyajun
     * @date 2022/10/13 20:07
     */
    public int writeData(byte[] buf, int length, int timeoutMillis) {
        int offset = 0;
        int HasWritten = 0;
        int odd_len = length;
        if (this.mBulkOutPoint == null) {
            return -1;
        }
        while (offset < length) {
            synchronized (this.WriteQueueLock) {
                int mLen = Math.min(odd_len, this.mBulkPacketSize);
                byte[] arrayOfByte = new byte[mLen];
                if (offset == 0) {
                    System.arraycopy(buf, 0, arrayOfByte, 0, mLen);
                } else {
                    System.arraycopy(buf, offset, arrayOfByte, 0, mLen);
                }
                HasWritten = this.mDeviceConnection.bulkTransfer(
                        this.mBulkOutPoint, arrayOfByte, mLen, timeoutMillis);
                if (HasWritten < 0) {
                    return -2;
                } else {
                    offset += HasWritten;
                    odd_len -= HasWritten;
                    // Log.d(TAG, "offset " + offset + " odd_len " + odd_len);
                }
            }
        }
        return offset;
    }

    private boolean enumerateEndPoint(UsbInterface sInterface) {
        if (sInterface == null) {
            return false;
        }
        for (int i = 0; i < sInterface.getEndpointCount(); ++i) {
            UsbEndpoint endPoint = sInterface.getEndpoint(i);
            if (endPoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && endPoint.getMaxPacketSize() == 0x20) {
                if (endPoint.getDirection() == UsbConstants.USB_DIR_IN) {
                    mBulkInPoint = endPoint;
                } else {
                    mBulkOutPoint = endPoint;
                }
                this.mBulkPacketSize = endPoint.getMaxPacketSize();
            } else if (endPoint.getType() == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
                mCtrlPoint = endPoint;
            }
        }
        return true;
    }

    /**
     * 获得USB接口
     *
     * @author huangyajun
     * @date 2022/10/13 20:04
     */
    private UsbInterface getUsbInterface(UsbDevice paramUsbDevice) {
        if (this.mDeviceConnection != null) {
            if (this.mInterface != null) {
                this.mDeviceConnection.releaseInterface(this.mInterface);
                this.mInterface = null;
            }
            this.mDeviceConnection.close();
            this.mUsbDevice = null;
            this.mInterface = null;
        }
        if (paramUsbDevice == null) {
            return null;
        }

        for (int i = 0; i < paramUsbDevice.getInterfaceCount(); i++) {
            UsbInterface intf = paramUsbDevice.getInterface(i);
            if (intf.getInterfaceClass() == 0xff
                    && intf.getInterfaceSubclass() == 0x01
                    && intf.getInterfaceProtocol() == 0x02) {
                return intf;
            }
        }
        return null;
    }

    /**
     * 获得Usb权限
     *
     * @author huangyajun
     * @date 2022/10/13 20:03
     */
    public int ResumeUsbPermission() {
        return UsbUtils.ResumeUsbPermission(mContext, ACTION_USB_PERMISSION, DeviceNum);
    }

    /**
     * 读取数据线程
     *
     * @author huangyajun
     * @date 2022/10/13 20:05
     */
    /* usb input data handler */
    private class read_thread extends Thread {
        UsbEndpoint endpoint;
        UsbDeviceConnection mConn;

        read_thread(UsbEndpoint point, UsbDeviceConnection con) {
            endpoint = point;
            mConn = con;
            this.setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            while (READ_ENABLE == true) {
                while (totalBytes > (maxnumbytes - 63)) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                synchronized (ReadQueueLock) {
                    if (endpoint != null) {
                        readcount = mConn.bulkTransfer(endpoint, usbdata, 64,
                                ReadTimeOutMillis);
                        if (readcount > 0) {
                            for (int count = 0; count < readcount; count++) {
                                readBuffer[writeIndex] = usbdata[count];
                                writeIndex++;
                                writeIndex %= maxnumbytes;
                            }

                            if (writeIndex >= readIndex) {
                                totalBytes = writeIndex - readIndex;
                            } else {
                                totalBytes = (maxnumbytes - readIndex)
                                        + writeIndex;
                            }

                        }
                    }
                }
            }
        }
    }

    public final class UartModem {
        public static final int TIOCM_LE = 0x001;
        public static final int TIOCM_DTR = 0x002;
        public static final int TIOCM_RTS = 0x004;
        public static final int TIOCM_ST = 0x008;
        public static final int TIOCM_SR = 0x010;
        public static final int TIOCM_CTS = 0x020;
        public static final int TIOCM_CAR = 0x040;
        public static final int TIOCM_RNG = 0x080;
        public static final int TIOCM_DSR = 0x100;
        public static final int TIOCM_CD = TIOCM_CAR;
        public static final int TIOCM_RI = TIOCM_RNG;
        public static final int TIOCM_OUT1 = 0x2000;
        public static final int TIOCM_OUT2 = 0x4000;
        public static final int TIOCM_LOOP = 0x8000;
    }

    public final class UsbType {
        public static final int USB_TYPE_VENDOR = (0x02 << 5);
        public static final int USB_RECIP_DEVICE = 0x00;
        public static final int USB_DIR_OUT = 0x00; /* to device */
        public static final int USB_DIR_IN = 0x80; /* to host */
    }

    public final class UartCmd {
        public static final int VENDOR_WRITE_TYPE = 0x40;
        public static final int VENDOR_READ_TYPE = 0xC0;
        public static final int VENDOR_READ = 0x95;
        public static final int VENDOR_WRITE = 0x9A;
        public static final int VENDOR_SERIAL_INIT = 0xA1;
        public static final int VENDOR_MODEM_OUT = 0xA4;
        public static final int VENDOR_VERSION = 0x5F;
    }

    public final class UartState {
        public static final int UART_STATE = 0x00;
        public static final int UART_OVERRUN_ERROR = 0x01;
        public static final int UART_PARITY_ERROR = 0x02;
        public static final int UART_FRAME_ERROR = 0x06;
        public static final int UART_RECV_ERROR = 0x02;
        public static final int UART_STATE_TRANSIENT_MASK = 0x07;
    }

    public final class UartIoBits {
        public static final int UART_BIT_RTS = (1 << 6);
        public static final int UART_BIT_DTR = (1 << 5);
    }


}

