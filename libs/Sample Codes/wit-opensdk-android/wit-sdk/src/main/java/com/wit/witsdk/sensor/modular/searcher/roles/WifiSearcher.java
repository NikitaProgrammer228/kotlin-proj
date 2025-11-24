package com.wit.witsdk.sensor.modular.searcher.roles;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.wit.witsdk.observer.interfaces.Observer;
import com.wit.witsdk.sensor.context.ProductModelManager;
import com.wit.witsdk.sensor.entity.WitProductOption;
import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.connector.enums.ConnectType;
import com.wit.witsdk.sensor.modular.connector.modular.udp.UdpServer;
import com.wit.witsdk.sensor.modular.connector.modular.udp.UdpServerPool;
import com.wit.witsdk.sensor.modular.connector.roles.WitCoreConnect;
import com.wit.witsdk.sensor.modular.device.entity.DeviceOption;
import com.wit.witsdk.sensor.modular.device.utils.DeviceModelFactory;
import com.wit.witsdk.sensor.modular.searcher.interfaces.AbsSearcher;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索WiFi传感器
 *
 * @author huangyajun
 * @date 2022/5/23 15:55
 */
public class WifiSearcher extends AbsSearcher implements Observer {

    /**
     * 是否搜索中
     *
     * @author huangyajun
     * @date 2022/5/20 19:31
     */
    private boolean searching;

    /**
     * 设备名称列表
     */
    private List<String> deviceNameList = new ArrayList<>();

    /**
     * udp服务器
     */
    private UdpServer udpServer = null;

    // 广播间隔
    int iBroadcastTime = 5;

    public WifiSearcher(Context context) {
        super(context);
    }

    /**
     * 开始搜索
     *
     * @author huangyajun
     * @date 2022/10/18 13:37
     */
    @Override
    protected void start() {
        searching = true;

        try {
            udpServer = UdpServerPool.createUdpServer(1399, 9250);
            deviceNameList.clear();
            // 监听udp的数据
            udpServer.removeObserver(this);
            udpServer.registerObserver(this);
            scanDevice();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // 只要没结束就一直等待
        while (searching) {
        }

        // 移除udp监听
        udpServer.removeObserver(this);

        searching = false;
    }

    /**
     * 发送扫描设备的命令
     *
     * @author huangyajun
     * @date 2022/5/23 20:41
     */
    public void scanDevice() {
        iBroadcastTime = 20;
        String s = "WIT" + getIp() + "\r\n";

        final byte[] ip = s.getBytes();
        Thread ipThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (iBroadcastTime-- > 0) {
                    try {

                        for (int i = 0; i < 255; i++) {
                            udpServer.sendBroadcast(getBroadcastIp(i), ip);
                            Thread.sleep(1);
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        ipThread.start();
    }

    /**
     * 获得局域网内广播ip
     *
     * @author huangyajun
     * @date 2022/10/17 17:22
     */
    private String getBroadcastIp(int ip) {
        WifiManager wm = (WifiManager) (getContext()).getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();
        //获取32位整型IP地址
        int ipAdd = wi.getIpAddress();
        //把整型地址转换成“*.*.*.*”地址
        return (ipAdd & 0xFF) + "." +
                ((ipAdd >> 8) & 0xFF) + "." +
                ((ipAdd >> 16) & 0xFF) + "." +
                (ip);
    }

    /**
     * 获得本机ip
     *
     * @author huangyajun
     * @date 2022/5/23 20:48
     */
    private String getIp() {
        WifiManager wm = (WifiManager) (getContext()).getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();
        //获取32位整型IP地址
        int ipAdd = wi.getIpAddress();
        //把整型地址转换成“*.*.*.*”地址
        String ip = intToIp(ipAdd);
        return ip;
    }

    /**
     * int值转ip地址
     *
     * @author huangyajun
     * @date 2022/10/17 17:21
     */
    private String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

    /**
     * 结束搜索
     *
     * @author huangyajun
     * @date 2022/10/18 13:38
     */
    @Override
    public void stop() {
        searching = false;
    }
    
    @Override
    public boolean isSearching() {
        return searching;
    }

    /**
     * 监听udp数据
     *
     * @author huangyajun
     * @date 2022/5/23 15:59
     */
    @Override
    public void update(byte[] data) {

        String dataString = new String(data);
        String[] packArray = dataString.split("\r\n");

        for (int i = 0; i < packArray.length; i++) {
            String line = packArray[i];

            // 如果长度不够肯定不是正确的设备
            if (line.length() < 15) {
                continue;
            }

            // 如果是已经找到过的设备就不用再添加一次了
            String lineAddr = line.substring(0, 12);
            if (deviceNameList.contains(lineAddr)) {
                continue;
            }

            deviceNameList.add(lineAddr);

            // 如果是WT开头就添加到设备列表
            if (lineAddr.charAt(0) == 'W' && lineAddr.charAt(1) == 'T') {
                // 创建设备模型
                WitProductOption currentSensor = ProductModelManager.getCurrentProduct();
                DeviceOption deviceOption = currentSensor.getDeviceOption();
                DeviceModel deviceModel = DeviceModelFactory.createDevice(lineAddr, deviceOption);

                // 创建连接器
                WitCoreConnect witCoreConnect = new WitCoreConnect();
                witCoreConnect.setConnectType(ConnectType.UDP);
                witCoreConnect.getConfig().getUdpOption().setRevPort(1399);
                witCoreConnect.getConfig().getUdpOption().setSendPort(9250);
                deviceModel.setDeviceData("ADDR", lineAddr);
                deviceModel.setCoreConnect(witCoreConnect);
                addDevice(deviceModel);
            }
        }
    }
}
