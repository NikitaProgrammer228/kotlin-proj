package com.wit.witsdk.sensor.context;

import android.content.Context;
import android.content.SharedPreferences;

import com.wit.witsdk.sensor.entity.WitProductOption;
import com.wit.witsdk.sensor.interfaces.BaseProduct;
import com.wit.witsdk.sensor.interfaces.IProduct;
import com.wit.witsdk.sensor.utils.ClassesReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备型号管理类
 *
 * @author huangyajun
 * @date 2023/2/2 9:53
 */
public class ProductModelManager {


    /**
     * 所有的传感器配置的类型
     */
    private static List<Class<? extends IProduct>> productClassList = new ArrayList<>();

    /**
     * 所有的传感器配置的构建类
     */
    private static Map<String, IProduct> productMap = new HashMap<String, IProduct>();

    /**
     * 所有的传感器配置
     */
    private static Map<String, WitProductOption> productOptionMap = new HashMap<String, WitProductOption>();

    /**
     * 默认的型号
     */
    public static final String DEFAULT_MODEL = "BWT901BLECL5.0";

    /**
     * 当前选择的型号
     */
    private static String currentModel = DEFAULT_MODEL;

    /**
     * 上下文
     */
    private static Context context;

    /**
     * 加载数据
     *
     * @author huangyajun
     * @date 2023/2/2 10:29
     */
    public static void loadData(Context context) {
        ProductModelManager.context = context;
        List<Class<? extends IProduct>> classes = loadProductList(context);
        try {
            loadProductClassList(classes, context);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * 加载所有的子类
     *
     * @author huangyajun
     * @date 2023/2/1 17:27
     */
    private static List<Class<? extends IProduct>> loadProductList(Context context) {
        List<Class<?>> classList = ClassesReader.reader("com.wit", context);
        List<Class<? extends IProduct>> subTypesOf = ClassesReader.getSubTypesOf(classList, BaseProduct.class);
        return subTypesOf;
    }

    /**
     * 设置传感器列表
     *
     * @author huangyajun
     * @date 2023/2/2 9:55
     */
    private static void loadProductClassList(List<Class<? extends IProduct>> productClassList, Context context) throws Exception {
        if (productClassList == null || productClassList.size() == 0) {
            throw new Exception("传感器型号不能为0个");
        }

        String saveModel = getSaveModel();

        ProductModelManager.productClassList = productClassList;
        productMap.clear();

        WitProductOption defaultSensor = null;
        WitProductOption saveSensor = null;

        for (int i = 0; i < productClassList.size(); i++) {

            Class<? extends IProduct> sensor = productClassList.get(i);
            IProduct iSensor = sensor.newInstance();


            // 加载上下文
            iSensor.setContext(context);
            // 存入map
            WitProductOption sensorOption = iSensor.getSensorOption();

            if (sensorOption == null) {
                throw new Exception("getSensorOption 返回的传感器配置不可为空，类: " + sensor.getName());
            }

            productMap.put(sensorOption.getModel(), iSensor);
            productOptionMap.put(sensorOption.getModel(), sensorOption);

            // 默认的设备
            if (DEFAULT_MODEL.equals(sensorOption.getModel())) {
                defaultSensor = sensorOption;
            }

            if (saveModel.equals(sensorOption.getModel())) {
                saveSensor = sensorOption;
            }
        }

        // 如果能找到保存的型号就用保存的型号
        if (saveSensor != null) {
            // 设置当前模式
            setCurrentModel(saveSensor.getModel());
        } else {
            // 设置当前模式
            setCurrentModel(defaultSensor.getModel());
        }
    }

    /**
     * 设置当前型号
     *
     * @author huangyajun
     * @date 2023/2/2 9:59
     */
    public static void setCurrentModel(String currentModel) {
        ProductModelManager.currentModel = currentModel;
        saveModel(currentModel);
    }

    /**
     * 获得保存的产品型号
     */
    public static String getSaveModel() {
        SharedPreferences sp = context.getSharedPreferences("sp_config", Context.MODE_PRIVATE);
        // 获取sp全部数据
        Map<String, ?> all = sp.getAll();
        Object baseModel = all.get("CurrentModel");
        if (baseModel == null) {
            return "";
        }
        return (String) baseModel;
    }

    /**
     * 保存配置
     *
     * @author huangyajun
     * @date 2022/5/9 19:38
     */
    public static void saveModel(String value) {
        SharedPreferences sp = context.getSharedPreferences("sp_config", Context.MODE_PRIVATE);
        sp.edit().putString("CurrentModel", value).apply();
    }

    /**
     * 获得传感器
     *
     * @author huangyajun
     * @date 2023/2/2 10:06
     */
    public static WitProductOption getProduct(String sensorModel) {
        return productOptionMap.get(sensorModel);
    }

    /**
     * 获得当前传感器
     *
     * @author huangyajun
     * @date 2023/2/2 10:07
     */
    public static WitProductOption getCurrentProduct() {
        WitProductOption sensor = getProduct(currentModel);

        if (sensor == null) {
            // 返回空的配置
            try {
                throw new Exception("当前型号不可为空");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        return sensor;
    }

    /**
     * 获得所有传感器配置
     *
     * @author huangyajun
     * @date 2023/2/2 16:49
     */
    public static List<WitProductOption> getProductList() {
        Collection<WitProductOption> values = productOptionMap.values();
        return new ArrayList<>(values);
    }
}
