package dev.codeforge

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectStore(private val privateDir: File) {
    private val root = File(privateDir, "projects")

    init {
        if (!root.exists()) root.mkdirs()
    }

    fun listProjects(): List<String> =
        root.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted().orEmpty()

    fun listFiles(projectName: String): List<String> {
        val projectDir = projectDirectory(projectName)
        return projectDir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(projectDir).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    fun save(projectName: String, fileName: String, content: String) {
        val projectDir = projectDirectory(projectName)
        if (!projectDir.exists() && !projectDir.mkdirs()) {
            error("Cannot create project directory")
        }
        val relativePath = safeRelativeFileName(fileName)
        val output = File(projectDir, relativePath).canonicalFile
        require(output.path.startsWith(projectDir.canonicalPath + File.separator)) {
            "File path escapes the project directory"
        }
        output.parentFile?.let { if (!it.exists() && !it.mkdirs()) error("Cannot create file directory") }
        output.writeText(content, Charsets.UTF_8)
    }

    fun read(projectName: String, fileName: String): String {
        val projectDir = projectDirectory(projectName).canonicalFile
        val file = File(projectDir, safeRelativeFileName(fileName)).canonicalFile
        require(file.path.startsWith(projectDir.path + File.separator) && file.isFile) {
            "File does not exist in this project"
        }
        return file.readText(Charsets.UTF_8)
    }

    fun loadFirstCodeFile(projectName: String): Pair<String, String>? {
        val fileName = listFiles(projectName)
            .firstOrNull { it.substringAfterLast('.', "").lowercase() in CODE_EXTENSIONS }
            ?: return null
        return fileName to read(projectName, fileName)
    }

    fun exportProject(projectName: String, output: OutputStream) {
        val safeName = safeSegment(projectName)
        val projectDir = projectDirectory(safeName)
        require(projectDir.isDirectory) { "Project does not exist" }
        ZipOutputStream(output.buffered()).use { zip ->
            projectDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val relative = file.relativeTo(projectDir).invariantSeparatorsPath
                zip.putNextEntry(ZipEntry("$safeName/$relative"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            zip.finish()
        }
    }

    fun importProject(input: InputStream): String {
        if (!root.exists() && !root.mkdirs()) error("Cannot create project workspace")
        var projectName: String? = null
        var importDirectory: File? = null
        var entryCount = 0
        var totalBytes = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                require(entryCount <= MAX_ARCHIVE_ENTRIES) { "Archive contains too many files" }

                val normalized = entry.name.replace('\\', '/')
                require(!normalized.startsWith("/") && !normalized.contains(":")) {
                    "Archive contains an absolute path"
                }
                val parts = normalized.split('/').filter { it.isNotEmpty() }
                require(parts.isNotEmpty() && parts.none { it == "." || it == ".." }) {
                    "Archive contains an unsafe path"
                }

                val archiveRoot = safeSegment(parts.first())
                if (projectName == null) {
                    projectName = archiveRoot
                    importDirectory = uniqueImportDirectory(archiveRoot)
                }
                require(projectName == archiveRoot) { "ZIP must contain exactly one project folder" }

                if (parts.size > 1) {
                    val destinationDir = requireNotNull(importDirectory)
                    val relative = parts.drop(1).joinToString(File.separator) { safeSegment(it) }
                    val target = File(destinationDir, relative).canonicalFile
                    require(target.path.startsWith(destinationDir.canonicalPath + File.separator) ||
                        target == destinationDir.canonicalFile) { "Archive path escapes project folder" }

                    if (entry.isDirectory || normalized.endsWith("/")) {
                        if (!target.exists() && !target.mkdirs()) error("Cannot create imported directory")
                    } else {
                        target.parentFile?.let {
                            if (!it.exists() && !it.mkdirs()) error("Cannot create imported folder")
                        }
                        target.outputStream().buffered().use { out ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                require(totalBytes <= MAX_ARCHIVE_BYTES) { "Archive expands beyond 50 MB" }
                                out.write(buffer, 0, count)
                            }
                        }
                    }
                }
                zip.closeEntry()
            }
        }

        require(!projectName.isNullOrBlank()) { "ZIP archive is empty" }
        return requireNotNull(importDirectory).name
    }

    private fun uniqueImportDirectory(projectName: String): File {
        val base = File(root, projectName)
        if (!base.exists()) return base
        var suffix = 1
        while (File(root, "${projectName}_imported$suffix").exists()) suffix += 1
        return File(root, "${projectName}_imported$suffix")
    }

    private fun projectDirectory(projectName: String): File = File(root, safeSegment(projectName))

    private fun safeSegment(value: String): String {
        val safe = value.trim().replace(Regex("[^A-Za-z0-9_. -]"), "_").trim('.', ' ')
        require(safe.isNotBlank() && safe != "." && safe != "..") { "Invalid path segment" }
        return safe.take(64)
    }

    private fun safeRelativeFileName(value: String): String {
        val normalized = value.trim().replace('\\', '/')
        val parts = normalized.split('/')
        require(parts.isNotEmpty() && parts.all {
            it.isNotBlank() && it != "." && it != ".." && !it.contains(":") && it.length <= 100
        }) { "Invalid relative file path" }
        return parts.joinToString(File.separator) { safeSegment(it) }
    }

    companion object {
        private const val MAX_ARCHIVE_ENTRIES = 1000
        private const val MAX_ARCHIVE_BYTES = 50L * 1024L * 1024L
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
            "Python" -> """
                print('Hello from CodeForge!')
                answer = 6 * 7
                print(answer)
            """.trimIndent()
            "Java" -> """
                int answer = 6 * 7;
                System.out.println("Hello from CodeForge!");
                System.out.println(answer);
            """.trimIndent()
            "C++" -> """
                #include <iostream>

                int main() {
                    std::cout << "Hello from CodeForge!\\n";
                    return 0;
                }
            """.trimIndent()
            else -> """
                console.log('Hello from CodeForge!');
                console.log(6 * 7);
            """.trimIndent()
        }

        fun runnerLabel(language: String): String = when (language) {
            "Python" -> "CPython 3.11"
            "Java" -> "Termux javac (or BeanShell fallback)"
            "JavaScript" -> "Rhino JavaScript"
            else -> "Termux clang++"
        }

        fun capabilityHint(language: String): String = when (language) {
            "Python" -> "Python 3.11 is embedded and runs locally."
            "Java" -> "Java compiles with Termux javac when configured; BeanShell is used otherwise."
            "JavaScript" -> "JavaScript runs locally using the embedded Rhino engine."
            else -> "C++ compiles and runs locally through Termux clang++ when configured."
        }
    }
}
