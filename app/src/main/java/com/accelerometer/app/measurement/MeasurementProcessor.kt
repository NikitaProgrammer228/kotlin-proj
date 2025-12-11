package com.accelerometer.app.measurement

import com.accelerometer.app.data.ProcessedSample
import com.accelerometer.app.data.SensorSample

class MeasurementProcessor(
    private val motionProcessor: MicroSwingMotionProcessor = MicroSwingMotionProcessor(
        expectedSampleRateHz = MeasurementConfig.EXPECTED_SAMPLE_RATE_HZ
    )
) {

    // Время начала ПОСЛЕ калибровки (не с первого сэмпла)
    private var measurementStartAfterCalibration: Double? = null

    fun reset() {
        measurementStartAfterCalibration = null
        motionProcessor.reset()
    }
    
    fun isCalibrated(): Boolean = motionProcessor.isCalibrated()

    fun process(sample: SensorSample): ProcessedSample? {
        val timestamp = sample.timestampSec

        // Передаём ускорение напрямую в g (SDK уже возвращает значения в g)
        val state = motionProcessor.processSample(
            axG = sample.accXg,
            ayG = sample.accYg,
            azG = sample.accZg,
            angleXDeg = sample.angleXDeg,
            angleYDeg = sample.angleYDeg,
            timestampSec = timestamp
        )
        
        // Если калибровка выключена — рисуем сразу; иначе ждём её завершения
        if (MeasurementConfig.ENABLE_CALIBRATION) {
        if (!motionProcessor.isCalibrated()) {
            return null
            }
        } else {
            // если калибровка отключена, но стабилизация (если задана) ещё идёт,
            // то motionProcessor.isCalibrated() может быть false; в этом случае всё равно рисуем
        }

        // Запоминаем время первого сэмпла ПОСЛЕ калибровки
        if (measurementStartAfterCalibration == null) {
            measurementStartAfterCalibration = timestamp
        }

        val elapsed = timestamp - (measurementStartAfterCalibration ?: timestamp)

        return ProcessedSample(
            t = elapsed,
            axMm = state.axMm,
            ayMm = state.ayMm,
            vxMm = state.vxMm,
            vyMm = state.vyMm,
            sxMm = state.sxMm,
            syMm = state.syMm,
            sxMmRaw = state.sxMmRaw,
            syMmRaw = state.syMmRaw,
            hasArtifact = state.hasArtifact
        )
    }

}

