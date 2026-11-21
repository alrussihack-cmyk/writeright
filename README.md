# WriteRight AI — Android MVP

Android-native English writing coach, starting with English for Work at B1 level.

## Build an APK with GitHub Actions
Every push to `main` starts the **Build Android APK** workflow. You can also start it manually from the Actions tab using **Build Android APK → Run workflow**.

When the workflow succeeds:
1. Open the repository's **Actions** tab.
2. Open the latest **Build Android APK** run.
3. Find **Artifacts** at the bottom and download `WriteRightAI-debug-apk`.
4. Extract the ZIP; the APK is inside.

The generated file is a debug APK for testing, not a signed Play Store release.

## Tech stack
- Kotlin + Jetpack Compose + Material 3
- Android min SDK 26, compile/target SDK 36
- Feature-oriented structure, navigation foundation
- Planned: Room, ML Kit Digital Ink, stylus-only writing board, answer evaluation and progress tracking

## Local build
Open the project in Android Studio with JDK 17 and Android SDK 36, then run the `app` configuration or execute `gradle assembleDebug`.
