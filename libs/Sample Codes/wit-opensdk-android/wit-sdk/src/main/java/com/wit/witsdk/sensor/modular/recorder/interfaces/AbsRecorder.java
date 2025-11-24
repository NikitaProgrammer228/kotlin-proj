package com.wit.witsdk.sensor.modular.recorder.interfaces;

import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.recorder.entity.RecorderOption;
import com.wit.witsdk.sensor.modular.recorder.exceptions.RecorderException;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录器超类
 *
 * @author huangyajun
 * @date 2022/5/20 9:41
 */
public abstract class AbsRecorder {

    /**
     * 是否正在记录文件
     */
    private boolean isRecording;

    /**
     * 记录保存文件夹
     */
    private String recordDir;

    /**
     * 记录配置
     */
    private RecorderOption recorderOption;

    /**
     * 所有设备
     */
    private List<DeviceModel> deviceModelList = new ArrayList<>();

    /**
     * 文件前缀名称
     */
    private String filePrefix;

    /**
     * 初始化
     *
     * @param recordDir       记录文件夹
     * @param filePrefix      记录文件前缀
     * @param deviceModelList 设备
     */
    public void init(String recordDir, String filePrefix, List<DeviceModel> deviceModelList) {
        this.recordDir = recordDir;
        this.filePrefix = filePrefix;
        this.deviceModelList = deviceModelList;
    }

    /**
     * 开始记录
     *
     * @return
     */
    public abstract boolean startRecord() throws RecorderException;

    /**
     * 结束记录
     *
     * @return
     */
    public abstract boolean stopRecord();


    public String getRecordDir() {
        return recordDir;
    }

    public void setRecordDir(String recordDir) {
        this.recordDir = recordDir;
    }

    public RecorderOption getRecorderOption() {
        return recorderOption;
    }

    public void setRecorderOption(RecorderOption recorderOption) {
        this.recorderOption = recorderOption;
    }

    public List<DeviceModel> getDeviceModelList() {
        return deviceModelList;
    }

    public void setDeviceModelList(List<DeviceModel> deviceModelList) {
        this.deviceModelList = deviceModelList;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    public boolean isRecording() {
        return isRecording;
    }

    protected void setRecording(boolean recording) {
        isRecording = recording;
    }
}
