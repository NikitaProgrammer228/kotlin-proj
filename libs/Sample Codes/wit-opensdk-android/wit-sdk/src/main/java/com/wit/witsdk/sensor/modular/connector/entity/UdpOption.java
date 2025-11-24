package com.wit.witsdk.sensor.modular.connector.entity;

import java.net.InetAddress;

/**
 * udp连接配置
 *
 * @author huangyajun
 * @date 2022/4/25 19:26
 */
public class UdpOption{

    private int revPort;

    private int sendPort;

//    private InetAddress ip;

//    public int getPort() {
//        return port;
//    }
//
//    public void setPort(int port) {
//        this.port = port;
//    }

//    public InetAddress getIp() {
//        return ip;
//    }
//
//    public void setIp(InetAddress ip) {
//        this.ip = ip;
//    }

    public int getRevPort() {
        return revPort;
    }

    public void setRevPort(int revPort) {
        this.revPort = revPort;
    }

    public int getSendPort() {
        return sendPort;
    }

    public void setSendPort(int sendPort) {
        this.sendPort = sendPort;
    }
}