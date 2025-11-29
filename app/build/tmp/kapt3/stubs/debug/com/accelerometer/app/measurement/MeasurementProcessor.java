package com.accelerometer.app.measurement;

import com.accelerometer.app.data.ProcessedSample;
import com.accelerometer.app.data.SensorSample;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\u0018\u00002\u00020\u0001B\u000f\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bJ\u0006\u0010\f\u001a\u00020\rR\u0012\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0007R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/accelerometer/app/measurement/MeasurementProcessor;", "", "motionProcessor", "Lcom/accelerometer/app/measurement/MicroSwingMotionProcessor;", "(Lcom/accelerometer/app/measurement/MicroSwingMotionProcessor;)V", "measurementStart", "", "Ljava/lang/Double;", "process", "Lcom/accelerometer/app/data/ProcessedSample;", "sample", "Lcom/accelerometer/app/data/SensorSample;", "reset", "", "app_debug"})
public final class MeasurementProcessor {
    @org.jetbrains.annotations.NotNull()
    private final com.accelerometer.app.measurement.MicroSwingMotionProcessor motionProcessor = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double measurementStart;
    
    public MeasurementProcessor(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.measurement.MicroSwingMotionProcessor motionProcessor) {
        super();
    }
    
    public final void reset() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.ProcessedSample process(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.SensorSample sample) {
        return null;
    }
    
    public MeasurementProcessor() {
        super();
    }
}