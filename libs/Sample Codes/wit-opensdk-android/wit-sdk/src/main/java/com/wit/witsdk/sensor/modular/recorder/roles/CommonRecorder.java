package com.wit.witsdk.sensor.modular.recorder.roles;


import android.util.Log;

import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.device.entity.CalcOption;
import com.wit.witsdk.sensor.modular.device.interfaces.IListenKeyUpdateObserver;
import com.wit.witsdk.sensor.modular.recorder.entity.RecorderOption;
import com.wit.witsdk.sensor.modular.recorder.exceptions.RecorderException;
import com.wit.witsdk.sensor.modular.recorder.interfaces.AbsRecorder;
import com.wit.witsdk.sensor.modular.recorder.utils.WriteFileHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 常规记录器
 *
 * @author huangyajun
 * @date 2022/5/20 9:40
 */
public class CommonRecorder extends AbsRecorder implements IListenKeyUpdateObserver {

    /**
     * 日志tag
     */
    private static String TAG = "CommonRecorder";

    /**
     * 线程池，超过60秒未使用的线程会被回收
     *
     * @return
     */
    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0,
                Integer.MAX_VALUE,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
    }

    public CommonRecorder() {
    }

    /**
     * 写入文件帮助类
     */
    private WriteFileHelper writeFileHelper;

    /**
     * 记录文件名称
     */
    private String recordFilePath = "";

    /**
     * 写入数据缓存
     */
    private StringBuffer recordBuffer = new StringBuffer();

    /**
     * 缓存数据包数
     */
    private int recordBufferSize = 0;

    /**
     * 日期格式化
     */
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * 线程池
     */
    private ExecutorService executorService;

    /**
     * 开始记录
     *
     * @author huangyajun
     * @date 2022/5/20 9:55
     */
    @Override
    public synchronized boolean startRecord() throws RecorderException {

        /*
         * 创建记录文件
         */
        // 得到记录文件名称
        String recordDir = this.getRecordDir();
        String filePrefix = this.getFilePrefix();
        String ts = new Date().getTime() + "";
        recordFilePath = recordDir + "/" + filePrefix + "_" + ts + ".txt";
        try {
            writeFileHelper = new WriteFileHelper(recordFilePath);
            Log.i(TAG, "创建文件成功：" + recordFilePath);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "创建文件失败：" + recordFilePath);
            throw new RecorderException("无法创建记录文件,原因是：" + e.getMessage());
        }

        /*
         * 监听设备的数据
         */
        List<DeviceModel> deviceModelList = this.getDeviceModelList();
        for (int i = 0; i < deviceModelList.size(); i++) {
            DeviceModel deviceModel = deviceModelList.get(i);
            deviceModel.registerListenKeyUpdateObserver(this);
        }

        /*
         * 写入记录文件头部信息
         */
        RecorderOption recorderOption = getRecorderOption();
        List<CalcOption> recordKeys = recorderOption.getRecordKeys();
        StringBuilder builder = new StringBuilder();
        builder.append("Time\t");
        builder.append("DeviceName\t");
        for (int i = 0; recordKeys != null && i < recordKeys.size(); i++) {
            CalcOption calcOption = recordKeys.get(i);
            builder.append(calcOption.getName() + "(" + calcOption.getSuffix() + ")\t");
        }
        builder.append("\r\n");
        try {
            writeFileHelper.write(builder.toString());
        } catch (IOException e) {
            throw new RecorderException("无法写入记录文件,原因是：" + e.getMessage());
        }

        // 清空buffer
        recordBuffer.delete(0, recordBuffer.length());

        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        // 创建线程池
        executorService = newCachedThreadPool();

        // 标志为记录中
        setRecording(true);
        return false;
    }

    /**
     * 停止记录
     *
     * @author huangyajun
     * @date 2022/5/20 9:55
     */
    @Override
    public synchronized boolean stopRecord() {

        if (isRecording()) {

            setRecording(false);

            /*
             * 取消监听设备的数据
             */
            List<DeviceModel> deviceModelList = this.getDeviceModelList();
            for (int i = 0; i < deviceModelList.size(); i++) {
                DeviceModel deviceModel = deviceModelList.get(i);
                deviceModel.removeListenKeyUpdateObserver(this);
            }

            try {
                writeFileHelper.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "无法关闭文件,原因是：" + e.getMessage());
            }

            // 关闭线程池
            if (executorService != null) {
                executorService.shutdown();
                executorService = null;
            }
            return true;
        }

        return false;
    }

    /**
     * 设备数据更新时
     *
     * @author huangyajun
     * @date 2022/5/20 9:48
     */
    @Override
    public void update(DeviceModel deviceModel) {

        // 记录已经停止就不再记录
        if (!isRecording()) {
            return;
        }

        // 执行
        executorService.execute(() -> {
            writeRecord(deviceModel);
        });
    }

    /**
     * 写入记录文件
     *
     * @author huangyajun
     * @date 2022/5/20 10:34
     */
    private synchronized void writeRecord(DeviceModel deviceModel) {

        recordBufferSize++;

        // 得到记录文件信息
        RecorderOption recorderOption = getRecorderOption();
        List<CalcOption> recordKeys = recorderOption.getRecordKeys();
        // 写入时间和设备名称
        recordBuffer.append(sdf.format(new Date()) + "\t");
        recordBuffer.append(deviceModel.getDeviceName() + "\t");

        for (int i = 0; recordKeys != null && i < recordKeys.size(); i++) {
            CalcOption calcOption = recordKeys.get(i);
            String deviceData = deviceModel.getDeviceData(calcOption.getKey());
            recordBuffer.append(deviceData + "\t");
        }
        recordBuffer.append("\r\n");

        // 如果缓存现在很多了
        if (recordBufferSize > 10) {
            recordBufferSize = 0;
            String string = recordBuffer.toString();
            // 清空buffer
            recordBuffer.delete(0, recordBuffer.length());

            // 写入文件
            try {
                writeFileHelper.write(string);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "无法写入文件,原因是：" + e.getMessage());
            }

        }
    }
}
