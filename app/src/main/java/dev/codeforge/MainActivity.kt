package dev.codeforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
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
    var language by remember { mutableStateOf("Python") }
    var source by remember { mutableStateOf("print(\"Hello from CodeForge!\")\n") }
    var output by remember { mutableStateOf("Ready. Your code stays on this device.") }
    var busy by remember { mutableStateOf(false) }
    val langs = listOf("Python", "Java", "C++", "JavaScript")

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Ink, onPrimary = Color.White, background = Paper,
        surface = Color.White, onSurface = Ink, onBackground = Ink
    )) {
        Column(Modifier.fillMaxSize().background(Paper).padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CODEFORGE", fontSize = 12.sp, letterSpacing = 3.sp, color = Muted)
                    Text("Workspace", fontSize = 28.sp)
                }
                Text("LOCAL IDE", fontSize = 11.sp, color = Muted)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                langs.forEach { item ->
                    FilterChip(selected = language == item, onClick = {
                        language = item
                        source = when(item) {
                            "Python" -> "print(\"Hello from CodeForge!\")\n"
                            "Java" -> "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}"
                            "C++" -> "#include <iostream>\nint main() { std::cout << \"Hello\\n\"; }"
                            else -> "console.log('Hello from CodeForge!');"
                        }
                        output = "Selected $item"
                    }, label = { Text(item) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("main.${when(language) { "Python" -> "py"; "Java" -> "java"; "C++" -> "cpp"; else -> "js" }}", color = Muted, fontSize = 13.sp)
                Button(onClick = {
                    if (busy) return@Button
                    busy = true
                    output = "Running locally…"
                    // Python execution is embedded with Chaquopy. Other language runners are
                    // deliberately reported as unavailable until their local toolchains ship.
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                        output = withContext(Dispatchers.IO) {
                            when(language) {
                                "Python" -> runPython(source, privateDir)
                                "JavaScript" -> "JavaScript runtime adapter is planned; no remote execution is used."
                                "Java" -> "Java source execution is not enabled yet. Android's ART runs compiled DEX, not raw .java files."
                                else -> "C++ source compilation requires an on-device NDK/Clang toolchain, not included in this build yet."
                            }
                        }
                        busy = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Ink)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp)); Text(if (busy) "Running" else "Run")
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.White).padding(14.dp)) {
                BasicTextField(
                    value = source, onValueChange = { source = it },
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 21.sp
                    )
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("OUTPUT", fontSize = 11.sp, letterSpacing = 2.sp, color = Muted)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(120.dp).background(Ink).padding(12.dp)) {
                Text(output, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("Files are stored in the app's private workspace. APK export toolchains are under development.", color = Muted, fontSize = 11.sp)
        }
    }
}

private fun runPython(code: String, privateDir: File): String {
    return try {
        val py = Python.getInstance()
        val builtins = py.getModule("builtins")
        val io = py.getModule("io")
        val sys = py.getModule("sys")
        val buffer = io.callAttr("StringIO")
        val oldOut = sys.get("stdout")
        sys.put("stdout", buffer)
        try {
            builtins.callAttr("exec", code)
            buffer.callAttr("getvalue").toString().ifBlank { "Finished (no output)." }
        } finally {
            sys.put("stdout", oldOut)
        }
    } catch (t: Throwable) {
        "Python error: ${t.message ?: t.javaClass.simpleName}"
    }
}
