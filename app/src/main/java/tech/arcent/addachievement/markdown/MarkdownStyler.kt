package tech.arcent.addachievement.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Build styled annotated markdown (headings, quote, bold, italic).
internal fun buildStyledMarkdown(
    raw: String,
    accent: Color,
): AnnotatedString =
    buildAnnotatedString {
        val heading2Token = "## "
        val heading1Token = "# "
        val quoteToken = "> "
        val bulletToken = "- "
        val numberedRegex = Regex("^\\d+\\. ")
        raw.split('\n').forEachIndexed { idx, line ->
            val kindData =
                when {
                    line.startsWith(heading2Token) -> heading2Token.length to "h2"
                    line.startsWith(heading1Token) -> heading1Token.length to "h1"
                    line.startsWith(quoteToken) -> quoteToken.length to "quote"
                    line.startsWith(bulletToken) -> 0 to "bullet"
                    numberedRegex.find(line)?.range?.first == 0 -> 0 to "number"
                    else -> 0 to "none"
                }
            val (tokLen, kind) = kindData
            val lineStart = length
            append(line)
            val lineEnd = length
            if (tokLen > 0 && (kind == "h1" || kind == "h2" || kind == "quote")) {
                addStyle(
                    SpanStyle(color = Color.Transparent),
                    lineStart,
                    lineStart + tokLen,
                )
            }
            when (kind) {
                "h1" -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp), lineStart + tokLen, lineEnd)
                "h2" -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp), lineStart + tokLen, lineEnd)
                "quote" -> addStyle(SpanStyle(color = accent.copy(alpha = .85f), fontStyle = FontStyle.Italic), lineStart + tokLen, lineEnd)
            }
            "\\*\\*(.+?)\\*\\*".toRegex().findAll(line).forEach { m ->
                val s = lineStart + m.range.first
                val e = lineStart + m.range.last + 1
                if (e - s >= 4) {
                    addStyle(SpanStyle(color = Color.Transparent), s, s + 2)
                    addStyle(SpanStyle(color = Color.Transparent), e - 2, e)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), s + 2, e - 2)
                }
            }
            val italicRegex = Regex("(?<!\\*)\\*([^*]+?)\\*(?!\\*)")
            italicRegex.findAll(line).forEach { m ->
                val segment = line.substring(m.range)
                if (segment.startsWith("**") || segment.endsWith("**")) return@forEach
                val s = lineStart + m.range.first
                val e = lineStart + m.range.last + 1
                if (e - s >= 2) {
                    addStyle(SpanStyle(color = Color.Transparent), s, s + 1)
                    addStyle(SpanStyle(color = Color.Transparent), e - 1, e)
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), s + 1, e - 1)
                }
            }
            if (idx < raw.count { it == '\n' }) append('\n')
        }
    }
