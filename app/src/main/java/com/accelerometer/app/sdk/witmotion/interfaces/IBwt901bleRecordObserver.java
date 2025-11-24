package com.accelerometer.app.sdk.witmotion.interfaces;

import com.accelerometer.app.sdk.witmotion.Bwt901ble;

/**
 * Обработчик уведомлений о новых данных датчика.
 */
public interface IBwt901bleRecordObserver {
    void onRecord(Bwt901ble bwt901ble);
}


