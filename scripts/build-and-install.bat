@echo off
REM Скрипт для сборки и установки приложения на устройство (Windows)

echo 🔨 Сборка APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo ✅ Сборка успешна!
    echo 📱 Установка на устройство...
    call gradlew.bat installDebug
    
    if %ERRORLEVEL% EQU 0 (
        echo ✅ Установка успешна!
        echo 🚀 Запуск приложения...
        adb shell am start -n com.accelerometer.app/.MainActivity
        echo ✅ Приложение запущено!
    ) else (
        echo ❌ Ошибка установки
        exit /b 1
    )
) else (
    echo ❌ Ошибка сборки
    exit /b 1
)

