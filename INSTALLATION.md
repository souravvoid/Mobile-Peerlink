# Setup and Installation

## Requirements
- Android Studio Iguana (or newer)
- Java JDK 17
- Android SDK 35

## Build Instructions
1. Clone the repository.
2. Open the project in Android Studio.
3. Gradle will automatically sync configurations.
4. Run testing verification: `./gradlew :app:testDebugUnitTest`
5. Compile APK: `./gradlew :app:assembleDebug`

## Local Development
- Connect a physical Android device (Recommended) due to Wi-Fi Direct and mDNS requirements, or use an emulator for basic UI testing.
