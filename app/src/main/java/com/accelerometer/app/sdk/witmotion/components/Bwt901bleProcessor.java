package com.accelerometer.app.sdk.witmotion.components;

import android.util.Log;

import com.accelerometer.app.sdk.witmotion.data.WitSensorKey;
import com.wit.witsdk.sensor.modular.connector.entity.BluetoothBLEOption;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.sensor.modular.connector.roles.WitCoreConnect;
import com.wit.witsdk.sensor.modular.device.DeviceModel;
import com.wit.witsdk.sensor.modular.processor.interfaces.IDataProcessor;
import com.wit.witsdk.sensor.utils.DipSensorMagHelper;
import com.wit.witsdk.utils.BitConvert;
import com.wit.witsdk.utils.NumberFormat;
import com.wit.witsdk.utils.StringUtils;

/**
 * Обработка данных датчика (адаптация из SDK-примера).
 */
public class Bwt901bleProcessor implements IDataProcessor {

    private boolean readDataThreadRunning = false;
    private DeviceModel deviceModel;

    @Override
    public void OnOpen(DeviceModel deviceModel) {
        this.deviceModel = deviceModel;
        readDataThreadRunning = true;
        Thread thread = new Thread(this::readDataThread);
        thread.start();
    }

    private void sendProtocolData(DeviceModel deviceModel, byte[] bytes, int delay) {
        deviceModel.sendProtocolData(bytes, delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Log.e("Bwt901bleProcessor", "sendProtocolData interrupted", e);
        }
    }

    private void readDataThread() {
        int count = 0;
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.e("Bwt901bleProcessor", "init delay interrupted", e);
        }

        while (readDataThreadRunning) {
            try {

                String magType = deviceModel.getDeviceData("72");
                if (StringUtils.IsNullOrEmpty(magType)) {
                    sendProtocolData(deviceModel, new byte[]{(byte) 0xff, (byte) 0xaa, 0x27, 0x72, 0x00}, 150);
                    sendProtocolData(deviceModel, new byte[]{(byte) 0xff, (byte) 0xaa, 0x27, 0x72, 0x00}, 150);
                }

                String reg2e = deviceModel.getDeviceData("2E");
                String reg2f = deviceModel.getDeviceData("2F");
                if (StringUtils.IsNullOrEmpty(reg2e) || StringUtils.IsNullOrEmpty(reg2f)) {
                    sendProtocolData(deviceModel, new byte[]{(byte) 0xff, (byte) 0xaa, 0x27, 0x2E, 0x00}, 150);
                }

                sendProtocolData(deviceModel, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x3a, (byte) 0x00}, 150);
                sendProtocolData(deviceModel, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x51, (byte) 0x00}, 150);

