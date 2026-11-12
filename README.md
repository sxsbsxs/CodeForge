# CodeForge

CodeForge is a local-first Android coding workspace built with Kotlin and Jetpack Compose. Its goal is to let users edit and execute code on their Android device, then package supported projects as Android apps without sending source code to a remote compiler.

## Languages and current execution support

| Language | Engine in this version | Status |
|---|---|---|
| Python | Embedded CPython 3.11 via Chaquopy | Local script execution |
| JavaScript | Embedded Rhino ES6 interpreter | Local script execution |
| Java | BeanShell interpreter | Local Java-like statements; not a full javac toolchain |
| C++ | Android Clang/NDK integration | Not yet bundled; the UI reports this clearly |

Java mode uses BeanShell to interpret Java-style statements. It does not compile ordinary Java projects into DEX. C++ execution requires an Android-native Clang toolchain and sysroot; neither is currently shipped in the APK.

## Features in 0.2.0

- Monochrome Android UI.
- Python, Java, C++ and JavaScript editor modes.
- Local Python, JavaScript and Java-snippet execution.
- Save and reopen named projects in the app-private workspace.
- Explicit capability messages instead of sending source code to a server.
- GitHub Actions debug APK artifact and tag-based Release publishing.

## Build

Open the repository in Android Studio with Android SDK 35 and JDK 17 installed. The project uses Gradle 8.11.1 and Android Gradle Plugin 8.9.1.

The GitHub Actions workflow downloads Gradle, installs the required Android SDK packages, and runs the assembleDebug Gradle task.

The debug APK is attached to each successful Actions run as the CodeForge-debug artifact. Push a version tag such as v0.2.0 to publish it as a GitHub Release.

## APK packaging status

The Package APK action currently explains the prerequisites and limitations; it does not claim to produce an APK yet. A genuine local APK builder needs a local JDK, Gradle, Android SDK platform/build-tools and often NDK/CMake, plus a per-language Android host template. That build pipeline is the next major milestone. No user source code is uploaded to GitHub Actions or a hosted compiler by the app.

## Workspace storage

Projects are stored under the app's private files directory in projects/<project-name>/. They remain local to the app installation. Export/import through Android's Storage Access Framework is planned.

## License

The CodeForge project source is licensed under MIT. Third-party libraries retain their own licenses; see THIRD_PARTY_NOTICES.md.
