package com.wit.witsdk.sensor.modular.connector.modular.udp;

import android.util.Log;

import com.wit.witsdk.observer.interfaces.Observer;
import com.wit.witsdk.observer.interfaces.Observerable;
import com.wit.witsdk.observer.role.ObserverServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * udp服务
 *
 * @author huangyajun
 * @date 2022/4/25 20:07
 */
public class UdpServer implements Runnable, Observerable {

    // 端口
    private int serverPort = 1399;

    // 服务数据
    private DatagramSocket server = null;

    // 收到的数据包
    private DatagramPacket packet = null;

    // 客户端
    private ArrayList<InetAddress> clientIpList = new ArrayList<>();

    private HashSet<InetAddress> set = new HashSet<>();

    // 客户端收数据的端口
    private int clientPort = 9250;

    // 观察服务
    private ObserverServer observerServer = new ObserverServer();

    // 是不是打开的
    private boolean isOpen;

    public UdpServer(int serverPort, int clientPort) throws SocketException {
        super();
        this.serverPort = serverPort;
        this.clientPort = clientPort;

        open();
    }


    /**
     * udp是否已经关闭
     *
     * @author huangyajun
     * @date 2022/4/26 17:38
     */
    public boolean isOpen() {

        return isOpen;
    }

    /**
     * 打开udp
     *
     * @author huangyajun
     * @date 2022/4/26 17:39
     */
    public void open() throws SocketException {
        // 已经打开了就不用再打开
        if (isOpen()) {
            return;
        }

        server = new DatagramSocket(serverPort);
        isOpen = true;
        // 开始收数据
        new Thread(this).start();
    }

    /**
     * 关闭服务器
     *
     * @author huangyajun
     * @date 2022/4/25 20:03
     */
    public void close() {
        // 没有观察者了就彻底关闭
        if (observerServer.observerSize() == 0) {
            isOpen = false;
            server.close();
        }
    }

    /**
     * 发送数据
     *
     * @author huangyajun
     * @date 2022/4/25 20:04
     */
    public void send(byte[] data) {
        Log.e("--", "Send" + (new String(data)));

        clientIpList.clear();
        clientIpList.addAll(set);
        for (int i = 0; i < clientIpList.size(); i++) {
            try {
                InetAddress inetAddress = clientIpList.get(i);

                DatagramPacket packetSend = new DatagramPacket(data, data.length, inetAddress, clientPort);
                server.send(packetSend);
                // Log.e("core Connect", "Send:" + String.format("%2x %2x %2x %2x %2x %2x %2x %2x %2x",data[0],data[1],data[2],data[3],data[4],data[5],data[6],data[7],data[8]));
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("core Connect", " udp发送失败");
            }
        }
    }

    /**
     * 广播发送
     *
     * @author huangyajun
     * @date 2022/5/23 20:40
     */
    public void sendBroadcast(String address, byte[] msgSend) {
        InetAddress brhostAddress = null;
        try {
            brhostAddress = InetAddress.getByName(address);
        } catch (Exception e) {
            Log.e("--", "未找到服务器");
            e.printStackTrace();
        }

        DatagramPacket packetSend = new DatagramPacket(msgSend, msgSend.length, brhostAddress, clientPort);
        try {
            server.send(packetSend);
            String str = new String(msgSend, "UTF-8");
            //Log.e("--", "udp:" + brhostAddress + ":" + clientPort + "   :" + str);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("--", "发送失败");
        }
    }

    /**
     * 接收数据
     *
     * @author huangyajun
     * @date 2022/4/25 19:59
     */
    public void receiveData() {
        byte[] revBytes = new byte[1024];
        int len = 0;
        packet = new DatagramPacket(revBytes, revBytes.length);
        try {
            while (isOpen) {
                server.receive(packet);
                if ((len = packet.getLength()) > 0) {
                    String msg = new String(packet.getData(), 0, len);
                    InetAddress ip = packet.getAddress();
                    set.add(ip);
                    notifyObserver(Arrays.copyOf(packet.getData(), len));
                    Log.d("udpServer", "来自主机" + ip + "的消息:" + msg);
                }
            }
        } catch (IOException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
            // 连接已经关闭
            clientIpList.clear();
        }
    }

    /**
     * 线程开始的地方
     *
     * @author huangyajun
     * @date 2022/4/25 22:01
     */
    @Override
    public void run() {
        receiveData();
    }


    @Override
    public void registerObserver(Observer o) {
        observerServer.registerObserver(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observerServer.removeObserver(o);
    }

    @Override
    public void notifyObserver(byte[] data) {
        observerServer.notifyObserver(data);
    }

}