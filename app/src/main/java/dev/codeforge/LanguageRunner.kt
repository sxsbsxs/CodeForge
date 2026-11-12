package dev.codeforge

import android.content.Context
import com.chaquo.python.Python

object LanguageRunner {
    suspend fun run(language: String, source: String, context: Context): String {
        return try {
            when (language) {
                "Python" -> runPython(source)
                "Java" -> TermuxRunner.runJava(context, source) ?: runJavaFallback(source)
                "JavaScript" -> EmbeddedJavaScriptRunner.run(source)
                "C++" -> TermuxRunner.runCpp(context, source)
                else -> "Unsupported language: $language"
            }
        } catch (t: Throwable) {
            "Execution error: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    private fun runJavaFallback(source: String): String {
        return EmbeddedJavaRunner.run(source) +
            "\n\n[Fallback] Termux OpenJDK is not configured. Install it to compile and run full Java source with javac."
    }

    private fun runPython(source: String): String {
        val py = Python.getInstance()
        val builtins = py.getModule("builtins")
        val io = py.getModule("io")
        val sys = py.getModule("sys")
        val stdout = io.callAttr("StringIO")
        val stderr = io.callAttr("StringIO")
        val previousOut = sys.get("stdout")
        val previousErr = sys.get("stderr")
        try {
            sys.put("stdout", stdout)
            sys.put("stderr", stderr)
            builtins.callAttr("exec", source)
            val outText = stdout.callAttr("getvalue").toString()
            val errText = stderr.callAttr("getvalue").toString()
            return (outText + errText).ifBlank { "Finished (no output)." }
        } catch (t: Throwable) {
            val errText = runCatching { stderr.callAttr("getvalue").toString() }.getOrDefault("")
            return errText + "Python error: ${t.message ?: t.javaClass.simpleName}"
        } finally {
            sys.put("stdout", previousOut)
            sys.put("stderr", previousErr)
        }
    }
}
