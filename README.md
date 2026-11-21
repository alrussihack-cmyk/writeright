# WriteRight AI

A fresh Android-native rebuild based on the three supplied reference screens.

## Product scope
Exactly three app pages:
1. Writing Board
2. Correction Results
3. Error Explanation

The header menu and theme toggle are overlays/actions, not additional pages.

## Stack
- Kotlin
- Android Native
- Jetpack Compose + Material 3
- ML Kit Digital Ink Recognition (English US, dynamic on-device model)
- Android Text-to-Speech

## Recognition
The English handwriting model is downloaded on demand. After its one-time download, handwriting recognition runs on-device. Network permission is used only for model download.

## Build
GitHub Actions runs `clean assembleDebug` and uploads `app-debug.apk`.

## Important testing note
A successful CI build verifies compilation and APK generation. Real-device handwriting recognition, stylus/touch behavior, and visual matching must still be checked on a physical Android device.
