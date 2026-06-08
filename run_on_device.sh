#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "============================================="
echo "   Camera Trainer Physical Device Run Script"
echo "============================================="

# 1. Setup Environment
export JAVA_HOME="/home/kynguyen/.jdks/jdk-17.0.11+9"
export PATH="/home/kynguyen/android-sdk/emulator:/home/kynguyen/android-sdk/platform-tools:$PATH"

echo "Checking environment..."
echo "Java Home: $JAVA_HOME"
echo "Android SDK Tools in path."

# Get list of connected physical devices (excluding emulators and header/empty lines)
DEVICE_ID=$(adb devices | grep -v "emulator" | grep "device$" | head -n 1 | awk '{print $1}')

if [ -z "$DEVICE_ID" ]; then
    echo "Error: No physical Android device detected via 'adb devices'."
    echo "Please make sure your device is connected, has USB debugging enabled, and is authorized."
    exit 1
fi

echo "Targeting device: $DEVICE_ID"

# 2. Build APK
echo "Building debug APK..."
./gradlew assembleDebug

# 3. Install APK
echo "Installing APK to device $DEVICE_ID..."
adb -s "$DEVICE_ID" install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Launch MainActivity
echo "Launching Camera Trainer..."
adb -s "$DEVICE_ID" shell am start -n com.example.cameratrainer/com.example.cameratrainer.MainActivity

echo "============================================="
echo "   App is running! Enjoy training!"
echo "============================================="
