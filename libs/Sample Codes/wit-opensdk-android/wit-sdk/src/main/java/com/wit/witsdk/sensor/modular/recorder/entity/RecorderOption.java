package com.wit.witsdk.sensor.modular.recorder.entity;

import com.wit.witsdk.sensor.modular.device.entity.CalcOption;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author haungyajun
 * @Date 2022/5/20 9:50 （可以根据需要修改）
 */
public class RecorderOption {

    /// <summary>
    /// 记录的键
    /// </summary>
    private List<CalcOption> recordKeys = new ArrayList<>();

    public List<CalcOption> getRecordKeys() {
        return recordKeys;
    }

    public void setRecordKeys(List<CalcOption> recordKeys) {
        this.recordKeys = recordKeys;
    }
}
