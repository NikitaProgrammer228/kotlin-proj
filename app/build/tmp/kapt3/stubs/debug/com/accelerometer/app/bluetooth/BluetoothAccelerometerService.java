package com.accelerometer.app.bluetooth;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.accelerometer.app.data.AccelerometerData;
import com.accelerometer.app.sdk.witmotion.Bwt901ble;
import com.accelerometer.app.sdk.witmotion.data.WitSensorKey;
import com.accelerometer.app.sdk.witmotion.interfaces.IBwt901bleRecordObserver;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.BluetoothSPP;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException;
import com.wit.witsdk.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver;
import com.wit.witsdk.sensor.modular.device.exceptions.OpenDeviceException;
import kotlinx.coroutines.flow.StateFlow;

/**
 * Сервис, обёртывающий SDK WitMotion BLE 5.0.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010!\n\u0000\n\u0002\u0010\u0002\n\u0002\b\f\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\u0018\u0000 /2\u00020\u0001:\u0002/0B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u001b\u001a\u00020\u001cH\u0002J\u0006\u0010\u001d\u001a\u00020\u001cJ\u0006\u0010\u001e\u001a\u00020\u001cJ\b\u0010\u001f\u001a\u00020\u001cH\u0002J\u0015\u0010 \u001a\u00020\u001c2\u0006\u0010!\u001a\u00020\u0011H\u0016\u00a2\u0006\u0002\u0010\"J\u0015\u0010#\u001a\u00020\u001c2\u0006\u0010!\u001a\u00020\u0011H\u0016\u00a2\u0006\u0002\u0010\"J\u0015\u0010$\u001a\u00020\u001c2\u0006\u0010%\u001a\u00020\u0011H\u0016\u00a2\u0006\u0002\u0010\"J\u0010\u0010&\u001a\u00020\u001c2\u0006\u0010\'\u001a\u00020\u0014H\u0016J\u0019\u0010(\u001a\u0004\u0018\u00010)2\b\u0010*\u001a\u0004\u0018\u00010+H\u0002\u00a2\u0006\u0002\u0010,J\u0006\u0010-\u001a\u00020\u001cJ\u0006\u0010.\u001a\u00020\u001cR\u0016\u0010\u0005\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u000b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0010\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0012R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\t0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0017\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u000b0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u000fR\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00140\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00061"}, d2 = {"Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService;", "Lcom/accelerometer/app/sdk/witmotion/interfaces/IBwt901bleRecordObserver;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_accelerometerData", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/accelerometer/app/data/AccelerometerData;", "_connectionState", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$ConnectionState;", "_dataHistory", "", "accelerometerData", "Lkotlinx/coroutines/flow/StateFlow;", "getAccelerometerData", "()Lkotlinx/coroutines/flow/StateFlow;", "bluetoothManager", "error/NonExistentClass", "Lerror/NonExistentClass;", "connectedDevice", "Lcom/accelerometer/app/sdk/witmotion/Bwt901ble;", "connectionState", "getConnectionState", "dataHistory", "getDataHistory", "devices", "", "clearDevices", "", "clearHistory", "disconnect", "initBluetoothManager", "onFoundBle", "bluetoothBLE", "(Lerror/NonExistentClass;)V", "onFoundDual", "onFoundSPP", "bluetoothSPP", "onRecord", "bwt901ble", "parseAcceleration", "", "raw", "", "(Ljava/lang/String;)Ljava/lang/Float;", "startDiscovery", "stopDiscovery", "Companion", "ConnectionState", "app_debug"})
public final class BluetoothAccelerometerService implements com.accelerometer.app.sdk.witmotion.interfaces.IBwt901bleRecordObserver, IBluetoothFoundObserver {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "BluetoothAccelerometer";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> DEVICE_NAME_FILTER = null;
    private static final int HISTORY_LIMIT = 2000;
    private static final float GRAVITY = 9.80665F;
    @org.jetbrains.annotations.Nullable()
    private WitBluetoothManager bluetoothManager;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.accelerometer.app.sdk.witmotion.Bwt901ble> devices = null;
    @org.jetbrains.annotations.Nullable()
    private com.accelerometer.app.sdk.witmotion.Bwt901ble connectedDevice;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState> _connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState> connectionState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.accelerometer.app.data.AccelerometerData> _accelerometerData = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.data.AccelerometerData> accelerometerData = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.accelerometer.app.data.AccelerometerData>> _dataHistory = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.accelerometer.app.data.AccelerometerData>> dataHistory = null;
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
    public final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.data.AccelerometerData> getAccelerometerData() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.accelerometer.app.data.AccelerometerData>> getDataHistory() {
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
    
    public final void clearHistory() {
    }
    
    public void onFoundBle(@org.jetbrains.annotations.NotNull()
    BluetoothBLE bluetoothBLE) {
    }
    
    public void onFoundSPP(@org.jetbrains.annotations.Nullable()
    BluetoothSPP bluetoothSPP) {
    }
    
    public void onFoundDual(@org.jetbrains.annotations.Nullable()
    BluetoothBLE bluetoothBLE) {
    }
    
    @java.lang.Override()
    public void onRecord(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.sdk.witmotion.Bwt901ble bwt901ble) {
    }
    
    private final java.lang.Float parseAcceleration(java.lang.String raw) {
        return null;
    }
    
    private final void clearDevices() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$Companion;", "", "()V", "DEVICE_NAME_FILTER", "", "", "GRAVITY", "", "HISTORY_LIMIT", "", "TAG", "app_debug"})
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