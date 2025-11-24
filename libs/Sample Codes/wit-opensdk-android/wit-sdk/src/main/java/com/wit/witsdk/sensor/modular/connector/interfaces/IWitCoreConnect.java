package com.wit.witsdk.sensor.modular.connector.interfaces;

import com.wit.witsdk.sensor.modular.connector.enums.ConnectStatus;
import com.wit.witsdk.sensor.modular.connector.enums.ConnectType;
import com.wit.witsdk.sensor.modular.connector.modular.ch340usb.exceptions.Ch340USBException;
import com.wit.witsdk.sensor.modular.connector.enums.*;
import com.wit.witsdk.sensor.modular.connector.exceptions.ConnectConfigException;
import com.wit.witsdk.sensor.modular.connector.exceptions.ConnectOpenException;

import java.net.SocketException;

/**
 * 核心连接器接口
 *
 * @Author haungyajun
 * @Date 2022/4/21 10:15 （可以根据需要修改）
 */
public interface IWitCoreConnect  {

    /**
     * 打开连接
     *
     * @author huangyajun
     * @date 2022/4/21 10:32
     */
    void open() throws ConnectConfigException, ConnectOpenException, SocketException, Ch340USBException;

    /**
     * 是否已经打开连接
     *
     * @author huangyajun
     * @date 2022/4/21 10:32
     */
    boolean isOpen();

    /**
     * 关闭连接
     *
     * @author huangyajun
     * @date 2022/4/21 10:33
     */
    void close();

    /**
     * 发送数据
     *
     * @author huangyajun
     * @date 2022/4/21 10:33
     */
    void sendData(byte[] data) throws Ch340USBException;

    /**
     * 设置连接类型
     *
     * @author huangyajun
     * @date 2022/4/21 10:33
     */
    boolean setConnectType(ConnectType connectType);

    /**
     * 获取连接类型
     *
     * @author huangyajun
     * @date 2022/4/21 10:33
     */
    ConnectType getConnectType();

    /**
     * 获取连接状态
     *
     * @author huangyajun
     * @date 2022/4/21 10:33
     */
    ConnectStatus getConnectStatus();

}
