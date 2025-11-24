#!/bin/bash

# Скрипт для просмотра логов приложения

echo "📋 Просмотр логов приложения..."
echo "Нажмите Ctrl+C для выхода"
echo ""

adb logcat -c
adb logcat -s BluetoothAccelerometer:* AndroidRuntime:E AccelerometerApp:* | grep -E "(BluetoothAccelerometer|AndroidRuntime|AccelerometerApp)"

