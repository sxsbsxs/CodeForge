package dev.codeforge

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Lightweight monochrome syntax highlighting for the four built-in editor modes.
 * It deliberately leaves source text and cursor offsets unchanged.
 */
class CodeSyntaxTransformation(private val language: String) : VisualTransformation {
    private val keywords = mapOf(
        "Python" to listOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "False", "finally", "for",
            "from", "global", "if", "import", "in", "is", "lambda", "None",
            "nonlocal", "not", "or", "pass", "raise", "return", "True", "try",
            "while", "with", "yield"
        ),
        "Java" to listOf(
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends",
            "false", "final", "finally", "float", "for", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native", "new", "null", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "void", "volatile", "while"
        ),
        "C++" to listOf(
            "alignas", "auto", "bool", "break", "case", "catch", "char", "class",
            "const", "constexpr", "continue", "default", "delete", "do", "double",
            "else", "enum", "extern", "false", "float", "for", "if", "inline", "int",
            "long", "namespace", "new", "nullptr", "private", "protected", "public",
            "return", "short", "signed", "sizeof", "static", "struct", "switch",
            "template", "this", "throw", "true", "try", "typedef", "typename",
            "union", "unsigned", "using", "virtual", "void", "volatile", "while"
        ),
        "JavaScript" to listOf(
            "async", "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "export", "extends", "false",
            "finally", "for", "from", "function", "if", "import", "in", "instanceof",
            "let", "new", "null", "of", "return", "static", "super", "switch", "this",
            "throw", "true", "try", "typeof", "var", "void", "while", "yield"
        )
    ).getValue(language)

    private val tokenPattern = Regex(
        """(?s)/\*.*?\*/|//[^\n]*|#[^\n]*|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\b(?:${keywords.joinToString("|") { Regex.escape(it) }})\b|\b\d+(?:\.\d+)?\b"""
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        tokenPattern.findAll(text.text).forEach { match ->
            val token = match.value
            val style = when {
                token.startsWith("/*") || token.startsWith("//") ||
                    (token.startsWith("#") && language != "C++") ->
                    SpanStyle(color = Color(0xFF929292), fontStyle = FontStyle.Italic)
                token.startsWith("#") && language == "C++" ->
                    SpanStyle(color = Color(0xFF666666), fontWeight = FontWeight.SemiBold)
                token.startsWith("\"") || token.startsWith("'") ->
                    SpanStyle(color = Color(0xFF555555))
                token.firstOrNull()?.isDigit() == true ->
                    SpanStyle(color = Color(0xFF444444), fontWeight = FontWeight.Medium)
                else ->
                    SpanStyle(color = Color(0xFF111111), fontWeight = FontWeight.Bold)
            }
            builder.addStyle(style, match.range.first, match.range.last + 1)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
