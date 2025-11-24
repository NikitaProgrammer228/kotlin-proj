package com.accelerometer.app.sdk.witmotion.components;

import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.resolver.entity.SendDataResult;
import com.wit.witsdk.sensor.modular.resolver.interfaces.IProtocolResolver;
import com.wit.witsdk.sensor.modular.resolver.interfaces.ISendDataCallback;
import com.wit.witsdk.utils.BitConvert;
import com.wit.witsdk.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Парсер протокола BLE 5.0 (адаптация из SDK-примера).
 */
public class Bwt901bleResolver implements IProtocolResolver {

    private List<Byte> activeByteDataBuffer = new ArrayList<>();
    private List<Byte> activeByteTemp = new ArrayList<>();

    @Override
    public void sendData(byte[] sendData, DeviceModel deviceModel, int waitTime, ISendDataCallback callback) {

        if (waitTime < 0) {
            waitTime = 100;
        }

        try {
            final int finalWaitTime = waitTime;
            deviceModel.sendData(sendData, rtnBytes -> {
                Byte[] returnData = rtnBytes;
                if (sendData != null && sendData.length >= 5 && sendData[2] == 0x27 && returnData != null && returnData.length >= 20) {

                    returnData = findReturnData(returnData);
                    if (returnData != null && returnData.length == 20) {
                        int readReg = sendData[4] << 8 | sendData[3];
                        int rtnReg = returnData[3] << 8 | returnData[2];

                        if (readReg == rtnReg) {
                            short[] pack = new short[9];

                            for (int i = 0; i < 4; i++) {
                                pack[i] = BitConvert.byte2short(new byte[]{returnData[5 + (i * 2)], returnData[4 + (i * 2)]});

                                String reg = Integer.toHexString(readReg + i).toUpperCase();
                                reg = StringUtils.padLeft(reg, 2, '0');
                                deviceModel.setDeviceData(reg, pack[i] + "");
                            }
                        }
                    }
                }
                Thread th = new Thread(() -> callback.run(new SendDataResult(true)));
                th.start();
            }, finalWaitTime, 1);
        } catch (Exception ex) {
            Thread th = new Thread(() -> callback.run(new SendDataResult(false)));
            th.start();
        }
    }

    @Override
    public void sendData(byte[] sendData, DeviceModel deviceModel) {
        sendData(sendData, deviceModel, -1, (res) -> {
        });
    }

    public static Byte[] findReturnData(Byte[] returnData) {

        List<Byte> bytes = Arrays.asList(returnData);

        List<Byte> tempArr;

        for (int i = 0; i < bytes.size(); i++) {
            if (bytes.size() - i >= 20) {
                tempArr = bytes.subList(i, i + 20);
                if (tempArr.size() == 20 && tempArr.get(0) == 0x55 && tempArr.get(1) == 0x71) {
                    return tempArr.toArray(new Byte[0]);
                }
            }
        }
        return null;
    }

    @Override
    public void passiveReceiveData(byte[] data, DeviceModel deviceModel) {

        if (data.length < 1) {
            return;
        }

        for (byte datum : data) {
            activeByteDataBuffer.add(datum);
        }

        while (activeByteDataBuffer.size() > 1 && activeByteDataBuffer.get(0) != 0x55 && activeByteDataBuffer.get(1) != 0x61) {
            activeByteDataBuffer.remove(0);
        }

        while (activeByteDataBuffer.size() >= 20) {
            activeByteTemp = new ArrayList<>(activeByteDataBuffer.subList(0, 20));
            activeByteDataBuffer = new ArrayList<>(activeByteDataBuffer.subList(20, activeByteDataBuffer.size()));

            if (activeByteTemp.get(0) == 0x55 && activeByteTemp.get(1) == 0x61 && activeByteTemp.size() == 20) {
                float[] fData = new float[9];
                int iStart = 0;
                for (int i = 0; i < 9; i++) {
                    fData[i] = (((short) activeByteTemp.get(iStart + i * 2 + 3)) << 8) | ((short) activeByteTemp.get(iStart + i * 2 + 2) & 0xff);
                    String identify = Integer.toHexString(activeByteTemp.get(1));
                    identify = StringUtils.padLeft(identify, 2, '0');
                    deviceModel.setDeviceData(identify + "_" + i, (fData[i]) + "");
                }
            }
        }
    }
}


