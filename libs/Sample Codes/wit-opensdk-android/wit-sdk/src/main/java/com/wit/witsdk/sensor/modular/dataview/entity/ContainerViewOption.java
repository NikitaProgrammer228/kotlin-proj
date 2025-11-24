package com.wit.witsdk.sensor.modular.dataview.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 视图容器选项
 *
 * @author huangyajun
 * @date 2022/5/13 14:29
 */
public class ContainerViewOption {

    /**
     * 视图配置
     */
    private List<DataViewOption> dataViewOptionList = new ArrayList<>();

    public List<DataViewOption> getDataViewOptionList() {
        return dataViewOptionList;
    }

    public void setDataViewOptionList(List<DataViewOption> dataViewOptionList) {
        this.dataViewOptionList = dataViewOptionList;
    }
}
