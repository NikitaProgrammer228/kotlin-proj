package com.wit.witsdk.sensor.modular.dataview.interfaces;

import com.wit.witsdk.sensor.modular.device.DeviceModel;

import java.util.List;

/**
 * 数据视图
 *
 * @Author haungyajun
 * @Date 2022/5/11 18:46 （可以根据需要修改）
 */
public interface IDataView {

    /**
     * 初始化视图
     *
     * @author huangyajun
     * @date 2022/5/13 16:17
     */
    void loadData(List<DeviceModel> deviceModelList);

    /**
     * 自动更新视图回调事件
     *
     * @author huangyajun
     * @date 2022/6/17 21:19
     */
    void onAutoUpdate();

    /**
     *
     *
     * @author huangyajun
     * @date 2022/6/18 13:51
     */
    void onRealUpdate(DeviceModel deviceModel);
    
    /**
     * 销毁数据视图
     *
     * @author huangyajun
     * @date 2022/5/16 15:26
     */
    void onDispose();
}
