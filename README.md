# Camera Trainer App

An interactive training application designed to help photographers practice and improve their framing and composition skills. The app presents users with high-resolution photos representing the "real world" and challenges them to find the best composition using a mobile viewfinder. An AI-powered grading engine evaluates their crops based on classical composition guidelines.

---

## Key Features

1. **Framing Missions**: Users receive specific photographic assignments (e.g., align elements with the Horizon Line, practice Symmetry, or implement the Rule of Thirds).
2. **Interactive Viewfinder**: Supports touch gestures for panning and zooming the source image within a fixed 4:3 viewfinder.
3. **Guideline Overlays**: Toggles visual helper lines on the viewfinder:
   - Rule of Thirds
   - Golden Ratio
   - Symmetry (vertical/horizontal centers)
   - Horizon Line
4. **Composition Evaluator**: An algorithmic grading engine that calculates mathematical offsets, weights, and point-of-interest (POI) alignments to generate a score out of 100 with detailed technical feedback.

---

## Project Structure

```bash
camera-trainer/
├── app/                      # Kotlin/Jetpack Compose Android Code
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/cameratrainer/
│   │   │   ├── MainActivity.kt
│   │   │   ├── domain/      # Use cases, scoring math, domain models
│   │   │   └── presentation/# Compose UI Screens, ViewModels, Theme components
│   │   └── res/             # Default launcher icons and styles
│   └── build.gradle          # Module build configurations
├── web-simulator/            # Web-based interactive app simulator
│   ├── index.html
│   ├── index.css
│   └── app.js                # Core JS port of the scoring math and gestures
├── build.gradle              # Project-level Gradle build file
├── settings.gradle           # Gradle module inclusions
├── local.properties          # Local Android SDK path (Git ignored)
└── README.md
```

---

## Getting Started

### 1. Running the Android Application

#### Prerequisites
- **Java JDK 17**
- **Android SDK** (Compile SDK version 34, Min SDK version 27)

#### Setup Local Properties
Create a `local.properties` file in the root folder pointing to your Android SDK directory:
```properties
sdk.dir=/path/to/your/android-sdk
```

#### Compile the APK
Run the following Gradle command to build the debug APK:
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

#### Run on a Simulator / Device
With an active Android device or emulator connected via `adb`, install and launch the application:
```bash
# Install the APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch MainActivity
adb shell am start -n com.example.cameratrainer/com.example.cameratrainer.MainActivity
```

---

### 2. Running the Web Simulator

The project includes a lightweight HTML5/JavaScript web simulator replicating the exact Compose UI styling, pan/zoom interaction, and coordinate-mapping grading engine.

To run it locally:
1. Navigate to the `web-simulator` directory:
   ```bash
   cd web-simulator
   ```
2. Start a local server (e.g., using Python):
   ```bash
   python3 -m http.server 8085
   ```
3. Open your browser and navigate to:
   `http://localhost:8085`
