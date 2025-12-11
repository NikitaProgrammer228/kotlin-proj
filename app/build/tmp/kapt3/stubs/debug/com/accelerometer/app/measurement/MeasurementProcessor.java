package com.accelerometer.app.measurement;

import com.accelerometer.app.data.ProcessedSample;
import com.accelerometer.app.data.SensorSample;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\u0018\u00002\u00020\u0001B\u000f\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\b\u001a\u00020\tJ\u0010\u0010\n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\f\u001a\u00020\rJ\u0006\u0010\u000e\u001a\u00020\u000fR\u0012\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0007R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/accelerometer/app/measurement/MeasurementProcessor;", "", "motionProcessor", "Lcom/accelerometer/app/measurement/MicroSwingMotionProcessor;", "(Lcom/accelerometer/app/measurement/MicroSwingMotionProcessor;)V", "measurementStartAfterCalibration", "", "Ljava/lang/Double;", "isCalibrated", "", "process", "Lcom/accelerometer/app/data/ProcessedSample;", "sample", "Lcom/accelerometer/app/data/SensorSample;", "reset", "", "app_debug"})
public final class MeasurementProcessor {
    @org.jetbrains.annotations.NotNull()
    private final com.accelerometer.app.measurement.MicroSwingMotionProcessor motionProcessor = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double measurementStartAfterCalibration;
    
    public MeasurementProcessor(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.measurement.MicroSwingMotionProcessor motionProcessor) {
        super();
    }
    
    public final void reset() {
    }
    
    public final boolean isCalibrated() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.accelerometer.app.data.ProcessedSample process(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.SensorSample sample) {
        return null;
    }
    
    public MeasurementProcessor() {
        super();
    }
}