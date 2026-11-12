package dev.codeforge

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectStoreTest {
    @Test
    fun savesAndReadsNestedSourceFiles() {
        val sandbox = Files.createTempDirectory("codeforge-store-test").toFile()
        try {
            val store = ProjectStore(sandbox)
            store.save("Demo", "src/main.py", "print('local')")

            assertEquals("print('local')", store.read("Demo", "src/main.py"))
            assertEquals(listOf("src/main.py"), store.listFiles("Demo"))
        } finally {
            sandbox.deleteRecursively()
        }
    }

    @Test
    fun exportsAndImportsACompleteProjectArchive() {
        val sourceRoot = Files.createTempDirectory("codeforge-export-test").toFile()
        val targetRoot = Files.createTempDirectory("codeforge-import-test").toFile()
        try {
            val sourceStore = ProjectStore(sourceRoot)
            sourceStore.save("Demo", "src/main.py", "print('archive works')")
            sourceStore.save("Demo", "README.md", "# Demo")

            val archive = ByteArrayOutputStream()
            sourceStore.exportProject("Demo", archive)

            val targetStore = ProjectStore(targetRoot)
            val importedName = targetStore.importProject(ByteArrayInputStream(archive.toByteArray()))

            assertEquals("Demo", importedName)
            assertEquals("print('archive works')", targetStore.read(importedName, "src/main.py"))
            assertEquals("# Demo", targetStore.read(importedName, "README.md"))
            assertTrue(targetStore.listFiles(importedName).contains("src/main.py"))
        } finally {
            sourceRoot.deleteRecursively()
            targetRoot.deleteRecursively()
        }
    }

    @Test
    fun importingTheSameProjectTwiceDoesNotOverwriteExistingFiles() {
        val sourceRoot = Files.createTempDirectory("codeforge-source-test").toFile()
        val targetRoot = Files.createTempDirectory("codeforge-target-test").toFile()
        try {
            val sourceStore = ProjectStore(sourceRoot)
            sourceStore.save("Demo", "main.py", "print(1)")
            val archive = ByteArrayOutputStream()
            sourceStore.exportProject("Demo", archive)

            val targetStore = ProjectStore(targetRoot)
            val bytes = archive.toByteArray()
            assertEquals("Demo", targetStore.importProject(ByteArrayInputStream(bytes)))
            assertEquals("Demo_imported1", targetStore.importProject(ByteArrayInputStream(bytes)))
            assertEquals("print(1)", targetStore.read("Demo", "main.py"))
        } finally {
            sourceRoot.deleteRecursively()
            targetRoot.deleteRecursively()
        }
    }

    @Test
    fun rejectsZipSlipPaths() {
        val sandbox = Files.createTempDirectory("codeforge-zip-slip-test").toFile()
        try {
            val archive = ByteArrayOutputStream()
            ZipOutputStream(archive).use { zip ->
                zip.putNextEntry(ZipEntry("../escape.txt"))
                zip.write("not safe".toByteArray())
                zip.closeEntry()
            }

            val store = ProjectStore(sandbox)
            var rejected = false
            try {
                store.importProject(ByteArrayInputStream(archive.toByteArray()))
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue("ZIP path traversal must be rejected", rejected)
            assertFalse(java.io.File(sandbox, "escape.txt").exists())
        } finally {
            sandbox.deleteRecursively()
        }
    }
}
