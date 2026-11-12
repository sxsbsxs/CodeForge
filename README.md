# CodeForge

CodeForge is a local-first Android coding workspace built with Kotlin and Jetpack Compose. Source files and project archives stay on the device; the app does not submit user code to a hosted compiler.

## Language support

| Language | Runtime/compiler | Current status |
|---|---|---|
| Python | Embedded CPython 3.11 via Chaquopy | Local script execution |
| JavaScript | Embedded Rhino ES6 interpreter | Local script execution |
| Java | Termux OpenJDK / javac, BeanShell fallback | Full Java compile/run when Termux is configured; interpreter fallback otherwise |
| C++ | Termux clang++ | C++17 compile/run when Termux is configured |

C++ and full Java compilation are optional and use Termux locally. The app has not yet been device-tested against every Termux distribution/version; see setup below.

## Features

- Black-and-white Jetpack Compose workspace.
- Four language modes and starter snippets.
- Lightweight monochrome syntax highlighting for keywords, comments, strings and numbers.
- Local Python and JavaScript execution.
- Java compile/run via Termux OpenJDK, with BeanShell fallback.
- C++17 compile/run via Termux clang++.
- Save and reopen projects in app-private storage.
- Multi-file project paths and file switching.
- ZIP project import/export through Android's Storage Access Framework.
- ZIP import path traversal checks and archive size/count limits.
- GitHub Actions debug APK artifact and tag-based Release publishing.

## Optional Termux setup for Java and C++

1. Install Termux from a trusted distribution. Termux and its plugins should come from the same signing source.
2. Open Termux and run:

   ```sh
   pkg update
   pkg install clang openjdk-17
   ```

3. Enable external command requests by adding this line to `~/.termux/termux.properties`:

   ```properties
   allow-external-apps=true
   ```

   Restart Termux after changing the setting.
4. In Android Settings, open CodeForge → App info → Permissions → Additional permissions and grant **Run commands in Termux environment**.

The Termux RUN_COMMAND API also requires the `allow-external-apps=true` setting and supports returning command results to apps on Termux 0.109 or later. See the [official RUN_COMMAND documentation](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent).

C++ mode sends source to Termux standard input, runs `clang++ -std=c++17`, then executes the generated binary in Termux. Java mode runs `javac` and `java`. When the source contains statements rather than a complete `Main` class, CodeForge wraps it in a minimal Java entry point. Source sent through Android intents is capped at 300 KB; command output is subject to Android/Termux return-size limits. Interactive stdin and cancellation are not yet implemented.

## APK packaging status

The **Package APK** control currently displays the requirements for the selected language. It does not yet generate a standalone APK for the user's project. Running source code and building an Android application are separate operations.

The next APK-builder milestone is to generate per-language Android host projects and use a local JDK, Gradle, Android SDK/build-tools, and NDK/CMake where required. The current CodeForge APK is built by GitHub Actions; this does not mean user projects are built in the cloud. CodeForge will not silently upload a user's source code to a hosted compiler.

## Build CodeForge

Open the repository in Android Studio with JDK 17 and Android SDK 35. The project uses Gradle 8.11.1 and Android Gradle Plugin 8.9.1.

The GitHub Actions workflow downloads Gradle, installs required Android SDK packages, runs unit tests and builds the debug APK. A successful run uploads a `CodeForge-debug` artifact. Push a version tag such as `v0.3.0` to publish a Release automatically. Alternatively, use Actions → Android CI → Run workflow and fill in the optional `release_tag` input to create a versioned Release from the selected commit.

## Workspace and archives

Projects are stored in the app-private files directory under `projects/<project-name>/`. ZIP import/export uses Android's document picker; imported archives are restricted to one project root, at most 1,000 entries and 50 MiB uncompressed.

## License

CodeForge-authored source is licensed under MIT. Third-party libraries retain their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
