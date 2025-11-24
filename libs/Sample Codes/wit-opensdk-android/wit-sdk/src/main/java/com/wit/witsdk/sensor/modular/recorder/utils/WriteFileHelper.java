package com.wit.witsdk.sensor.modular.recorder.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 记录文件帮助类
 *
 * @author huangyajun
 * @date 2022/5/20 9:56
 */
public class WriteFileHelper {

    private FileOutputStream fout;

    private File file;

    public WriteFileHelper(String filePath) throws FileNotFoundException {
        file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        fout = new FileOutputStream(filePath, false);
    }

    public void write(String str) throws IOException {
        byte[] bytes = str.getBytes();
        fout.write(bytes);
    }

    public void close() throws IOException {
        fout.close();
        fout.flush();
    }
}
