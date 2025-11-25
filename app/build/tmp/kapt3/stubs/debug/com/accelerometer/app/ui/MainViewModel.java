package com.accelerometer.app.ui;

import androidx.lifecycle.ViewModel;
import com.accelerometer.app.data.MeasurementState;
import com.accelerometer.app.data.SensorSample;
import com.accelerometer.app.measurement.MeasurementController;
import kotlinx.coroutines.flow.StateFlow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rJ\u0010\u0010\u000e\u001a\u00020\u000b2\b\b\u0002\u0010\u000f\u001a\u00020\u0010J\u0006\u0010\u0011\u001a\u00020\u000bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u0012"}, d2 = {"Lcom/accelerometer/app/ui/MainViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "measurementController", "Lcom/accelerometer/app/measurement/MeasurementController;", "measurementState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/accelerometer/app/data/MeasurementState;", "getMeasurementState", "()Lkotlinx/coroutines/flow/StateFlow;", "onSensorSample", "", "sample", "Lcom/accelerometer/app/data/SensorSample;", "startMeasurement", "durationSec", "", "stopMeasurement", "app_debug"})
public final class MainViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.accelerometer.app.measurement.MeasurementController measurementController = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.data.MeasurementState> measurementState = null;
    
    public MainViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.data.MeasurementState> getMeasurementState() {
        return null;
    }
    
    public final void startMeasurement(double durationSec) {
    }
    
    public final void stopMeasurement() {
    }
    
    public final void onSensorSample(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.SensorSample sample) {
    }
}