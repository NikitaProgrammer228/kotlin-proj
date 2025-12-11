package com.accelerometer.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
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
import com.accelerometer.app.export.ExportService;
import com.accelerometer.app.export.PdfExportService;
import com.accelerometer.app.database.AppDatabase;
import com.accelerometer.app.service.MeasurementRepository;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u009a\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\u0018\u0000 E2\u00020\u0001:\u0001EB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u001d\u001a\u00020\u001eH\u0002J\b\u0010\u001f\u001a\u00020\u001eH\u0002J\b\u0010 \u001a\u00020\u001eH\u0002J\b\u0010!\u001a\u00020\u001eH\u0002J\u0010\u0010\"\u001a\u00020\u001e2\u0006\u0010#\u001a\u00020$H\u0002J\u0010\u0010%\u001a\u00020\u001e2\u0006\u0010#\u001a\u00020$H\u0002J\u0010\u0010&\u001a\u00020\u00162\u0006\u0010\'\u001a\u00020\u0016H\u0002J\u0012\u0010(\u001a\u00020\u001e2\b\u0010)\u001a\u0004\u0018\u00010*H\u0014J\b\u0010+\u001a\u00020\u001eH\u0014J\u0010\u0010,\u001a\u00020\u001e2\u0006\u0010-\u001a\u00020.H\u0002J\u0010\u0010/\u001a\u00020\u001e2\u0006\u00100\u001a\u000201H\u0002J\u0010\u00102\u001a\u00020\u001e2\u0006\u00100\u001a\u000201H\u0002J\b\u00103\u001a\u00020\u001eH\u0002J\b\u00104\u001a\u00020\u001eH\u0002J\b\u00105\u001a\u00020\u001eH\u0002J\b\u00106\u001a\u00020\u001eH\u0002J\u0010\u00107\u001a\u00020\u001e2\u0006\u00108\u001a\u000209H\u0002J\b\u0010:\u001a\u00020\u001eH\u0002J\u0018\u0010;\u001a\u00020\u001e2\u0006\u0010-\u001a\u00020.2\u0006\u0010<\u001a\u00020\u0016H\u0002J\u0010\u0010=\u001a\u00020\u001e2\u0006\u00100\u001a\u00020>H\u0002J&\u0010?\u001a\u00020\u001e2\u0006\u00108\u001a\u0002092\f\u0010@\u001a\b\u0012\u0004\u0012\u00020B0A2\u0006\u0010C\u001a\u00020DH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u000eX\u0082D\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00160\u00150\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0017\u001a\u00020\u00188BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001b\u0010\u001c\u001a\u0004\b\u0019\u0010\u001a\u00a8\u0006F"}, d2 = {"Lcom/accelerometer/app/ui/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "autoStartEnabled", "", "binding", "Lcom/accelerometer/app/databinding/ActivityMainBinding;", "bluetoothService", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService;", "chartRangeMm", "", "currentUserId", "", "lastAccelerationMagnitude", "", "measurementDurationSec", "measurementRepository", "Lcom/accelerometer/app/service/MeasurementRepository;", "motionThresholdG", "requestPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "", "viewModel", "Lcom/accelerometer/app/ui/MainViewModel;", "getViewModel", "()Lcom/accelerometer/app/ui/MainViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "checkPermissionsAndConnect", "", "clearCharts", "disconnect", "exportMeasurement", "exportToCsv", "result", "Lcom/accelerometer/app/data/MeasurementResult;", "exportToPdf", "getMimeType", "extension", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "openFile", "file", "Ljava/io/File;", "renderMeasurementState", "state", "Lcom/accelerometer/app/data/MeasurementState;", "saveMeasurementToDatabase", "setupCharts", "setupClickListeners", "setupDurationSpinner", "setupObservers", "setupTimeChart", "chart", "Lcom/github/mikephil/charting/charts/LineChart;", "showDeviceSelectionDialog", "showExportSuccess", "format", "updateConnectionState", "Lcom/accelerometer/app/bluetooth/BluetoothAccelerometerService$ConnectionState;", "updateTimeChart", "entries", "", "Lcom/github/mikephil/charting/data/Entry;", "color", "", "Companion", "app_debug"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.accelerometer.app.databinding.ActivityMainBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.accelerometer.app.bluetooth.BluetoothAccelerometerService bluetoothService;
    private double measurementDurationSec = 10.0;
    private final float chartRangeMm = 50.0F;
    private com.accelerometer.app.service.MeasurementRepository measurementRepository;
    private long currentUserId = 0L;
    private boolean autoStartEnabled = true;
    private final double motionThresholdG = 0.05;
    private double lastAccelerationMagnitude = 0.0;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.Double> TEST_DURATIONS = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String[]> requestPermissionLauncher = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.accelerometer.app.ui.MainActivity.Companion Companion = null;
    
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
    
    private final void setupDurationSpinner() {
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
    
    private final void exportMeasurement() {
    }
    
    private final void exportToCsv(com.accelerometer.app.data.MeasurementResult result) {
    }
    
    private final void exportToPdf(com.accelerometer.app.data.MeasurementResult result) {
    }
    
    private final void showExportSuccess(java.io.File file, java.lang.String format) {
    }
    
    private final void openFile(java.io.File file) {
    }
    
    private final java.lang.String getMimeType(java.lang.String extension) {
        return null;
    }
    
    private final void saveMeasurementToDatabase(com.accelerometer.app.data.MeasurementState state) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u0006\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/accelerometer/app/ui/MainActivity$Companion;", "", "()V", "TEST_DURATIONS", "", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}