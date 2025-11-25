package com.accelerometer.app.measurement;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\b\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/accelerometer/app/measurement/MeasurementConfig;", "", "()V", "AMPLITUDE_THRESHOLD_MM", "", "CALIBRATION_DURATION_SEC", "COORDINATION_SCALE", "DT_SMOOTHING", "HIGH_PASS_CUTOFF_HZ", "LOW_PASS_CUTOFF_HZ", "MEASUREMENT_DURATION_SEC", "OSCILLATION_CORRECTION", "app_debug"})
public final class MeasurementConfig {
    public static final double MEASUREMENT_DURATION_SEC = 10.0;
    public static final double CALIBRATION_DURATION_SEC = 2.0;
    public static final double HIGH_PASS_CUTOFF_HZ = 0.2;
    public static final double LOW_PASS_CUTOFF_HZ = 5.0;
    public static final double DT_SMOOTHING = 0.05;
    public static final double AMPLITUDE_THRESHOLD_MM = 5.0;
    public static final double OSCILLATION_CORRECTION = 1.0;
    public static final double COORDINATION_SCALE = 1.0;
    @org.jetbrains.annotations.NotNull()
    public static final com.accelerometer.app.measurement.MeasurementConfig INSTANCE = null;
    
    private MeasurementConfig() {
        super();
    }
}