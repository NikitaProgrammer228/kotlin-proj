package com.accelerometer.app.measurement;

import android.util.Log;

/**
 * Процессор движения для измерения колебаний платформы.
 *
 * Вместо двойного интегрирования ускорения (что даёт огромный дрейф),
 * мы вычисляем угол наклона датчика и переводим его в "виртуальное смещение".
 *
 * Это соответствует тому, как работает MicroSwing — платформа качается,
 * и датчик измеряет угол отклонения от вертикали.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\f\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001:\u0001%B\u000f\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0018\u001a\u00020\bJ6\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u00032\u0006\u0010\u001c\u001a\u00020\u00032\u0006\u0010\u001d\u001a\u00020\u00032\u0006\u0010\u001e\u001a\u00020\u00032\u0006\u0010\u001f\u001a\u00020\u00032\u0006\u0010 \u001a\u00020\u0003J\u0006\u0010!\u001a\u00020\"J\u0016\u0010#\u001a\u00020\u0017*\u00020\u00032\b\b\u0002\u0010$\u001a\u00020\nH\u0002R\u000e\u0010\u0005\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u0011\u001a\u0004\u0018\u00010\u0003X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0012R\u000e\u0010\u0013\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u0015\u001a\u0004\u0018\u00010\u0003X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0012R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006&"}, d2 = {"Lcom/accelerometer/app/measurement/MicroSwingMotionProcessor;", "", "expectedSampleRateHz", "", "(D)V", "baseAngleXDeg", "baseAngleYDeg", "calibrated", "", "calibrationCount", "", "calibrationSumX", "calibrationSumY", "hpOutX", "hpOutY", "hpPrevX", "hpPrevY", "lastTimestampSec", "Ljava/lang/Double;", "prevSx", "prevSy", "startTimestampSec", "tag", "", "isCalibrated", "processSample", "Lcom/accelerometer/app/measurement/MicroSwingMotionProcessor$MotionState;", "axG", "ayG", "azG", "angleXDeg", "angleYDeg", "timestampSec", "reset", "", "format", "decimals", "MotionState", "app_debug"})
public final class MicroSwingMotionProcessor {
    private final double expectedSampleRateHz = 0.0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String tag = "MotionProcessor";
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double startTimestampSec;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double lastTimestampSec;
    private boolean calibrated = false;
    private double baseAngleXDeg = 0.0;
    private double baseAngleYDeg = 0.0;
    private double calibrationSumX = 0.0;
    private double calibrationSumY = 0.0;
    private int calibrationCount = 0;
    private double hpPrevX = 0.0;
    private double hpPrevY = 0.0;
    private double hpOutX = 0.0;
    private double hpOutY = 0.0;
    private double prevSx = 0.0;
    private double prevSy = 0.0;
    
    public MicroSwingMotionProcessor(double expectedSampleRateHz) {
        super();
    }
    
    public final void reset() {
    }
    
    public final boolean isCalibrated() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.accelerometer.app.measurement.MicroSwingMotionProcessor.MotionState processSample(double axG, double ayG, double azG, double angleXDeg, double angleYDeg, double timestampSec) {
        return null;
    }
    
    private final java.lang.String format(double $this$format, int decimals) {
        return null;
    }
    
    public MicroSwingMotionProcessor() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0015\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B5\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0003\u0012\u0006\u0010\u0007\u001a\u00020\u0003\u0012\u0006\u0010\b\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\tJ\t\u0010\u0011\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0003H\u00c6\u0003JE\u0010\u0017\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\u00032\b\b\u0002\u0010\b\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u0018\u001a\u00020\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001b\u001a\u00020\u001cH\u00d6\u0001J\t\u0010\u001d\u001a\u00020\u001eH\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000bR\u0011\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000bR\u0011\u0010\b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000bR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000bR\u0011\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000b\u00a8\u0006\u001f"}, d2 = {"Lcom/accelerometer/app/measurement/MicroSwingMotionProcessor$MotionState;", "", "axMm", "", "ayMm", "vxMm", "vyMm", "sxMm", "syMm", "(DDDDDD)V", "getAxMm", "()D", "getAyMm", "getSxMm", "getSyMm", "getVxMm", "getVyMm", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
    public static final class MotionState {
        private final double axMm = 0.0;
        private final double ayMm = 0.0;
        private final double vxMm = 0.0;
        private final double vyMm = 0.0;
        private final double sxMm = 0.0;
        private final double syMm = 0.0;
        
        public MotionState(double axMm, double ayMm, double vxMm, double vyMm, double sxMm, double syMm) {
            super();
        }
        
        public final double getAxMm() {
            return 0.0;
        }
        
        public final double getAyMm() {
            return 0.0;
        }
        
        public final double getVxMm() {
            return 0.0;
        }
        
        public final double getVyMm() {
            return 0.0;
        }
        
        public final double getSxMm() {
            return 0.0;
        }
        
        public final double getSyMm() {
            return 0.0;
        }
        
        public final double component1() {
            return 0.0;
        }
        
        public final double component2() {
            return 0.0;
        }
        
        public final double component3() {
            return 0.0;
        }
        
        public final double component4() {
            return 0.0;
        }
        
        public final double component5() {
            return 0.0;
        }
        
        public final double component6() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.accelerometer.app.measurement.MicroSwingMotionProcessor.MotionState copy(double axMm, double ayMm, double vxMm, double vyMm, double sxMm, double syMm) {
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
}