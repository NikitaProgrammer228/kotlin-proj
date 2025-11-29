package com.accelerometer.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import com.accelerometer.app.R;
import com.accelerometer.app.bluetooth.BluetoothAccelerometerService;
import com.accelerometer.app.measurement.MeasurementConfig;
import com.accelerometer.app.data.MeasurementState;
import com.accelerometer.app.data.MeasurementStatus;
import com.accelerometer.app.databinding.ActivityMainBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000r\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0015\u001a\u00020\u0016H\u0002J\b\u0010\u0017\u001a\u00020\u0016H\u0002J\b\u0010\u0018\u001a\u00020\u0016H\u0002J\u0012\u0010\u0019\u001a\u00020\u00162\b\u0010\u001a\u001a\u0004\u0018\u00010\u001bH\u0014J\b\u0010\u001c\u001a\u00020\u0016H\u0014J\u0010\u0010\u001d\u001a\u00020\u00162\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\b\u0010 \u001a\u00020\u0016H\u0002J\b\u0010!\u001a\u00020\u0016H\u0002J\b\u0010\"\u001a\u00020\u0016H\u0002J\u0010\u0010#\u001a\u00020\u00162\u0006\u0010$\u001a\u00020%H\u0002J\b\u0010&\u001a\u00020\u0016H\u0002J\u0010\u0010\'\u001a\u00020\u00162\u0006\u0010\u001e\u001a\u00020(H\u0002J&\u0010)\u001a\u00020\u00162\u0006\u0010$\u001a\u00020%2\f\u0010*\u001a\b\u0012\u0004\u0012\u00020,0+2\u0006\u0010-\u001a\u00020.H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082D\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000f\u001a\u00020\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006/"}, d2 = {"Lcom/accelerometer/app/ui/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/accelerometer/app/databinding/ActivityMainBinding;", "bluetoothService", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService;", "chartRangeMm", "", "measurementDurationSec", "", "requestPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "", "viewModel", "Lcom/accelerometer/app/ui/MainViewModel;", "getViewModel", "()Lcom/accelerometer/app/ui/MainViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "checkPermissionsAndConnect", "", "clearCharts", "disconnect", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "renderMeasurementState", "state", "Lcom/accelerometer/app/data/MeasurementState;", "setupCharts", "setupClickListeners", "setupObservers", "setupTimeChart", "chart", "Lcom/github/mikephil/charting/charts/LineChart;", "showDeviceSelectionDialog", "updateConnectionState", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$ConnectionState;", "updateTimeChart", "entries", "", "Lcom/github/mikephil/charting/data/Entry;", "color", "", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.accelerometer.app.databinding.ActivityMainBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.accelerometer.app.bluetooth.BluetoothAccelerometerService bluetoothService;
    private final double measurementDurationSec = 10.0;
    private final float chartRangeMm = 30.0F;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String[]> requestPermissionLauncher = null;
    
    public MainActivity() {
        super();
    }
    
    private final com.accelerometer.app.ui.MainViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupCharts() {
    }
    
    private final void setupTimeChart(com.github.mikephil.charting.charts.LineChart chart) {
    }
    
    private final void setupObservers() {
    }
    
    private final void setupClickListeners() {
    }
    
    private final void checkPermissionsAndConnect() {
    }
    
    private final void showDeviceSelectionDialog() {
    }
    
    private final void disconnect() {
    }
    
    private final void updateConnectionState(com.accelerometer.app.bluetooth.BluetoothAccelerometerService.ConnectionState state) {
    }
    
    private final void renderMeasurementState(com.accelerometer.app.data.MeasurementState state) {
    }
    
    private final void updateTimeChart(com.github.mikephil.charting.charts.LineChart chart, java.util.List<? extends com.github.mikephil.charting.data.Entry> entries, int color) {
    }
    
    private final void clearCharts() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}