                if (count++ % 50 == 0 || count < 5) {
                    sendProtocolData(deviceModel, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x64, (byte) 0x00}, 150);
                    sendProtocolData(deviceModel, new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x27, (byte) 0x40, (byte) 0x00}, 150);
                }

                if (count % 5 == 0) {
                    WitCoreConnect coreConnect = deviceModel.getCoreConnect();
                    BluetoothBLEOption bluetoothBLEOption = coreConnect.getConfig().getBluetoothBLEOption();
                    deviceModel.setDeviceData(WitSensorKey.Rssi, WitBluetoothManager.getRssi(bluetoothBLEOption.getMac()) + "");
                }
            } catch (Exception e) {
                Log.e("Bwt901bleProcessor", "readDataThread error", e);
            }
        }
    }

    @Override
    public void OnClose() {
        readDataThreadRunning = false;
    }

    @Override
    public void OnUpdate(DeviceModel deviceModel) {

        String regAx = deviceModel.getDeviceData("61_0");
        String regAy = deviceModel.getDeviceData("61_1");
        String regAz = deviceModel.getDeviceData("61_2");

        String regWx = deviceModel.getDeviceData("61_3");
        String regWy = deviceModel.getDeviceData("61_4");
        String regWz = deviceModel.getDeviceData("61_5");

        String regAngleX = deviceModel.getDeviceData("61_6");
        String regAngleY = deviceModel.getDeviceData("61_7");
        String regAngleZ = deviceModel.getDeviceData("61_8");

        String regQ1 = deviceModel.getDeviceData("51");
        String regQ2 = deviceModel.getDeviceData("52");
        String regQ3 = deviceModel.getDeviceData("53");
        String regQ4 = deviceModel.getDeviceData("54");
        String regTemperature = deviceModel.getDeviceData("40");
        String regPower = deviceModel.getDeviceData("64");

        String reg2e = deviceModel.getDeviceData("2E");
        String reg2f = deviceModel.getDeviceData("2F");

        if (!StringUtils.IsNullOrEmpty(reg2e) &&
                !StringUtils.IsNullOrEmpty(reg2f)) {
            short reg2eValue = Short.parseShort(reg2e);
            short reg2fValue = Short.parseShort(reg2f);

            int tempVerSion = BitConvert.byte2Int(new byte[]{
                    BitConvert.short2byte(reg2fValue)[0],
                    BitConvert.short2byte(reg2fValue)[1],
                    BitConvert.short2byte(reg2eValue)[0],
                    BitConvert.short2byte(reg2eValue)[1]
            });

            String sbinary = Integer.toBinaryString(tempVerSion);
            sbinary = StringUtils.padLeft(sbinary, 32, '0');
            if (sbinary.substring(0, 1).equals("1")) {
                String tempNewVS = Integer.parseInt(sbinary.substring(2, 18), 2) + "";
                tempNewVS += "." + Integer.parseInt(sbinary.substring(19, 24), 2);
                tempNewVS += "." + Integer.parseInt(sbinary.substring(25), 2);
                deviceModel.setDeviceData(WitSensorKey.VersionNumber, tempNewVS);
            } else {
                int tempNewVS = BitConvert.byte2Int(new byte[]{
                        0,
                        0,
                        BitConvert.short2byte(reg2eValue)[0],
                        BitConvert.short2byte(reg2eValue)[1]
                });
                deviceModel.setDeviceData(WitSensorKey.VersionNumber, tempNewVS + "");
            }
        }

        if (!StringUtils.IsNullOrEmpty(regAx)) {
            deviceModel.setDeviceData(WitSensorKey.AccX, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regAx) / 32768 * 16));
        }
        if (!StringUtils.IsNullOrEmpty(regAy)) {
            deviceModel.setDeviceData(WitSensorKey.AccY, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regAy) / 32768 * 16));
        }
        if (!StringUtils.IsNullOrEmpty(regAz)) {
            deviceModel.setDeviceData(WitSensorKey.AccZ, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regAz) / 32768 * 16));
        }

        if (!StringUtils.IsNullOrEmpty(regWx)) {
            deviceModel.setDeviceData(WitSensorKey.AsX, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regWx) / 32768 * 2000));
        }
        if (!StringUtils.IsNullOrEmpty(regWy)) {
            deviceModel.setDeviceData(WitSensorKey.AsY, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regWy) / 32768 * 2000));
        }
        if (!StringUtils.IsNullOrEmpty(regWz)) {
            deviceModel.setDeviceData(WitSensorKey.AsZ, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regWz) / 32768 * 2000));
        }

        if (!StringUtils.IsNullOrEmpty(regAngleX)) {
            deviceModel.setDeviceData(WitSensorKey.AngleX, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regAngleX) / 32768 * 180));
        }
        if (!StringUtils.IsNullOrEmpty(regAngleY)) {
            deviceModel.setDeviceData(WitSensorKey.AngleY, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regAngleY) / 32768 * 180));
        }
        if (!StringUtils.IsNullOrEmpty(regAngleZ)) {
            deviceModel.setDeviceData(WitSensorKey.AngleZ, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regAngleZ) / 32768 * 180));
        }

        String regHX = deviceModel.getDeviceData("3A");
        String regHY = deviceModel.getDeviceData("3B");
        String regHZ = deviceModel.getDeviceData("3C");
        String magType = deviceModel.getDeviceData("72");
        if (!StringUtils.IsNullOrEmpty(regHX) &&
                !StringUtils.IsNullOrEmpty(regHY) &&
                !StringUtils.IsNullOrEmpty(regHZ) &&
                !StringUtils.IsNullOrEmpty(magType)) {
            short type = Short.parseShort(magType);
            deviceModel.setDeviceData(WitSensorKey.HX, DipSensorMagHelper.GetMagToUt(type, Double.parseDouble(regHX)) + "");
            deviceModel.setDeviceData(WitSensorKey.HY, DipSensorMagHelper.GetMagToUt(type, Double.parseDouble(regHY)) + "");
            deviceModel.setDeviceData(WitSensorKey.HZ, DipSensorMagHelper.GetMagToUt(type, Double.parseDouble(regHZ)) + "");
        }

        if (!StringUtils.IsNullOrEmpty(regTemperature)) {
            deviceModel.setDeviceData(WitSensorKey.T, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regTemperature) / 100));
        }

        if (!StringUtils.IsNullOrEmpty(regPower)) {

            int regPowerValue = Integer.parseInt(regPower);

            float eqPercent = getEqPercent((float) (regPowerValue / 100.0));
            deviceModel.setDeviceData(WitSensorKey.ElectricQuantityPercentage, eqPercent + "");
            deviceModel.setDeviceData(WitSensorKey.ElectricQuantity, regPowerValue + "");
        }

        if (!StringUtils.IsNullOrEmpty(regQ1)) {
            deviceModel.setDeviceData(WitSensorKey.Q0, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regQ1) / 32768.0));
        }
        if (!StringUtils.IsNullOrEmpty(regQ2)) {
            deviceModel.setDeviceData(WitSensorKey.Q1, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regQ2) / 32768.0));
        }
        if (!StringUtils.IsNullOrEmpty(regQ3)) {
            deviceModel.setDeviceData(WitSensorKey.Q2, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regQ3) / 32768.0));
        }
        if (!StringUtils.IsNullOrEmpty(regQ4)) {
            deviceModel.setDeviceData(WitSensorKey.Q3, NumberFormat.formatDoubleToString("%.3f", Double.parseDouble(regQ4) / 32768.0));
        }
    }

    public float getEqPercent(float eq) {
        float p;
        if (eq > 5.50) {
            p = Interp(eq,
                    new float[]{6.5f, 6.8f, 7.35f, 7.75f, 8.5f, 8.8f},
                    new float[]{0, 10, 30, 60, 90, 100});
        } else {
            p = Interp(eq,
                    new float[]{3.4f, 3.5f, 3.68f, 3.7f, 3.73f, 3.77f, 3.79f, 3.82f, 3.87f, 3.93f, 3.96f, 3.99f},
                    new float[]{0, 5, 10, 15, 20, 30, 40, 50, 60, 75, 90, 100});
        }
        return p;
    }

    public float Interp(float a, float[] x, float[] y) {
        float v = 0;
        int L = x.length;
        if (a < x[0]) v = y[0];
        else if (a > x[L - 1]) v = y[L - 1];
        else {
            for (int i = 0; i < y.length - 1; i++) {
                if (a > x[i + 1]) continue;
                v = y[i] + (a - x[i]) / (x[i + 1] - x[i]) * (y[i + 1] - y[i]);
                break;
            }
        }
        return v;
    }
}


