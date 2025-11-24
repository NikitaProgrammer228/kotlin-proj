package com.wit.witsdk.sensor.modular.connector.entity;

/**
 * ch340usb选项
 *
 * @author huangyajun
 * @date 2022/4/26 20:00
 */
public class Ch340UsbOption {

    private int baud = 115200;

    public int getBaud() {
        return baud;
    }

    public void setBaud(int baud) {
        this.baud = baud;
    }
}