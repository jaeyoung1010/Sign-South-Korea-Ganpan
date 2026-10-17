# Sign Korean

Sign Korean is an Android app for learning Korean from real-world signs. Product and implementation documents live in [`docs`](docs).

## Local setup

Requirements:

- JDK 17
- Android SDK Platform 37.0 and Build Tools 36.0.0
- Node.js 24 or newer for repository quality scanners

Set `ANDROID_HOME` or create an ignored `local.properties`, then run:

```bash
./gradlew qualityGate
```

The gate builds the debug APK and runs formatting, detekt, Android Lint, JVM tests, dependency-version validation, and credential scanning. Instrumentation smoke tests run in CI and can be run locally with an API 26+ emulator:

```bash
./gradlew connectedDebugAndroidTest
```

Do not place TTS generation keys or other credentials in Android source, Gradle properties, resources, logs, or committed environment files.
