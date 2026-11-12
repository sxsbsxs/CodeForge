# CodeForge

CodeForge is a local-first Android coding workspace built with Kotlin and Jetpack Compose. The app keeps source files in its private workspace and does not send user code to a hosted compiler.

## Language support

| Language | Local runtime/compiler | Current support |
|---|---|---|
| Python | Embedded CPython 3.11 via Chaquopy | Runs Python scripts inside CodeForge |
| JavaScript | Embedded Rhino ES6 interpreter | Runs JavaScript locally |
| Java | Termux OpenJDK / javac, BeanShell fallback | Compiles and runs Java source locally when Termux is configured; otherwise interprets Java-like statements |
| C++ | Termux clang++ | Compiles and runs C++ locally when Termux is configured |

## C++ and full Java setup (optional Termux toolchain)

CodeForge can use Termux as a local compiler provider. Source is sent through Termux's standard-input command interface and compiled/executed inside the Termux app sandbox. It is not uploaded to a cloud service.

1. Install a compatible Termux build.
2. Open Termux and run: pkg update, then pkg install clang openjdk-17.
3. Enable external command requests by adding allow-external-apps=true to ~/.termux/termux.properties. Restart Termux after changing the setting.
4. In Android Settings, open CodeForge → App info → Permissions → Additional permissions and grant “Run commands in Termux environment”.

Termux's external command interface requires the permission above and allow-external-apps=true. The result callback used by CodeForge requires Termux version 0.109 or later. See the official Termux RUN_COMMAND documentation: https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent.

C++ mode compiles a C++17 source file with clang++ then executes it locally. Java mode compiles Java statements inside a generated Main class, or accepts a complete Main class, using javac. Console input is not yet interactive. Source passed to the Termux interface is limited to 300 KB to stay below Android's Intent size limit.

## Features in 0.3.0

- Black-and-white Jetpack Compose coding workspace.
- Four language modes.
- Embedded Python and JavaScript execution.
- Java snippet interpreter fallback.
- Optional full Java and C++ compile/run through Termux.
- Save and reopen projects in app-private storage.
- GitHub Actions APK artifact and tag-based Release workflow.

## APK packaging status

The Package APK action currently reports the per-language build prerequisites; it does not yet generate a user's standalone APK. Running code and building a separate Android application are different operations. A true local APK builder still needs Android project templates plus a local JDK, Gradle, Android SDK platform/build-tools, and NDK/CMake for native projects. This is the next milestone. No source code is uploaded to GitHub Actions by the mobile app.

## Build CodeForge

Open the repository in Android Studio with JDK 17 and Android SDK 35. The project uses Gradle 8.11.1 and Android Gradle Plugin 8.9.1.

The GitHub Actions workflow downloads Gradle, installs required Android SDK packages and runs the assembleDebug task. A successful run uploads a CodeForge-debug APK artifact. Push a version tag such as v0.3.0 to publish a GitHub Release.

## Workspace storage

Projects are stored under the app's private files directory in projects/<project-name>/. They remain local to the app installation. Storage Access Framework import/export and a multi-file tree are planned.

## License

CodeForge-authored source is licensed under MIT. Third-party libraries retain their own licenses; see THIRD_PARTY_NOTICES.md.
