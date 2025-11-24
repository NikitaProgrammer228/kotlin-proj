package com.wit.witsdk.sensor.interfaces;

import android.content.Context;

import com.wit.witsdk.sensor.entity.WitProductOption;

/**
 * 传感器配置类的超类
 *
 * @author huangyajun
 * @date 2023/2/1 14:26
 */
public abstract class BaseProduct implements IProduct {

    private Context context;

    /**
     * 获得传感器的配置
     *
     * @author huangyajun
     * @date 2023/2/1 14:27
     */
    @Override
    public abstract WitProductOption getSensorOption();

    /**
     * 获得上下文
     *
     * @author huangyajun
     * @date 2023/2/1 14:27
     */
    @Override
    public Context getContext() {
        return context;
    }

    /**
     * 设置上下文
     *
     * @author huangyajun
     * @date 2023/2/1 14:27
     */
    @Override
    public void setContext(Context context) {
        this.context = context;
    }
}
