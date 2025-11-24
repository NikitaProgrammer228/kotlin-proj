package com.wit.witsdk.sensor.interfaces;

import android.content.Context;

import com.wit.witsdk.sensor.entity.WitProductOption;

/**
 * 传感器配置接口
 *
 * @author huangyajun
 * @date 2023/2/1 13:56
 */
public interface IProduct {

    /**
     * 获得传感器配置
     *
     * @author huangyajun
     * @date 2023/2/1 14:23
     */
    WitProductOption getSensorOption();
    
    /**
     * 获得上下文
     *
     * @author huangyajun
     * @date 2023/2/1 14:24
     */
    Context getContext();

    /**
     * 设置上下文
     *
     * @author huangyajun
     * @date 2023/2/1 14:24
     */
    void setContext(Context context);
}
