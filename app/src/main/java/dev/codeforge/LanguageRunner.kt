package dev.codeforge

import com.chaquo.python.Python

object LanguageRunner {
    fun run(language: String, source: String): String {
        return try {
            when (language) {
                "Python" -> runPython(source)
                "Java" -> EmbeddedJavaRunner.run(source)
                "JavaScript" -> EmbeddedJavaScriptRunner.run(source)
                "C++" -> "C++ runner unavailable: this APK does not include an Android arm64 Clang compiler and sysroot. No cloud execution is performed."
                else -> "Unsupported language: $language"
            }
        } catch (t: Throwable) {
            "Execution error: ${t.message ?: t.javaClass.simpleName}"
        }
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
