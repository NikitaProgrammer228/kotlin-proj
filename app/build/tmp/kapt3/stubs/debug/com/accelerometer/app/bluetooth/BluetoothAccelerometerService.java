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
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010!\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0007\u0018\u0000 02\u00020\u00012\u00020\u0002:\u000201B\r\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0002\u0010\u0005J\b\u0010\u001a\u001a\u00020\u001bH\u0002J\u0006\u0010\u001c\u001a\u00020\u001bJ\b\u0010\u001d\u001a\u00020\u001bH\u0002J\u0012\u0010\u001e\u001a\u00020\u001f2\b\u0010 \u001a\u0004\u0018\u00010!H\u0002J\u0010\u0010\"\u001a\u00020\u001b2\u0006\u0010#\u001a\u00020$H\u0016J\u0010\u0010%\u001a\u00020\u001b2\u0006\u0010&\u001a\u00020\'H\u0016J\u0010\u0010(\u001a\u00020\u001b2\u0006\u0010)\u001a\u00020\u000fH\u0016J\u0019\u0010*\u001a\u0004\u0018\u00010+2\b\u0010,\u001a\u0004\u0018\u00010!H\u0002\u00a2\u0006\u0002\u0010-J\u0006\u0010.\u001a\u00020\u001bJ\u0006\u0010/\u001a\u00020\u001bR\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\b0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019\u00a8\u00062"}, d2 = {"Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService;", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/interfaces/IBluetoothFoundObserver;", "Lcom/wit/witsdk/modular/sensor/example/ble5/interfaces/IBwt901bleRecordObserver;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_connectionState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$ConnectionState;", "_sensorSamples", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/accelerometer/app/data/SensorSample;", "bluetoothManager", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/WitBluetoothManager;", "connectedDevice", "Lcom/wit/witsdk/modular/sensor/example/ble5/Bwt901ble;", "connectionState", "Lkotlinx/coroutines/flow/StateFlow;", "getConnectionState", "()Lkotlinx/coroutines/flow/StateFlow;", "devices", "", "sensorSamples", "Lkotlinx/coroutines/flow/SharedFlow;", "getSensorSamples", "()Lkotlinx/coroutines/flow/SharedFlow;", "clearDevices", "", "disconnect", "initBluetoothManager", "matchesDeviceName", "", "deviceName", "", "onFoundBle", "bluetoothBLE", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/BluetoothBLE;", "onFoundSPP", "bluetoothSPP", "Lcom/wit/witsdk/modular/sensor/modular/connector/modular/bluetooth/BluetoothSPP;", "onRecord", "bwt901ble", "parseRawAcceleration", "", "raw", "(Ljava/lang/String;)Ljava/lang/Integer;", "startDiscovery", "stopDiscovery", "Companion", "ConnectionState", "app_debug"})
public final class BluetoothAccelerometerService implements com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver, com.wit.witsdk.modular.sensor.example.ble5.interfaces.IBwt901bleRecordObserver {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "BluetoothAccelerometer";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> DEVICE_NAME_FILTER = null;
    private static final float RAW_MULTIPLIER = 16384.0F;
    @org.jetbrains.annotations.Nullable()
    private com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager bluetoothManager;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble> devices = null;
    @org.jetbrains.annotations.Nullable()
    private com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble connectedDevice;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState> _connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState> connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.accelerometer.app.data.SensorSample> _sensorSamples = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.accelerometer.app.data.SensorSample> sensorSamples = null;
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
    
    private final void initBluetoothManager() {
    }
    
    /**
     * Начать поиск устройств и подключиться к первому найденному датчику.
     */
    public final void startDiscovery() {
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
    
    @java.lang.Override()
    public void onFoundSPP(@org.jetbrains.annotations.NotNull()
    com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP bluetoothSPP) {
    }
    
    @java.lang.Override()
    public void onRecord(@org.jetbrains.annotations.NotNull()
    com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble bwt901ble) {
    }
    
    private final java.lang.Integer parseRawAcceleration(java.lang.String raw) {
        return null;
    }
    
    private final void clearDevices() {
    }
    
    private final boolean matchesDeviceName(java.lang.String deviceName) {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$Companion;", "", "()V", "DEVICE_NAME_FILTER", "", "", "RAW_MULTIPLIER", "", "TAG", "app_debug"})
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
}