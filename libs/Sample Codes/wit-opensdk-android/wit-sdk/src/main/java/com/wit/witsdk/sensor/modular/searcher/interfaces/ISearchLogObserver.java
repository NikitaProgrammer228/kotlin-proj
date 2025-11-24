package com.wit.witsdk.sensor.modular.searcher.interfaces;

/**
 * 搜索日志观察者
 *
 * @author huangyajun
 * @date 2022/4/26 11:27
 */
public interface ISearchLogObserver {

    /**
     * 接收通知
     *
     * @author huangyajun
     * @date 2022/4/26 13:42
     */
    void update(String log, Object... args);
}
