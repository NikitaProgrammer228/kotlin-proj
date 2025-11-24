package com.wit.witsdk.sensor.modular.searcher.interfaces.impl;

import com.wit.witsdk.sensor.modular.searcher.interfaces.ISearchLogObserver;
import com.wit.witsdk.sensor.modular.searcher.interfaces.ISearchLogObserverable;

import java.util.ArrayList;
import java.util.List;

public class SearchLogObserverServer implements ISearchLogObserverable {

    private List<ISearchLogObserver> list; //面向接口编程

    public SearchLogObserverServer() {
        list = new ArrayList<>();
    }

    @Override
    public void registerSearchLogObserver(ISearchLogObserver observer) {
        list.add(observer);
    }

    @Override
    public void removeSearchLogObserver(ISearchLogObserver observer) {
        if (!list.isEmpty()) {
            list.remove(observer);
        }
    }

    @Override
    public void notifySearchLogObserver(String log, Object... args) {
        for (int i = 0; i < list.size(); i++) {
            ISearchLogObserver observer = list.get(i);
            observer.update(log, args);//通知Observer调用update方法
        }
    }
}
