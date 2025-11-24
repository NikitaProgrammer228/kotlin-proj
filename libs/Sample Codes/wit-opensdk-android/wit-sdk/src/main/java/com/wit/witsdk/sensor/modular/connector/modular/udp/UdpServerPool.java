package com.wit.witsdk.sensor.modular.connector.modular.udp;

import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author haungyajun
 * @Date 2022/4/26 9:16 （可以根据需要修改）
 */
public class UdpServerPool {

    /**
     * 现有的udp服务器
     *
     * @author huangyajun
     * @date 2022/4/26 9:20
     */
    private static Map<Integer, UdpServer> map = new HashMap<>();

    /**
     * 获得udp服务器
     *
     * @author huangyajun
     * @date 2022/4/26 9:16
     */
    public static synchronized UdpServer createUdpServer(int serverPort, int clientPort) throws SocketException {

        UdpServer udpServer;

        if (map.containsKey(serverPort)) {
            udpServer = map.get(serverPort);
            if (!udpServer.isOpen()) {
                udpServer.open();
            }
        } else {
            udpServer = new UdpServer(serverPort, clientPort);
            map.put(serverPort, udpServer);
        }

        return udpServer;
    }


}
