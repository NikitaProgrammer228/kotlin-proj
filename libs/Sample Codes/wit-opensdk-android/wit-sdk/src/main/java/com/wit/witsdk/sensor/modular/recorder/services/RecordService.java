package com.wit.witsdk.sensor.modular.recorder.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.wit.witsdk.R;
import com.wit.witsdk.broadcast.InvokeMethodBroadcastReceiver;
import com.wit.witsdk.sensor.context.ProductModelManager;
import com.wit.witsdk.sensor.entity.WitProductOption;
import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.device.DeviceModelManager;
import com.wit.witsdk.sensor.modular.recorder.entity.RecorderOption;
import com.wit.witsdk.sensor.modular.recorder.exceptions.RecorderException;
import com.wit.witsdk.sensor.modular.recorder.interfaces.AbsRecorder;
import com.wit.witsdk.sensor.modular.recorder.roles.CommonRecorder;
import com.wit.witsdk.utils.UiThreadHelper;

import java.io.File;
import java.util.List;

/**
 * 记录文件服务
 *
 * @author huangyajun
 * @date 2022/6/20 14:24
 */
public class RecordService extends Service {

    // 广播接收
    public static final String BROADCAST_SERVER = "RecordService.BROADCAST_SERVER";

    public static final String BROADCAST_CLIENT = "RecordService.BROADCAST_CLIENT";

    // 广播接收器
    private BroadcastReceiver deviceServiceBroadcastReceiver = new InvokeMethodBroadcastReceiver(this, BROADCAST_SERVER, "action");

    // 文件记录器
    private AbsRecorder recorder = new CommonRecorder();

    // 日志tag
    private String TAG = "RecordService";

    // 设备模型管理器
    private DeviceModelManager deviceModelManager = DeviceModelManager.getInstance();

    // 记录文件目录
    private String recordDirPath = Environment.getExternalStorageDirectory() + "/Download/WitRecords";

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

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    /**
     * 开始记录
     *
     * @author huangyajun
     * @date 2022/5/20 14:22
     */
    public void actionStartRecord(Bundle extras) {
        Log.d(TAG, "开始记录文件");

        WitProductOption currentProduct = ProductModelManager.getCurrentProduct();
        RecorderOption recorderOption = currentProduct.getRecorderOption();
        recorder.setRecorderOption(recorderOption);
        List<DeviceModel> deviceModelList = deviceModelManager.getAllByList();
        recorder.init(recordDirPath, "data", deviceModelList);
        try {
            recorder.startRecord();

            UiThreadHelper.runUi(() -> {
                Toast.makeText(this, getString(R.string.start_record), Toast.LENGTH_SHORT).show();
            });

        } catch (RecorderException e) {
            e.printStackTrace();
        }
    }

    /**
     * 结束记录
     *
     * @author huangyajun
     * @date 2022/5/20 14:22
     */
    public void actionStopRecord(Bundle extras) {
        Log.d(TAG, "结束记录文件");
        recorder.stopRecord();

        UiThreadHelper.runUi(() -> {
            Toast.makeText(this, getString(R.string.stop_record), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 是否记录文件中
     *
     * @author huangyajun
     * @date 2022/5/20 14:22
     */
    public void actionOpenRecordDir(Bundle extras) {
        UiThreadHelper.runUi(() -> {

            File recordDir = new File(recordDirPath);
            // 创建记录文件目录
            if (recordDir.exists() == false) {
                recordDir.mkdirs();
            }

            // 打开记录文件目录
            Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Download%2fWitRecords");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("*/*");
            intent.setData(uri);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    /**
     * 是否记录文件中
     *
     * @author huangyajun
     * @date 2022/5/20 14:22
     */
    public void actionIsRecording(Bundle extras) {
        Log.d(TAG, "是否记录文件中");
        recorder.isRecording();

        // Toast.makeText(this, "Stop recorder", Toast.LENGTH_SHORT).show();
    }

}