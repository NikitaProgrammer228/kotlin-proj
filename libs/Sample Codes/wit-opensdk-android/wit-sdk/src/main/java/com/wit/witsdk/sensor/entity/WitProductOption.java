package com.wit.witsdk.sensor.entity;

import androidx.annotation.DrawableRes;

import com.wit.witsdk.sensor.modular.configurator.interfaces.IConfigurator;
import com.wit.witsdk.sensor.modular.dataview.entity.ContainerViewOption;
import com.wit.witsdk.sensor.modular.device.entity.DeviceOption;
import com.wit.witsdk.sensor.modular.recorder.entity.RecorderOption;

/**
 * 产品配置
 *
 * @author huangyajun
 * @date 2023/2/1 13:57
 */
public class WitProductOption {

    /**
     * 传感器型号,上位机上会显示
     */
    private String model;

    /**
     * 产品图片
     */
    @DrawableRes
    private int productImageId;

    /**
     * 记录文件配置
     */
    private RecorderOption recorderOption;

    /**
     * 设备配置
     */
    private DeviceOption deviceOption;

    /**
     * 数据视图配置
     */
    private ContainerViewOption containerViewOption;

    /**
     * 配置器类型
     */
    private Class<? extends IConfigurator> configuratorClass;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getProductImageId() {
        return productImageId;
    }

    public void setProductImageId(int productImageId) {
        this.productImageId = productImageId;
    }

    public RecorderOption getRecorderOption() {
        return recorderOption;
    }

    public void setRecorderOption(RecorderOption recorderOption) {
        this.recorderOption = recorderOption;
    }

    public ContainerViewOption getContainerViewOption() {
        return containerViewOption;
    }

    public void setContainerViewOption(ContainerViewOption containerViewOption) {
        this.containerViewOption = containerViewOption;
    }

    public DeviceOption getDeviceOption() {
        return deviceOption;
    }

    public void setDeviceOption(DeviceOption deviceOption) {
        this.deviceOption = deviceOption;
    }

    public Class<? extends IConfigurator> getConfiguratorClass() {
        return configuratorClass;
    }

    public void setConfiguratorClass(Class<? extends IConfigurator> configuratorClass) {
        this.configuratorClass = configuratorClass;
    }
}
