# Roadmap

## 0.2.0 — local editor and embedded interpreters
- [x] Android Compose shell with monochrome UI
- [x] Embedded Python 3.11 runtime via Chaquopy
- [x] Rhino JavaScript interpreter
- [x] BeanShell Java-statement interpreter
- [x] Save and reopen projects in private local storage
- [x] GitHub Actions debug APK artifact and tag Release workflow
- [ ] C/C++ Android-native compiler integration
- [ ] Persistent multi-file project tree and tabs
- [ ] Import/export projects using Android Storage Access Framework

## 0.3.0 — language toolchains
- [ ] Evaluate an Android-compatible Clang/LLVM distribution and its licensing/ABI constraints
- [ ] Add a true C/C++ compiler with compile diagnostics
- [ ] Decide on a Java source-to-DEX pipeline (compiler plus D8) rather than presenting interpretation as javac
- [ ] Add execution timeouts, cancellation and resource limits

## 0.4.0 — Android packaging
- [ ] Generate an Android host project from a Python project (Chaquopy-based template)
- [ ] Generate a WebView-based Android host project from a JavaScript project
- [ ] Generate Java/Kotlin Android project templates and DEX packaging
- [ ] Generate NDK/CMake Android project templates for C/C++
- [ ] Build/sign APKs locally after a verified on-device JDK/Gradle/SDK/NDK toolchain is installed

## Important packaging constraint

Running a source file and building an Android APK are separate operations. A genuine local APK builder needs a local JDK, Gradle, Android SDK platform/build-tools and often NDK/CMake. This app does not bundle that very large toolchain yet and does not claim that APK packaging works. CodeForge will not silently upload user source code to a cloud compiler.
