#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "============================================="
echo "   Camera Trainer Simulator Run Script"
echo "============================================="

# 1. Setup Environment
export JAVA_HOME="/home/kynguyen/.jdks/jdk-17.0.11+9"
export PATH="/home/kynguyen/android-sdk/emulator:/home/kynguyen/android-sdk/platform-tools:$PATH"

echo "Checking environment..."
echo "Java Home: $JAVA_HOME"
echo "Android SDK Tools in path."

# 2. Check and start emulator if not running
echo "Checking emulator status..."
DEVICE_ONLINE=$(adb devices | grep -E "emulator-[0-9]+" || true)

if [ -z "$DEVICE_ONLINE" ]; then
    echo "No running emulator detected. Starting 'test_avd'..."
    nohup emulator -avd test_avd > emulator.log 2>&1 &
    echo "Emulator started in the background. Waiting for device..."
fi

# Wait for ADB connection
adb wait-for-device

# Wait for Android system boot completion
echo "Waiting for Android system to boot..."
while [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
    sleep 2
done
echo "Android Simulator is ready."

# 3. Build APK
echo "Building debug APK..."
./gradlew assembleDebug

# 4. Install APK
echo "Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. Launch MainActivity
echo "Launching Camera Trainer..."
adb shell am start -n com.example.cameratrainer/com.example.cameratrainer.MainActivity

echo "============================================="
echo "   App is running! Enjoy training!"
echo "============================================="
