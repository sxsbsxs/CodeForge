# Roadmap

## 0.1.0 — foundation
- [x] Android Compose shell with monochrome UI
- [x] Embedded Python 3.11 runtime via Chaquopy
- [x] Local Python execution and captured stdout
- [x] GitHub Actions debug APK workflow
- [ ] Persist/open project files and editor tabs
- [ ] JavaScript runtime adapter (QuickJS or equivalent)
- [ ] Java compiler/runtime strategy for Android
- [ ] C/C++ Clang/NDK toolchain packaging

## APK packaging reality
Building an Android APK locally is not equivalent to running a source file. Each target needs Android SDK/Gradle, a compatible JDK, Android platform/build-tools, and (for native code) NDK/CMake. Shipping a full Android build toolchain inside an ordinary phone app is very large and has device/storage constraints. CodeForge will expose packaging only after the relevant local toolchain is installed and verified; it will never silently send user source code to a cloud compiler.

Python APK packaging will use a dedicated Android-compatible packaging path. JavaScript, Java and C++ need their own Android project templates and local build integration. The current app does not claim those builders are complete.
