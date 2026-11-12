# Roadmap

## 0.3.x — local editor and language execution
- [x] Android Compose shell with monochrome UI
- [x] Embedded CPython 3.11 runtime via Chaquopy
- [x] Rhino JavaScript interpreter
- [x] BeanShell Java-statement fallback
- [x] Optional Termux RUN_COMMAND bridge
- [x] Java compile/run through Termux OpenJDK when configured
- [x] C++17 compile/run through Termux clang++ when configured
- [x] Save and reopen projects in private local storage
- [x] Multi-file project paths and file switching
- [x] ZIP import/export through Storage Access Framework
- [x] ZIP path validation and archive expansion limits
- [x] GitHub Actions debug APK artifact and tag Release workflow
- [ ] Interactive stdin, process cancellation and execution resource limits
- [ ] Device-side validation across Termux versions and Android vendors
- [x] Basic monochrome syntax highlighting for all four modes
- [ ] Diagnostics and richer code completion

## 0.4.0 — local Android APK packaging
- [ ] Generate an Android host project from Python source (Chaquopy-based template)
- [ ] Generate a WebView-based Android host project from JavaScript source
- [ ] Generate Java Android project templates and DEX packaging
- [ ] Generate NDK/CMake Android project templates for C/C++
- [ ] Detect and verify local JDK, Gradle, Android SDK and NDK installations
- [ ] Build and sign generated APKs locally
- [ ] Export, install and share generated APKs

## Packaging constraints

Running a source file is not the same as building an Android application. APK generation needs an Android host project, Android SDK/build-tools and Gradle; native C/C++ projects additionally need compatible NDK/CMake tooling. CodeForge does not yet generate user APKs. It will not silently upload project source to a cloud compiler.
