package com.wit.witsdk.ui;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * byte数组转换工具类
 *
 * @author huangyajun
 * @date 2022/4/21 10:51
 */
public class ByteArrayConvert {

    /**
     * byte数组转string
     *
     * @author huangyajun
     * @date 2022/4/21 11:07
     */
    public static String ByteArrayToString(byte[] data) {
        return new String(data);
    }

    /**
     * string转byte数组
     *
     * @author huangyajun
     * @date 2022/4/21 11:07
     */
    public static byte[] StringToByteArray(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * byte数组转16进制字符串
     *
     * @author huangyajun
     * @date 2022/4/21 11:08
     */
    public static String ByteArrayToHexString(byte[] data) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i = 0; i < data.length; i++) {
            temp = Integer.toHexString(data[i] & 0xFF);
            if (temp.length() == 1) {
                // 1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    /**
     * byte数组转16进制字符串
     *
     * @author huangyajun
     * @date 2022/4/21 11:08
     */
    public static String ByteArrayToHexString(Byte[] data) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i = 0; i < data.length; i++) {
            temp = Integer.toHexString(data[i] & 0xFF);
            if (temp.length() == 1) {
                // 1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    public static String ByteArrayToHexString(List<Byte> data) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i = 0; i < data.size(); i++) {
            temp = Integer.toHexString(data.get(i) & 0xFF);
            if (temp.length() == 1) {
                // 1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    /**
     * 16进制字符串转byte数组
     *
     * @author huangyajun
     * @date 2022/4/21 11:09
     */
    public static byte[] HexStringToByteArray(String data) {
        String[] hexArray = data.split(" ");
        byte[] byteArray = new byte[hexArray.length];
        for (int i = 0; i < hexArray.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(hexArray[i].toUpperCase(), 16);
        }
        return byteArray;
    }
//
//    /**
//     * byte数组转10进制字符串
//     *
//     * @author huangyajun
//     * @date 2022/4/21 11:09
//     */
//    public static String ByteArrayToDecString(byte[] data) {
//        // throw new Exception();
//        return null;
//    }
//
//    /**
//     * 10进制字符串转byte数组
//     *
//     * @author huangyajun
//     * @date 2022/4/21 11:10
//     */
//    public static byte[] DecStringToByteArray(String data) {
//        return null;
//    }
//
//    /**
//     * byte数组转八进制字符串
//     *
//     * @author huangyajun
//     * @date 2022/4/21 11:10
//     */
//    public static String ByteArrayToOtcString(byte[] data) {
//        return null;
//    }
//
//    /**
//     * 八进制字符串转byte数组
//     *
//     * @author huangyajun
//     * @date 2022/4/21 11:11
//     */
//    public static byte[] OtcStringToByteArray(String data) {
//        return null;
//    }
//
//    /**
//     * 二进制字符串转byte数组
//     *
//     * @author huangyajun
//     * @date 2022/4/21 11:12
//     */
//    public static byte[] BinStringToByteArray(String data) {
//        return null;
//    }
//
//    /**
//     * byte数组转二进制字符串
//     *
//     * @author huangyajun
//     * @date 2022/4/21 11:12
//     */
//    public static String ByteArrayToBinString(byte[] data) {
//        return null;
//    }
}
