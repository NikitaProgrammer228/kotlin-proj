package com.accelerometer.app.measurement;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0010\u0007\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0011X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\u001bX\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/accelerometer/app/measurement/MeasurementConfig;", "", "()V", "AMPLITUDE_THRESHOLD_MM", "", "AMPLITUDE_THRESHOLD_MM_COORD", "AMPLITUDE_THRESHOLD_MM_FREQ", "ANGLE_TO_MM_SCALE_X", "ANGLE_TO_MM_SCALE_Y", "AUTOSTART_THRESHOLD_MM", "AXIS_INVERT_X", "AXIS_INVERT_Y", "CHART_AXIS_RANGE_MM", "COORDINATION_SCALE", "COORDINATION_SCALE_TARGET", "COORDINATION_SCALE_TIME", "ENABLE_CALIBRATION", "", "ENABLE_DEBUG_LOGS", "EXPECTED_SAMPLE_RATE_HZ", "HIGH_PASS_ALPHA", "MEASUREMENT_DURATION_SEC", "MOTION_CALIBRATION_DURATION_SEC", "MOTION_POSITION_LIMIT_MM", "OSCILLATION_CORRECTION", "STABILIZATION_DURATION_SEC", "TARGET_GRAPH_SCALE", "", "app_debug"})
public final class MeasurementConfig {
    public static final double MEASUREMENT_DURATION_SEC = 10.0;
    public static final double CHART_AXIS_RANGE_MM = 50.0;
    public static final boolean ENABLE_DEBUG_LOGS = true;
    public static final double EXPECTED_SAMPLE_RATE_HZ = 50.0;
    public static final double MOTION_CALIBRATION_DURATION_SEC = 1.0;
    public static final boolean ENABLE_CALIBRATION = false;
    public static final double STABILIZATION_DURATION_SEC = 0.0;
    public static final double AXIS_INVERT_X = 1.0;
    public static final double AXIS_INVERT_Y = -1.0;
    public static final double MOTION_POSITION_LIMIT_MM = 40.0;
    public static final float TARGET_GRAPH_SCALE = 1.18F;
    public static final double HIGH_PASS_ALPHA = 0.86;
    public static final double ANGLE_TO_MM_SCALE_X = 4.3;
    public static final double ANGLE_TO_MM_SCALE_Y = 4.3;
    public static final double AMPLITUDE_THRESHOLD_MM_FREQ = 3.0;
    public static final double AMPLITUDE_THRESHOLD_MM_COORD = 1.5;
    public static final double AMPLITUDE_THRESHOLD_MM = 1.5;
    public static final double OSCILLATION_CORRECTION = 0.3;
    public static final double COORDINATION_SCALE_TIME = 0.6;
    public static final double COORDINATION_SCALE_TARGET = 0.6;
    public static final double COORDINATION_SCALE = 0.6;
    public static final double AUTOSTART_THRESHOLD_MM = 5.0;
    @org.jetbrains.annotations.NotNull()
    public static final com.accelerometer.app.measurement.MeasurementConfig INSTANCE = null;
    
    private MeasurementConfig() {
        super();
    }
}