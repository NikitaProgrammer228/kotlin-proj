package com.accelerometer.app.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0012\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B?\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\u0002\u0010\rJ\t\u0010\u0018\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0005H\u00c6\u0003J\u000f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\nH\u00c6\u0003J\u000b\u0010\u001c\u001a\u0004\u0018\u00010\fH\u00c6\u0003JC\u0010\u001d\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u00072\b\b\u0002\u0010\t\u001a\u00020\n2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00c6\u0001J\u0013\u0010\u001e\u001a\u00020\u001f2\b\u0010 \u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010!\u001a\u00020\"H\u00d6\u0001J\t\u0010#\u001a\u00020$H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0013\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017\u00a8\u0006%"}, d2 = {"Lcom/accelerometer/app/data/MeasurementState;", "", "status", "Lcom/accelerometer/app/data/MeasurementStatus;", "elapsedSec", "", "processedSamples", "", "Lcom/accelerometer/app/data/ProcessedSample;", "metrics", "Lcom/accelerometer/app/data/MeasurementMetrics;", "result", "Lcom/accelerometer/app/data/MeasurementResult;", "(Lcom/accelerometer/app/data/MeasurementStatus;DLjava/util/List;Lcom/accelerometer/app/data/MeasurementMetrics;Lcom/accelerometer/app/data/MeasurementResult;)V", "getElapsedSec", "()D", "getMetrics", "()Lcom/accelerometer/app/data/MeasurementMetrics;", "getProcessedSamples", "()Ljava/util/List;", "getResult", "()Lcom/accelerometer/app/data/MeasurementResult;", "getStatus", "()Lcom/accelerometer/app/data/MeasurementStatus;", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
public final class MeasurementState {
    @org.jetbrains.annotations.NotNull()
    private final com.accelerometer.app.data.MeasurementStatus status = null;
    private final double elapsedSec = 0.0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.accelerometer.app.data.ProcessedSample> processedSamples = null;
    @org.jetbrains.annotations.NotNull()
    private final com.accelerometer.app.data.MeasurementMetrics metrics = null;
    @org.jetbrains.annotations.Nullable()
    private final com.accelerometer.app.data.MeasurementResult result = null;
    
    public MeasurementState(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.MeasurementStatus status, double elapsedSec, @org.jetbrains.annotations.NotNull()
    java.util.List<com.accelerometer.app.data.ProcessedSample> processedSamples, @org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.MeasurementMetrics metrics, @org.jetbrains.annotations.Nullable()
    com.accelerometer.app.data.MeasurementResult result) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.MeasurementStatus getStatus() {
        return null;
    }
    
    public final double getElapsedSec() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.accelerometer.app.data.ProcessedSample> getProcessedSamples() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.MeasurementMetrics getMetrics() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.accelerometer.app.data.MeasurementResult getResult() {
        return null;
    }
    
    public MeasurementState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.MeasurementStatus component1() {
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
    public final com.accelerometer.app.data.MeasurementMetrics component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.accelerometer.app.data.MeasurementResult component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.data.MeasurementState copy(@org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.MeasurementStatus status, double elapsedSec, @org.jetbrains.annotations.NotNull()
    java.util.List<com.accelerometer.app.data.ProcessedSample> processedSamples, @org.jetbrains.annotations.NotNull()
    com.accelerometer.app.data.MeasurementMetrics metrics, @org.jetbrains.annotations.Nullable()
    com.accelerometer.app.data.MeasurementResult result) {
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