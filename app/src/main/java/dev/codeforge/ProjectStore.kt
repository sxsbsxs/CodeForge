package dev.codeforge

import java.io.File

class ProjectStore(private val privateDir: File) {
    private val root = File(privateDir, "projects")

    init {
        if (!root.exists()) root.mkdirs()
    }

    fun listProjects(): List<String> =
        root.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted().orEmpty()

    fun save(projectName: String, fileName: String, content: String) {
        val safeProject = safeSegment(projectName)
        val safeFile = safeFileName(fileName)
        val projectDir = File(root, safeProject)
        if (!projectDir.exists() && !projectDir.mkdirs()) {
            error("Cannot create project directory")
        }
        File(projectDir, safeFile).writeText(content, Charsets.UTF_8)
    }

    fun loadFirstCodeFile(projectName: String): Pair<String, String>? {
        val projectDir = File(root, safeSegment(projectName))
        val file = projectDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in CODE_EXTENSIONS }
            ?.sortedBy { it.name }
            ?.firstOrNull() ?: return null
        return file.name to file.readText(Charsets.UTF_8)
    }

    private fun safeSegment(value: String): String {
        val safe = value.trim().replace(Regex("[^A-Za-z0-9_. -]"), "_").trim('.', ' ')
        require(safe.isNotBlank() && safe != "." && safe != "..") { "Invalid project name" }
        return safe.take(64)
    }

    private fun safeFileName(value: String): String {
        val safe = value.trim()
        require(safe.isNotBlank() && safe.length <= 100) { "Invalid file name" }
        require('/' !in safe && '\\' !in safe && safe != "." && safe != "..") {
            "File name must not contain a path"
        }
        return safe
    }

    companion object {
        private val CODE_EXTENSIONS = setOf("py", "java", "cpp", "cc", "c", "js")

        fun defaultFileName(language: String): String = when (language) {
            "Python" -> "main.py"
            "Java" -> "Main.java"
            "C++" -> "main.cpp"
            else -> "main.js"
        }

        fun languageForFile(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
            "py" -> "Python"
            "java" -> "Java"
            "cpp", "cc", "c" -> "C++"
            else -> "JavaScript"
        }

        fun starter(language: String): String = when (language) {
            "Python" -> "print('Hello from CodeForge!')\nanswer = 6 * 7\nprint(answer)\n"
            "Java" -> "int answer = 6 * 7;\nSystem.out.println(\"Hello from CodeForge!\");\nSystem.out.println(answer);\n"
            "C++" -> "#include <iostream>\nint main() {\n    std::cout << \"Hello from CodeForge!\\n\";\n    return 0;\n}\n"
            else -> "console.log('Hello from CodeForge!');\nconsole.log(6 * 7);\n"
        }

        fun runnerLabel(language: String): String = when (language) {
            "Python" -> "CPython 3.11"
            "Java" -> "BeanShell Java interpreter"
            "JavaScript" -> "Rhino JavaScript"
            else -> "Clang toolchain required"
        }

        fun capabilityHint(language: String): String = when (language) {
            "Python" -> "Python 3.11 is embedded and runs locally."
            "Java" -> "Java mode interprets Java-like statements. Full javac/Android DEX compilation is not included."
            "JavaScript" -> "JavaScript runs locally using the embedded Rhino engine."
            else -> "C++ is listed in the editor, but a native Android Clang toolchain is not bundled yet."
        }
    }
}
