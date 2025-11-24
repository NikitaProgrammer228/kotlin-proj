package com.accelerometer.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.viewModels;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.accelerometer.app.R;
import com.accelerometer.app.bluetooth.BluetoothAccelerometerService;
import com.accelerometer.app.databinding.ActivityMainBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0013\u001a\u00020\u0014H\u0002J\b\u0010\u0015\u001a\u00020\u0014H\u0002J\b\u0010\u0016\u001a\u00020\u0014H\u0002J\u0012\u0010\u0017\u001a\u00020\u00142\b\u0010\u0018\u001a\u0004\u0018\u00010\u0019H\u0014J\b\u0010\u001a\u001a\u00020\u0014H\u0014J \u0010\u001b\u001a\u00020\u00142\u0006\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\f2\u0006\u0010\u001f\u001a\u00020 H\u0002J\b\u0010!\u001a\u00020\u0014H\u0002J\b\u0010\"\u001a\u00020\u0014H\u0002J\b\u0010#\u001a\u00020\u0014H\u0002J\b\u0010$\u001a\u00020\u0014H\u0002J(\u0010%\u001a\u00020\u00142\u0006\u0010\u001c\u001a\u00020\u001d2\u0006\u0010&\u001a\u00020\'2\u0006\u0010(\u001a\u00020\'2\u0006\u0010\u001f\u001a\u00020 H\u0002J\u0010\u0010)\u001a\u00020\u00142\u0006\u0010*\u001a\u00020+H\u0002J\u0010\u0010,\u001a\u00020\u00142\u0006\u0010-\u001a\u00020.H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\r\u001a\u00020\u000e8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\u0012\u001a\u0004\b\u000f\u0010\u0010\u00a8\u0006/"}, d2 = {"Lcom/accelerometer/app/ui/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/accelerometer/app/databinding/ActivityMainBinding;", "bluetoothService", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService;", "measurementStartTime", "", "requestPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "", "viewModel", "Lcom/accelerometer/app/ui/MainViewModel;", "getViewModel", "()Lcom/accelerometer/app/ui/MainViewModel;", "viewModel$delegate", "Lerror/NonExistentClass;", "checkPermissionsAndConnect", "", "clearCharts", "disconnect", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "setupChart", "chart", "Lcom/github/mikephil/charting/charts/LineChart;", "label", "color", "", "setupCharts", "setupClickListeners", "setupObservers", "showDeviceSelectionDialog", "updateChart", "x", "", "y", "updateCharts", "data", "Lcom/accelerometer/app/data/AccelerometerData;", "updateConnectionState", "state", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$ConnectionState;", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.accelerometer.app.databinding.ActivityMainBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final com.accelerometer.app.ui.MainViewModel viewModel$delegate = null;
    private com.accelerometer.app.bluetooth.BluetoothAccelerometerService bluetoothService;
    private long measurementStartTime = 0L;
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
    
    private final void setupChart(com.github.mikephil.charting.charts.LineChart chart, java.lang.String label, int color) {
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
    
    private final void updateCharts(com.accelerometer.app.data.AccelerometerData data) {
    }
    
    private final void updateChart(com.github.mikephil.charting.charts.LineChart chart, float x, float y, int color) {
    }
    
    private final void clearCharts() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}