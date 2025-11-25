package com.accelerometer.app.measurement;

import com.accelerometer.app.data.ProcessedSample;
import com.accelerometer.app.data.SensorSample;
import com.accelerometer.app.utils.MeasurementMath;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\b\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B#\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\b\u0010\u001b\u001a\u00020\u0018H\u0002J\u0006\u0010\u001c\u001a\u00020\fJ\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u001e2\u0006\u0010\u0019\u001a\u00020\u001aJ\u0006\u0010\u001f\u001a\u00020\u0018R\u000e\u0010\u0007\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u000f\u001a\u0004\u0018\u00010\u0003X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0010R\u0012\u0010\u0011\u001a\u0004\u0018\u00010\u0003X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0010R\u000e\u0010\u0005\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u0012\u001a\u0004\u0018\u00010\u0003X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0010R\u000e\u0010\u0004\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/accelerometer/app/measurement/MeasurementProcessor;", "", "calibrationDurationSec", "", "minDt", "maxDt", "(DDD)V", "biasAccumulatorX", "biasAccumulatorY", "biasX", "biasY", "calibrationFinished", "", "calibrationSamples", "", "calibrationStart", "Ljava/lang/Double;", "lastTimestamp", "measurementStart", "sx", "sy", "vx", "vy", "accumulateBias", "", "sample", "Lcom/accelerometer/app/data/SensorSample;", "finalizeCalibration", "isCalibrating", "process", "Lcom/accelerometer/app/data/ProcessedSample;", "reset", "app_debug"})
public final class MeasurementProcessor {
    private final double calibrationDurationSec = 0.0;
    private final double minDt = 0.0;
    private final double maxDt = 0.0;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double calibrationStart;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double measurementStart;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double lastTimestamp;
    private int calibrationSamples = 0;
    private double biasAccumulatorX = 0.0;
    private double biasAccumulatorY = 0.0;
    private double biasX = 0.0;
    private double biasY = 0.0;
    private double vx = 0.0;
    private double vy = 0.0;
    private double sx = 0.0;
    private double sy = 0.0;
    private boolean calibrationFinished = false;
    
    public MeasurementProcessor(double calibrationDurationSec, double minDt, double maxDt) {
        super();
    }
    
    public final void reset() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.accelerometer.app.data.ProcessedSample process(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.SensorSample sample) {
        return null;
    }
    
    public final boolean isCalibrating() {
        return false;
    }
    
    private final void accumulateBias(com.accelerometer.app.data.SensorSample sample) {
    }
    
    private final void finalizeCalibration() {
    }
    
    public MeasurementProcessor() {
        super();
    }
}