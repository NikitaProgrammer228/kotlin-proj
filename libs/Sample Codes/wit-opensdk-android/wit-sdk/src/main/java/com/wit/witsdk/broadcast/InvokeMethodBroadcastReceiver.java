package com.wit.witsdk.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 会自动调用方法的广播接收器
 *
 * @author huangyajun
 * @date 2022/5/10 10:02
 */
public class InvokeMethodBroadcastReceiver extends BroadcastReceiver {

    // 日志TAG
    public static final String TAG = "InvokeMethodBroadcast";

    // 处理广播请求的对象
    private final Object handleObj;

    // 接收的action
    private String filterAction = "";

    // 可以公开的方法前缀
    private String methodPrefix = "";


    public InvokeMethodBroadcastReceiver(Object handleObj, String filterAction, String methodPrefix) {
        this.handleObj = handleObj;
        this.filterAction = filterAction;
        this.methodPrefix = methodPrefix;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String method = intent.getStringExtra("method");
        Bundle extras = intent.getExtras();
        String action = intent.getAction();

        // 只接受对应的广播
        if (!action.equals(this.filterAction)) {
            return;
        }

        // 只能访问公开的方法
        if (method == null || !method.contains(methodPrefix)) {
            Log.e(TAG, "调用" + handleObj.getClass().getName() + "." + method + "方法被过滤,该方法不是指定的公开方法");
            System.exit(-1);
            return;
        } else {
            // Log.i(TAG, "开始调用" + handleObj.getClass().getName() + "." + method + "方法");
        }

        // 启动一个新线程去执行action方法
        Thread thread = new Thread(() -> {
            try {
                Class aClass = handleObj.getClass();
                Method method1 = aClass.getMethod(method, Bundle.class);
                method1.invoke(handleObj, extras);
                // Log.i(TAG, "调用" + handleObj.getClass().getName() + "." + method + "方法结束");
            } catch (NoSuchMethodException e) {
                // Log.e(TAG, "调用" + handleObj.getClass().getName() + "." + method + "方法出错,方法找不到");
                // e.printStackTrace();
            } catch (InvocationTargetException e) {
                Log.e(TAG, "调用" + handleObj.getClass().getName() + "." + method + "方法内部发生异常,请检查方法内部是否出错");
                e.printStackTrace();
                System.exit(-1);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "调用" + handleObj.getClass().getName() + "." + method + "方法出错,没有权限访问改方法,请检查是否为public");
                e.printStackTrace();
                System.exit(-1);
            }
        });
        thread.start();
    }
}
