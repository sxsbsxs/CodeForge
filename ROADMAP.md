# Roadmap

## 0.3.0 — local language runtimes
- [x] Android Compose shell with monochrome UI
- [x] Embedded Python 3.11 runtime via Chaquopy
- [x] Rhino JavaScript interpreter
- [x] BeanShell Java-statement fallback
- [x] Save and reopen projects in private local storage
- [x] Optional Termux RUN_COMMAND bridge
- [x] Java compile/run through Termux OpenJDK when configured
- [x] C++ compile/run through Termux clang++ when configured
- [x] GitHub Actions debug APK artifact and tag Release workflow
- [ ] Interactive stdin and process cancellation
- [ ] Persistent multi-file project tree and tabs
- [ ] Import/export projects using Android Storage Access Framework

## 0.4.0 — Android APK packaging
- [ ] Generate an Android host project from a Python project (Chaquopy-based template)
- [ ] Generate a WebView-based Android host project from a JavaScript project
- [ ] Generate Java/Kotlin Android project templates and DEX packaging
- [ ] Generate NDK/CMake Android project templates for C/C++
- [ ] Use Termux JDK/Gradle/SDK for fully local on-device APK builds
- [ ] Sign, export, install and share generated APKs

## Important packaging constraint

Running a source file and building an Android APK are separate operations. A genuine local APK builder needs a local JDK, Gradle, Android SDK platform/build-tools and often NDK/CMake. CodeForge does not yet bundle those tools or generate user APKs. It will not silently upload user source code to a cloud compiler.
