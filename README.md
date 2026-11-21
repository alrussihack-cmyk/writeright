# WriteRight AI

A fresh Android-native rebuild based on the three supplied reference screens.

## Product scope
Three states of one screen (matching the reference screenshots):
1. Writing board: handwrite English; the AI watches and reads the board after you pause.
2. Results on the board: your own handwriting stays, a red oval circles the wrong letters and the corrected word appears above in green. A card at the bottom lists the mistakes.
3. Explanation sheet: one mistake at a time (wrong vs right word, letter tiles, a memory tip, speaker button).

The header menu and theme toggle are overlays/actions, not additional pages.

## How a correction is found
Strokes are grouped into lines and words, each line is read by ML Kit, and each word is checked against a built-in list of common misspellings (`Spelling.kt`). Add a line `wrong=right` there to teach it a new word. The explanation of which letters are wrong is generated from the difference between the two spellings.

## Fonts
Cairo (UI + Arabic) and Kalam (handwriting look), both SIL Open Font License, bundled in `res/font`.

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
