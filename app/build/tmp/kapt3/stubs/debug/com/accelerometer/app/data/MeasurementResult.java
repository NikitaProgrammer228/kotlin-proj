package com.accelerometer.app.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B#\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\u0002\u0010\tJ\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0005H\u00c6\u0003J\u000f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u00c6\u0003J-\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0018H\u00d6\u0001J\t\u0010\u0019\u001a\u00020\u001aH\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001b"}, d2 = {"Lcom/accelerometer/app/data/MeasurementResult;", "", "metrics", "Lcom/accelerometer/app/data/MeasurementMetrics;", "durationSec", "", "samples", "", "Lcom/accelerometer/app/data/ProcessedSample;", "(Lcom/accelerometer/app/data/MeasurementMetrics;DLjava/util/List;)V", "getDurationSec", "()D", "getMetrics", "()Lcom/accelerometer/app/data/MeasurementMetrics;", "getSamples", "()Ljava/util/List;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
public final class MeasurementResult {
    @org.jetbrains.annotations.NotNull()
    private final com.accelerometer.app.data.MeasurementMetrics metrics = null;
    private final double durationSec = 0.0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.accelerometer.app.data.ProcessedSample> samples = null;
    
    public MeasurementResult(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.MeasurementMetrics metrics, double durationSec, @org.jetbrains.annotations.NotNull()
    java.util.List<com.accelerometer.app.data.ProcessedSample> samples) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.MeasurementMetrics getMetrics() {
        return null;
    }
    
    public final double getDurationSec() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.accelerometer.app.data.ProcessedSample> getSamples() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.MeasurementMetrics component1() {
        return null;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.accelerometer.app.data.ProcessedSample> component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.MeasurementResult copy(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.MeasurementMetrics metrics, double durationSec, @org.jetbrains.annotations.NotNull()
    java.util.List<com.accelerometer.app.data.ProcessedSample> samples) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}