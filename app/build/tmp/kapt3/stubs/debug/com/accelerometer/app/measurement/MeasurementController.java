package com.accelerometer.app.measurement;

import com.accelerometer.app.data.MeasurementMetrics;
import com.accelerometer.app.data.MeasurementResult;
import com.accelerometer.app.data.MeasurementState;
import com.accelerometer.app.data.MeasurementStatus;
import com.accelerometer.app.data.ProcessedSample;
import com.accelerometer.app.data.SensorSample;
import com.accelerometer.app.utils.MeasurementMath;
import kotlinx.coroutines.flow.StateFlow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u0000 +2\u00020\u0001:\u0001+B#\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u001e\u001a\u00020\u00032\u0006\u0010\u001f\u001a\u00020\u0003H\u0002J\u0014\u0010 \u001a\u00020!2\n\b\u0002\u0010\"\u001a\u0004\u0018\u00010#H\u0002J\u000e\u0010$\u001a\u00020!2\u0006\u0010%\u001a\u00020&J\u0010\u0010\'\u001a\u00020!2\b\b\u0002\u0010(\u001a\u00020\u0003J\u0010\u0010)\u001a\u00020!2\b\b\u0002\u0010*\u001a\u00020\fR\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0003X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0012\u0010\r\u001a\u0004\u0018\u00010\u0003X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u000eR\u000e\u0010\u000f\u001a\u00020\u0003X\u0082D\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\t0\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u001dX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006,"}, d2 = {"Lcom/accelerometer/app/measurement/MeasurementController;", "", "correctionFactor", "", "coordinationScale", "amplitudeThresholdMm", "(DDD)V", "_state", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/accelerometer/app/data/MeasurementState;", "artifactThresholdMm", "isValid", "", "lastSampleTimestamp", "Ljava/lang/Double;", "maxGapSec", "processed", "", "Lcom/accelerometer/app/data/ProcessedSample;", "processor", "Lcom/accelerometer/app/measurement/MeasurementProcessor;", "state", "Lkotlinx/coroutines/flow/StateFlow;", "getState", "()Lkotlinx/coroutines/flow/StateFlow;", "status", "Lcom/accelerometer/app/data/MeasurementStatus;", "targetDurationSec", "validationMessage", "", "abs", "value", "finalizeMeasurement", "", "precomputed", "Lcom/accelerometer/app/data/MeasurementMetrics;", "onSample", "sample", "Lcom/accelerometer/app/data/SensorSample;", "startMeasurement", "durationSec", "stopMeasurement", "resetToIdle", "Companion", "app_debug"})
public final class MeasurementController {
    private final double correctionFactor = 0.0;
    private final double coordinationScale = 0.0;
    private final double amplitudeThresholdMm = 0.0;
    public static final double DEFAULT_DURATION_SEC = 10.0;
    @org.jetbrains.annotations.NotNull()
    private final com.accelerometer.app.measurement.MeasurementProcessor processor = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.accelerometer.app.data.ProcessedSample> processed = null;
    @org.jetbrains.annotations.NotNull()
    private com.accelerometer.app.data.MeasurementStatus status = com.accelerometer.app.data.MeasurementStatus.IDLE;
    private double targetDurationSec = 10.0;
    private boolean isValid = true;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String validationMessage;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double lastSampleTimestamp;
    private final double maxGapSec = 0.1;
    private final double artifactThresholdMm = 40.0;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.accelerometer.app.data.MeasurementState> _state = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.data.MeasurementState> state = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.accelerometer.app.measurement.MeasurementController.Companion Companion = null;
    
    public MeasurementController(double correctionFactor, double coordinationScale, double amplitudeThresholdMm) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.accelerometer.app.data.MeasurementState> getState() {
        return null;
    }
    
    public final void startMeasurement(double durationSec) {
    }
    
    public final void stopMeasurement(boolean resetToIdle) {
    }
    
    public final void onSample(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.SensorSample sample) {
    }
    
    private final double abs(double value) {
        return 0.0;
    }
    
    private final void finalizeMeasurement(com.accelerometer.app.data.MeasurementMetrics precomputed) {
    }
    
    public MeasurementController() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/accelerometer/app/measurement/MeasurementController$Companion;", "", "()V", "DEFAULT_DURATION_SEC", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}