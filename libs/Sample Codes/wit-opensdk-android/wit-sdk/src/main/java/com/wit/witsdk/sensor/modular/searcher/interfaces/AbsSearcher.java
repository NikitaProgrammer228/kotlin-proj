package com.wit.witsdk.sensor.modular.searcher.interfaces;

import android.content.Context;

import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.device.DeviceModelManager;
import com.wit.witsdk.sensor.modular.searcher.entity.SearcherOption;
import com.wit.witsdk.sensor.modular.searcher.interfaces.impl.SearchLogObserverServer;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索器抽象类
 *
 * @author huangyajun
 * @date 2022/5/10 14:06
 */
public abstract class AbsSearcher {

    /**
     * 搜索参数
     */
    public SearcherOption searcherOption;

    /**
     * 返回结果
     */
    public List<DeviceModel> resultList = new ArrayList<DeviceModel>();

    /**
     * 搜索日志被观察者
     */
    private SearchLogObserverServer searchLogObserverServer = new SearchLogObserverServer();

    /**
     * 上下文
     */
    private Context context;

    /**
     * 找到设备时的监听者接口
     */
    public interface FindDeviceListener {
        void onFindDevice(DeviceModel deviceModel);
    }

    /**
     * 找到设备时的监听者
     */
    private FindDeviceListener findDeviceListener;

    // 构造
    public AbsSearcher(Context context) {
        this.context = context;
    }

    /**
     * 添加找到设备时的监听者
     *
     * @author huangyajun
     * @date 2022/5/10 14:04
     */
    public void addOnFindDeviceListener(FindDeviceListener findDeviceListener) {
        this.findDeviceListener = findDeviceListener;
    }

    /**
     * 开始搜索
     *
     * @author huangyajun
     * @date 2022/4/28 14:17
     */
    public void beginStart(SearcherOption searcherOption) {
        this.searcherOption = searcherOption;
        resultList.clear();
        start();
    }

    /**
     * 子类需要继承这个方法开始搜索
     *
     * @author huangyajun
     * @date 2022/4/28 14:17
     */
    protected abstract void start();

    /**
     * 结束搜索
     *
     * @author huangyajun
     * @date 2022/4/28 14:17
     */
    public abstract void stop();

    /**
     * 是否搜索中
     *
     * @author huangyajun
     * @date 2022/4/28 14:17
     */
    public abstract boolean isSearching();

    /**
     * 获得搜索结果
     *
     * @author huangyajun
     * @date 2022/4/28 14:17
     */
    public List<DeviceModel> getResult() {
        return resultList;
    }

    /**
     * 注册日志输出事件
     *
     * @author huangyajun
     * @date 2022/4/28 14:45
     */
    public void registerSearchLogObserver(ISearchLogObserver o) {
        searchLogObserverServer.registerSearchLogObserver(o);
    }

    /**
     * 移除日志输出事件
     *
     * @author huangyajun
     * @date 2022/4/28 14:45
     */
    public void removeSearchLogObserver(ISearchLogObserver o) {
        searchLogObserverServer.removeSearchLogObserver(o);
    }

    /**
     * 输出日志
     *
     * @author huangyajun
     * @date 2022/4/28 14:41
     */
    protected void invokeSearchLog(String log, Object... args) {
        searchLogObserverServer.notifySearchLogObserver(log, args);
    }

    /**
     * 调用找到设备时的监听者
     *
     * @author huangyajun
     * @date 2022/5/10 14:04
     */
    private void invokeFindDeviceListener(DeviceModel deviceModel) {
        if (findDeviceListener != null) {
            findDeviceListener.onFindDevice(deviceModel);
        }
    }

    /**
     * 添加设备到结果集合
     *
     * @author huangyajun
     * @date 2022/5/10 14:04
     */
    public void addDevice(DeviceModel deviceModel) {
        invokeFindDeviceListener(deviceModel);
        DeviceModelManager.getInstance().putDeviceModel(deviceModel.getDeviceName(), deviceModel);
        resultList.add(deviceModel);
    }

    /**
     * 获得上下文
     * @return
     */
    public Context getContext(){
        return context;
    }
}
