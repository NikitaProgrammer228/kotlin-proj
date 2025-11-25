package com.accelerometer.app.utils;

import com.accelerometer.app.data.MeasurementMetrics;
import com.accelerometer.app.data.ProcessedSample;

/**
 * Набор формул из разделов 8.1 и 8.3 руководства MicroSwing.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\b\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0006\u001a\u00020\u00042\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\bH\u0002J4\u0010\t\u001a\u00020\n2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\b2\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u0004J$\u0010\u0011\u001a\u00020\u00042\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\b2\u0006\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u0004J,\u0010\u0012\u001a\u00020\u00042\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\b2\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u0004J\u0014\u0010\u0013\u001a\u00020\u00042\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\bJ$\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00040\b2\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00040\b2\u0006\u0010\u0016\u001a\u00020\u0004H\u0002J\u000e\u0010\u0017\u001a\u00020\u00042\u0006\u0010\u0018\u001a\u00020\u0019R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lcom/accelerometer/app/utils/MeasurementMath;", "", "()V", "GRAVITY_MM", "", "RAW_SCALE", "amplitudeIrregularity", "amplitudes", "", "buildMetrics", "Lcom/accelerometer/app/data/MeasurementMetrics;", "samples", "Lcom/accelerometer/app/data/ProcessedSample;", "durationSec", "amplitudeThresholdMm", "correctionFactor", "scalingCoefficient", "calcCoordinationFactor", "calcOscillationFrequency", "calcStability", "extractAmplitudes", "values", "thresholdMm", "rawToAccMm", "raw", "", "app_debug"})
public final class MeasurementMath {
    private static final double RAW_SCALE = 16384.0;
    private static final double GRAVITY_MM = 9806.65;
    @org.jetbrains.annotations.NotNull()
    public static final com.accelerometer.app.utils.MeasurementMath INSTANCE = null;
    
    private MeasurementMath() {
        super();
    }
    
    public final double rawToAccMm(int raw) {
        return 0.0;
    }
    
    public final double calcStability(@org.jetbrains.annotations.NotNull()
    java.util.List<com.accelerometer.app.data.ProcessedSample> samples) {
        return 0.0;
    }
    
    public final double calcOscillationFrequency(@org.jetbrains.annotations.NotNull()
    java.util.List<com.accelerometer.app.data.ProcessedSample> samples, double durationSec, double amplitudeThresholdMm, double correctionFactor) {
        return 0.0;
    }
    
    public final double calcCoordinationFactor(@org.jetbrains.annotations.NotNull()
    java.util.List<com.accelerometer.app.data.ProcessedSample> samples, double amplitudeThresholdMm, double scalingCoefficient) {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.MeasurementMetrics buildMetrics(@org.jetbrains.annotations.NotNull()
    java.util.List<com.accelerometer.app.data.ProcessedSample> samples, double durationSec, double amplitudeThresholdMm, double correctionFactor, double scalingCoefficient) {
        return null;
    }
    
    private final double amplitudeIrregularity(java.util.List<java.lang.Double> amplitudes) {
        return 0.0;
    }
    
    private final java.util.List<java.lang.Double> extractAmplitudes(java.util.List<java.lang.Double> values, double thresholdMm) {
        return null;
    }
}