package dev.codeforge

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class TermuxCommandResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val internalError: String? = null
)

object TermuxResultRegistry {
    private val ids = AtomicInteger(2000)
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<TermuxCommandResult>>()

    fun register(): Pair<Int, CompletableDeferred<TermuxCommandResult>> {
        val id = ids.incrementAndGet()
        val deferred = CompletableDeferred<TermuxCommandResult>()
        pending[id] = deferred
        return id to deferred
    }

    fun complete(id: Int, result: TermuxCommandResult) {
        pending.remove(id)?.complete(result)
    }

    fun remove(id: Int) {
        pending.remove(id)?.cancel()
    }
}

object TermuxRunner {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RUN_SERVICE = "com.termux.app.RunCommandService"
    private const val PREFIX = "/data/data/com.termux/files/usr"
    private const val HOME = "/data/data/com.termux/files/home"
    private const val BASH = "$PREFIX/bin/bash"
    private const val MAX_SOURCE_BYTES = 300_000
    private const val COMMAND_TIMEOUT_MS = 120_000L

    fun setupMessage(context: Context): String? {
        val installed = try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        if (!installed) {
            return "Install Termux, then run: pkg update && pkg install clang openjdk-17"
        }
        if (ContextCompat.checkSelfPermission(
                context,
                "com.termux.permission.RUN_COMMAND"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return "Grant CodeForge the Android permission “Run commands in Termux environment” in App info → Permissions → Additional permissions."
        }
        return null
    }

    suspend fun runCpp(context: Context, source: String): String {
        val setup = setupMessage(context)
        if (setup != null) {
            return "C++ needs the optional local Termux toolchain. $setup\nAlso set allow-external-apps=true in Termux's ~/.termux/termux.properties, then restart Termux."
        }
        if (source.toByteArray(Charsets.UTF_8).size > MAX_SOURCE_BYTES) {
            return "Source is larger than 300 KB. Reduce the file size before sending it to the local Termux compiler."
        }

        val script = """
            set +e
            mkdir -p "$HOME/.codeforge"
            cd "$HOME/.codeforge" || exit 90
            cat > main.cpp || exit 91
            clang++ -std=c++17 -O0 -g0 main.cpp -o main
            compile_status=$?
            if [ "$compile_status" -ne 0 ]; then
                rm -f main.cpp main
                exit "$compile_status"
            fi
            ./main
            run_status=$?
            rm -f main.cpp main
            exit "$run_status"
        """.trimIndent()

        return try {
            val result = execute(context, BASH, arrayOf("-lc", script), source)
            formatResult(result, "C++")
        } catch (t: Throwable) {
            "C++ Termux error: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    suspend fun runJava(context: Context, source: String): String? {
        if (setupMessage(context) != null) return null
        if (source.toByteArray(Charsets.UTF_8).size > MAX_SOURCE_BYTES) {
            return "Source is larger than 300 KB. Reduce the file size before compiling."
        }

        val javaSource = toJavaSource(source)
        val script = """
            set +e
            command -v javac >/dev/null 2>&1 || {
                echo "javac is missing. In Termux run: pkg install openjdk-17" >&2
                exit 127
            }
            mkdir -p "$HOME/.codeforge"
            cd "$HOME/.codeforge" || exit 90
            cat > Main.java || exit 91
            javac Main.java
            compile_status=$?
            if [ "$compile_status" -ne 0 ]; then
                rm -f Main.java Main*.class
                exit "$compile_status"
            fi
            java Main
            run_status=$?
            rm -f Main.java Main*.class
            exit "$run_status"
        """.trimIndent()

        return try {
            val result = execute(context, BASH, arrayOf("-lc", script), javaSource)
            formatResult(result, "Java")
        } catch (t: Throwable) {
            "Java Termux error: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    private fun toJavaSource(source: String): String {
        if (Regex("""\b(class|interface|enum|record)\s+Main\b""").containsMatchIn(source)) {
            return source
        }
        val imports = source.lineSequence()
            .filter { it.trim().startsWith("import ") }
            .toList()
        val body = source.lineSequence()
            .filterNot { it.trim().startsWith("import ") }
            .joinToString("\n")
        return buildString {
            if (imports.isNotEmpty()) append(imports.joinToString("\n")).append("\n")
            append("public class Main {\n")
            append("  public static void main(String[] args) throws Exception {\n")
            append(body).append("\n")
            append("  }\n")
            append("}\n")
        }
    }

    private suspend fun execute(
        context: Context,
        executable: String,
        arguments: Array<String>,
        stdin: String?
    ): TermuxCommandResult {
        val (requestId, deferred) = TermuxResultRegistry.register()
        try {
            val callbackIntent = Intent(context, TermuxResultService::class.java)
                .putExtra(TermuxResultService.EXTRA_EXECUTION_ID, requestId)
            val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else 0
            val pendingIntent = PendingIntent.getService(
                context,
                requestId,
                callbackIntent,
                PendingIntent.FLAG_ONE_SHOT or mutableFlag
            )

            val commandIntent = Intent(RUN_COMMAND_ACTION).apply {
                setClassName(TERMUX_PACKAGE, RUN_SERVICE)
                putExtra("com.termux.RUN_COMMAND_PATH", executable)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arguments)
                putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)
                if (stdin != null) putExtra("com.termux.RUN_COMMAND_STDIN", stdin)
                putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "CodeForge local compile/run")
                putExtra(
                    "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION",
                    "Executes your source code locally in the Termux environment."
                )
            }
            context.startService(commandIntent)
            return withTimeout(COMMAND_TIMEOUT_MS) { deferred.await() }
        } finally {
            TermuxResultRegistry.remove(requestId)
        }
    }

    private fun formatResult(result: TermuxCommandResult, language: String): String {
        result.internalError?.let { return "Termux could not start the command: $it" }
        val sections = buildList {
            if (result.stdout.isNotBlank()) add(result.stdout.trimEnd())
            if (result.stderr.isNotBlank()) add(result.stderr.trimEnd())
        }
        val content = sections.joinToString("\n")
        return when {
            result.exitCode == 0 && content.isBlank() -> "$language finished successfully (no output)."
            result.exitCode == 0 -> content
            content.isBlank() -> "$language failed with exit code ${result.exitCode}."
            else -> "$content\n\nProcess exited with code ${result.exitCode}."
        }
    }
}
