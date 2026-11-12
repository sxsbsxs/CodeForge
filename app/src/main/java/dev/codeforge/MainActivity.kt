package dev.codeforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Ink = Color(0xFF111111)
private val Paper = Color(0xFFF7F7F5)
private val Muted = Color(0xFF777777)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        setContent { CodeForgeScreen(filesDir) }
    }
}

@Composable
private fun CodeForgeScreen(privateDir: File) {
    val store = remember { ProjectStore(privateDir) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext
    val languages = listOf("Python", "Java", "C++", "JavaScript")

    var language by remember { mutableStateOf("Python") }
    var projectName by remember { mutableStateOf("MyProject") }
    var fileName by remember { mutableStateOf("main.py") }
    var source by remember { mutableStateOf(ProjectStore.starter("Python")) }
    var output by remember { mutableStateOf("Ready. Code execution stays on this device.") }
    var busy by remember { mutableStateOf(false) }
    var showProjects by remember { mutableStateOf(false) }
    var projects by remember { mutableStateOf(store.listProjects()) }

    fun selectLanguage(next: String) {
        language = next
        fileName = ProjectStore.defaultFileName(next)
        source = ProjectStore.starter(next)
        output = ProjectStore.capabilityHint(next)
    }

    fun saveCurrent(): String {
        return try {
            store.save(projectName, fileName, source)
            projects = store.listProjects()
            "Saved locally: $projectName/$fileName"
        } catch (t: Throwable) {
            "Save failed: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink,
            onPrimary = Color.White,
            background = Paper,
            surface = Color.White,
            onSurface = Ink,
            onBackground = Ink
        )
    ) {
        Column(
            Modifier.fillMaxSize().background(Paper).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CODEFORGE", fontSize = 12.sp, letterSpacing = 3.sp, color = Muted)
                    Text("Workspace", fontSize = 27.sp, color = Ink)
                }
                Text("LOCAL IDE", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                languages.forEach { item ->
                    FilterChip(
                        selected = language == item,
                        onClick = { selectLanguage(item) },
                        label = { Text(item) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Project") }
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("File") }
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { output = saveCurrent() },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { projects = store.listProjects(); showProjects = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open project") }
                    DropdownMenu(
                        expanded = showProjects,
                        onDismissRequest = { showProjects = false }
                    ) {
                        if (projects.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No saved projects yet") },
                                onClick = { showProjects = false },
                                enabled = false
                            )
                        } else {
                            projects.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        showProjects = false
                                        projectName = name
                                        val loaded = store.loadFirstCodeFile(name)
                                        if (loaded != null) {
                                            fileName = loaded.first
                                            language = ProjectStore.languageForFile(loaded.first)
                                            source = loaded.second
                                            output = "Opened $name/${loaded.first}"
                                        } else {
                                            output = "Project is empty."
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        projectName = "Project${System.currentTimeMillis() % 100000}"
                        fileName = ProjectStore.defaultFileName(language)
                        source = ProjectStore.starter(language)
                        output = "New project ready. Press Save to keep it."
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("New") }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fileName, color = Muted, fontSize = 12.sp)
                Text(ProjectStore.runnerLabel(language), color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))

            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(13.dp)
            ) {
                BasicTextField(
                    value = source,
                    onValueChange = { source = it },
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Ink,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (!busy) {
                            output = saveCurrent()
                            busy = true
                            val runLanguage = language
                            val runSource = source
                            scope.launch {
                                output = withContext(Dispatchers.IO) {
                                    LanguageRunner.run(runLanguage, runSource, context)
                                }
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) { Text(if (busy) "Running…" else "▶ Run") }

                OutlinedButton(
                    onClick = {
                        output = when (language) {
                            "Python" -> "Python APK export is not available yet. The embedded Python engine runs scripts locally; packaging still needs a local Android build toolchain."
                            "Java" -> "Java can compile and run locally through Termux OpenJDK. Creating an APK still needs the Android SDK, Gradle and a generated app host."
                            "JavaScript" -> "JavaScript runs locally through Rhino. APK packaging needs a WebView Android host template and the local Android build toolchain."
                            else -> "C++ can compile locally through Termux clang++. Packaging an Android app needs the Android NDK/CMake and a native host template."
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Package APK") }
            }

            Spacer(Modifier.height(10.dp))
            Text("OUTPUT", fontSize = 10.sp, letterSpacing = 2.sp, color = Muted)
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier.fillMaxWidth().height(112.dp)
                    .background(Ink, RoundedCornerShape(10.dp)).padding(12.dp)
            ) {
                Text(
                    output,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                "Private local workspace • No source-code upload",
                color = Muted,
                fontSize = 10.sp
            )
        }
    }
}
