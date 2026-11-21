# WriteRight AI

Android-native English writing coach, starting with English for Work at B1 level.

## Phase 2: local curriculum database
- Room database with courses, lessons, vocabulary, exercises, attempts and learning progress.
- Offline seed curriculum for English for Work (B1).
- Three starter lessons: Workplace responsibilities, Shift handover, and Reporting a maintenance issue.
- Vocabulary and writing exercises stored locally.
- Attempt and progress persistence is available through CourseRepository.

## Build an APK
Every push to `main` starts **Build Android APK**. When the workflow succeeds, open the run in the Actions tab and download the `WriteRightAI-debug-apk` artifact.

The generated file is a debug APK for testing, not a signed Play Store release.

## Tech stack
- Kotlin + Jetpack Compose + Material 3
- Room 2.8.x + KSP
- Android min SDK 26, compile/target SDK 36
- Planned next: stylus-only writing surface, ML Kit Digital Ink recognition, answer evaluation and spaced review.
