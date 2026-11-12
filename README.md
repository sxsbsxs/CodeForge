# CodeForge

A local-first Android coding workspace for Python, Java, C++ and JavaScript.

> Early development: the editor shell and build pipeline are being established. Language execution and APK packaging are implemented incrementally; do not treat all four runtimes or on-device APK builders as complete yet.

## Goals
- Android app built with Kotlin and Jetpack Compose
- Black-and-white minimal interface
- Local project files and editor
- Local execution adapters for supported languages
- Per-language Android packaging templates and explicit capability checks
- GitHub Actions debug/release APK builds

## Build
Open this project in Android Studio or run `./gradlew assembleDebug` with Android SDK installed.

## Current status
See [ROADMAP.md](ROADMAP.md).