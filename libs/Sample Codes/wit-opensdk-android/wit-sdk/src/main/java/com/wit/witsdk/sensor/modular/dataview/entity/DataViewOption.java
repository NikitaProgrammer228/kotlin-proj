package com.wit.witsdk.sensor.modular.dataview.entity;

import com.wit.witsdk.sensor.modular.dataview.interfaces.IDataView;

import java.io.Serializable;

/**
 * 数据视图配置
 *
 * @author huangyajun
 * @date 2022/5/13 14:31
 */
public abstract class DataViewOption implements Serializable {

    // 在选项卡上的名称
    protected String tabName;

    // 视图控件类型
    protected Class<? extends IDataView> dataViewType;

    public Class<? extends IDataView> getDataViewType() {
        return dataViewType;
    }

    public void setDataViewType(Class<? extends IDataView> dataViewType) {
        this.dataViewType = dataViewType;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }
}
