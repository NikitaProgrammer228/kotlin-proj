package com.wit.witsdk.sensor.modular.searcher.interfaces;

/**
 * 观察对象
 *
 * @author huangyajun
 * @date 2022/4/26 11:28
 */
public interface ISearchLogObserverable {

    /**
     * 添加观察者
     *
     * @author huangyajun
     * @date 2022/4/26 13:42
     */
    void registerSearchLogObserver(ISearchLogObserver o);

    /**
     * 删除观察者
     *
     * @author huangyajun
     * @date 2022/4/26 13:42
     */
    void removeSearchLogObserver(ISearchLogObserver o);

    /**
     * 通知观察者
     *
     * @author huangyajun
     * @date 2022/4/26 13:42
     */
    public void notifySearchLogObserver(String log, Object... args);

}
