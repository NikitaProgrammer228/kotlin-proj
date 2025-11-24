package com.wit.witsdk.sensor.modular.searcher.roles;

import android.content.Context;
import android.util.Log;

import com.wit.witsdk.sensor.context.ProductModelManager;
import com.wit.witsdk.sensor.entity.WitProductOption;
import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.connector.enums.ConnectType;
import com.wit.witsdk.sensor.modular.connector.modular.ch340usb.Ch340USB;
import com.wit.witsdk.sensor.modular.connector.modular.ch340usb.exceptions.Ch340USBException;
import com.wit.witsdk.sensor.modular.connector.roles.WitCoreConnect;
import com.wit.witsdk.sensor.modular.device.entity.DeviceOption;
import com.wit.witsdk.sensor.modular.device.utils.DeviceModelFactory;
import com.wit.witsdk.sensor.modular.searcher.interfaces.AbsSearcher;

/**
 * @Author haungyajun
 * @Date 2022/5/7 14:39 （可以根据需要修改）
 */
public class UsbWitSearcher extends AbsSearcher {

    /**
     * 是否搜索中
     *
     * @author huangyajun
     * @date 2022/5/20 19:31
     */
    private boolean searching;

    public UsbWitSearcher(Context context) {
        super(context);
    }

    @Override
    protected void start() {
        searching = true;
        //
        Ch340USB ch340USB;
        try {
            ch340USB = Ch340USB.getInstance();
        } catch (Ch340USBException e) {
            Log.i("", e.getMessage());
            searching = false;
            return;
        }

        // 如果连接到了CH340
        if (ch340USB.isConnected()) {
            // 创建设备模型
            WitProductOption currentSensor = ProductModelManager.getCurrentProduct();
            DeviceOption deviceOption = currentSensor.getDeviceOption();
            DeviceModel deviceModel = DeviceModelFactory.createDevice("USB-DEVICE", deviceOption);

            // 创建连接器
            WitCoreConnect witCoreConnect = new WitCoreConnect();
            witCoreConnect.setConnectType(ConnectType.CH340USB);
            witCoreConnect.getConfig().getCh340UsbOption().setBaud(9600);
            deviceModel.setCoreConnect(witCoreConnect);
            addDevice(deviceModel);
        }
        searching = false;
    }

    @Override
    public void stop() {
        searching = false;
    }

    @Override
    public boolean isSearching() {
        return searching;
    }
}
