package com.accelerometer.app.bluetooth;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import com.accelerometer.app.data.SensorSample;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver;
import com.wit.witsdk.modular.sensor.device.exceptions.OpenDeviceException;
import com.wit.witsdk.modular.sensor.modular.processor.constant.WitSensorKey;
import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble;
import com.wit.witsdk.modular.sensor.example.ble5.interfaces.IBwt901bleRecordObserver;
import kotlinx.coroutines.flow.SharedFlow;
import kotlinx.coroutines.flow.StateFlow;

/**
 * Сервис, обёртывающий SDK WitMotion BLE 5.0.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0090\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010!\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\n\u0018\u0000 >2\u00020\u00012\u00020\u0002:\u0003>?@B\r\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0002\u0010\u0005J\b\u0010 \u001a\u00020!H\u0002J\u0010\u0010\"\u001a\u00020!2\u0006\u0010#\u001a\u00020\u0015H\u0002J\u000e\u0010$\u001a\u00020!2\u0006\u0010%\u001a\u00020\u001bJ\u0006\u0010&\u001a\u00020!J\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u001b0(J\b\u0010)\u001a\u00020!H\u0002J\u0012\u0010*\u001a\u00020+2\b\u0010,\u001a\u0004\u0018\u00010-H\u0002J\u0010\u0010.\u001a\u00020!2\u0006\u0010/\u001a\u000200H\u0016J\u0010\u00101\u001a\u00020!2\u0006\u00102\u001a\u000203H\u0016J\u0010\u00104\u001a\u00020!2\u0006\u00105\u001a\u00020\u0015H\u0016J\u0019\u00106\u001a\u0004\u0018\u0001072\b\u00108\u001a\u0004\u0018\u00010-H\u0002\u00a2\u0006\u0002\u00109J\u0019\u0010:\u001a\u0004\u0018\u0001072\b\u00108\u001a\u0004\u0018\u00010-H\u0002\u00a2\u0006\u0002\u00109J\u0006\u0010;\u001a\u00020!J\u0006\u0010<\u001a\u00020!J\u0006\u0010=\u001a\u00020!R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\b0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\n0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0011R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00150\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001b0\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\r0\u001d\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001f\u00a8\u0006A"}, d2 = {"Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService;", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/interfaces/IBluetoothFoundObserver;", "Lcom/wit/witsdk/modular/sensor/example/ble5/interfaces/IBwt901bleRecordObserver;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_batteryLevel", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_connectionState", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$ConnectionState;", "_sensorSamples", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/accelerometer/app/data/SensorSample;", "batteryLevel", "Lkotlinx/coroutines/flow/StateFlow;", "getBatteryLevel", "()Lkotlinx/coroutines/flow/StateFlow;", "bluetoothManager", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/WitBluetoothManager;", "connectedDevice", "Lcom/wit/witsdk/modular/sensor/example/ble5/Bwt901ble;", "connectionState", "getConnectionState", "devices", "", "discoveredDevices", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$DiscoveredDevice;", "sensorSamples", "Lkotlinx/coroutines/flow/SharedFlow;", "getSensorSamples", "()Lkotlinx/coroutines/flow/SharedFlow;", "clearDevices", "", "configureSensor", "sensor", "connectToDevice", "device", "disconnect", "getDiscoveredDevices", "", "initBluetoothManager", "matchesDeviceName", "", "deviceName", "", "onFoundBle", "bluetoothBLE", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/BluetoothBLE;", "onFoundSPP", "bluetoothSPP", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/BluetoothSPP;", "onRecord", "bwt901ble", "parseAccelerationG", "", "raw", "(Ljava/lang/String;)Ljava/lang/Double;", "parseAngleDegrees", "startDiscovery", "startDiscoveryForSelection", "stopDiscovery", "Companion", "ConnectionState", "DiscoveredDevice", "app_debug"})
public final class BluetoothAccelerometerService implements com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver, com.wit.witsdk.modular.sensor.example.ble5.interfaces.IBwt901bleRecordObserver {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "BluetoothAccelerometer";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> DEVICE_NAME_FILTER = null;
    @org.jetbrains.annotations.Nullable()
    private com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager bluetoothManager;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble> devices = null;
    @org.jetbrains.annotations.Nullable()
    private com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble connectedDevice;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.DiscoveredDevice> discoveredDevices = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState> _connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState> connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.accelerometer.app.data.SensorSample> _sensorSamples = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.accelerometer.app.data.SensorSample> sensorSamples = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _batteryLevel = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> batteryLevel = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.accelerometer.app.bluetooth.BluetoothAccelerometerService.Companion Companion = null;
    
    public BluetoothAccelerometerService(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState> getConnectionState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.accelerometer.app.data.SensorSample> getSensorSamples() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getBatteryLevel() {
        return null;
    }
    
    private final void initBluetoothManager() {
    }
    
    /**
     * Начать поиск устройств и подключиться к первому найденному датчику.
     */
    public final void startDiscovery() {
    }
    
    /**
     * Начать поиск устройств без автоподключения.
     * Найденные устройства добавляются в список discoveredDevices.
     */
    public final void startDiscoveryForSelection() {
    }
    
    /**
     * Получить список найденных устройств.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.DiscoveredDevice> getDiscoveredDevices() {
        return null;
    }
    
    /**
     * Подключиться к выбранному устройству.
     */
    public final void connectToDevice(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.bluetooth.BluetoothAccelerometerService.DiscoveredDevice device) {
    }
    
    /**
     * Остановить поиск устройств.
     */
    public final void stopDiscovery() {
    }
    
    /**
     * Отключение от текущего устройства.
     */
    public final void disconnect() {
    }
    
    @java.lang.Override()
    public void onFoundBle(@org.jetbrains.annotations.NotNull()
    com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE bluetoothBLE) {
    }
    
    /**
     * Настройка датчика: частота 50 Гц, разблокировка регистров
     */
    private final void configureSensor(com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble sensor) {
    }
    
    @java.lang.Override()
    public void onFoundSPP(@org.jetbrains.annotations.NotNull()
    com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP bluetoothSPP) {
    }
    
    @java.lang.Override()
    public void onRecord(@org.jetbrains.annotations.NotNull()
    com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble bwt901ble) {
    }
    
    private final java.lang.Double parseAccelerationG(java.lang.String raw) {
        return null;
    }
    
    private final java.lang.Double parseAngleDegrees(java.lang.String raw) {
        return null;
    }
    
    private final void clearDevices() {
    }
    
    private final boolean matchesDeviceName(java.lang.String deviceName) {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$Companion;", "", "()V", "DEVICE_NAME_FILTER", "", "", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$ConnectionState;", "", "(Ljava/lang/String;I)V", "DISCONNECTED", "CONNECTING", "CONNECTED", "app_debug"})
    public static enum ConnectionState {
        /*public static final*/ DISCONNECTED /* = new DISCONNECTED() */,
        /*public static final*/ CONNECTING /* = new CONNECTING() */,
        /*public static final*/ CONNECTED /* = new CONNECTED() */;
        
        ConnectionState() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState> getEntries() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u001f\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\u000b\u0010\r\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0006H\u00c6\u0003J)\u0010\u0010\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0014\u001a\u00020\u0015H\u00d6\u0001J\t\u0010\u0016\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000b\u00a8\u0006\u0017"}, d2 = {"Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$DiscoveredDevice;", "", "name", "", "mac", "bluetoothBLE", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/BluetoothBLE;", "(Ljava/lang/String;Ljava/lang/String;Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/BluetoothBLE;)V", "getBluetoothBLE", "()Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/BluetoothBLE;", "getMac", "()Ljava/lang/String;", "getName", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class DiscoveredDevice {
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String name = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String mac = null;
        @org.jetbrains.annotations.NotNull()
        private final com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE bluetoothBLE = null;
        
        public DiscoveredDevice(@org.jetbrains.annotations.Nullable()
        java.lang.String name, @org.jetbrains.annotations.NotNull()
        java.lang.String mac, @org.jetbrains.annotations.NotNull()
        com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE bluetoothBLE) {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMac() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE getBluetoothBLE() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.accelerometer.app.bluetooth.BluetoothAccelerometerService.DiscoveredDevice copy(@org.jetbrains.annotations.Nullable()
        java.lang.String name, @org.jetbrains.annotations.NotNull()
        java.lang.String mac, @org.jetbrains.annotations.NotNull()
        com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE bluetoothBLE) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}