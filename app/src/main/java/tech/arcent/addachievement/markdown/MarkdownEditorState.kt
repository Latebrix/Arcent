package tech.arcent.addachievement.markdown

import androidx.compose.ui.text.TextRange

internal data class MarkdownEditorState(
    val raw: String,
    val selection: TextRange,
) {
    private val heading2Token = "## "
    private val heading1Token = "# "
    private val quoteToken = "> "
    private val bulletToken = "- "
    private val numberedRegex = Regex("^\\d+\\. ")

    private fun lines(): MutableList<String> = raw.split('\n').toMutableList()

    private fun selectionLineRange(): IntRange {
        val startLine = raw.substring(0, selection.start).count { it == '\n' }
        val endLine = raw.substring(0, selection.end).count { it == '\n' }
        return startLine..endLine
    }

    private fun rebuild(
        newLines: List<String>,
        newSel: TextRange? = null,
        onValueChange: (String) -> Unit,
    ): MarkdownEditorState {
        val newText = newLines.joinToString("\n")
        onValueChange(newText)
        return copy(raw = newText, selection = newSel ?: TextRange(newText.length))
    }

    internal fun onExternalEdit(
        newRaw: String,
        newSel: TextRange,
    ): MarkdownEditorState = copy(raw = newRaw, selection = newSel)

    internal fun toggleHeading(
        level: Int,
        onValueChange: (String) -> Unit,
    ): MarkdownEditorState {
        val r = selectionLineRange()
        val ls = lines()
        val token = if (level == 1) heading1Token else heading2Token
        val allSet = r.all { i -> i < ls.size && ls[i].startsWith(token) }
        r.forEach { i ->
            if (i < ls.size) {
                var l = ls[i]
                if (l.startsWith(heading2Token)) l = l.removePrefix(heading2Token)
                if (l.startsWith(heading1Token)) l = l.removePrefix(heading1Token)
                if (!allSet) l = token + l
                ls[i] = l
            }
        }
        // keep original selection so consecutive heading toggles operate on same lines
        return rebuild(ls, selection, onValueChange)
    }

    internal fun toggleQuote(onValueChange: (String) -> Unit): MarkdownEditorState {
        val r = selectionLineRange()
        val ls = lines()
        val all = r.all { i -> i < ls.size && ls[i].startsWith(quoteToken) }
        r.forEach {
                i ->
            if (i < ls.size) {
                var l = ls[i]
                if (l.startsWith(quoteToken)) {
                    l = l.removePrefix(quoteToken)
                } else if (!all) {
                    l = quoteToken + l
                }
                ls[i] = l
            }
        }
        return rebuild(ls, null, onValueChange)
    }

    internal fun toggleBullet(onValueChange: (String) -> Unit): MarkdownEditorState {
        val r = selectionLineRange()
        val ls = lines()
        val all = r.all { i -> i < ls.size && ls[i].startsWith(bulletToken) }
        r.forEach {
                i ->
            if (i < ls.size) {
                var l = ls[i]
                numberedRegex.find(l)?.let { if (it.range.first == 0) l = l.removeRange(it.range) }
                if (l.startsWith(bulletToken)) {
                    l = l.removePrefix(bulletToken)
                } else if (!all) {
                    l = bulletToken + l
                }
                ls[i] = l
            }
        }
        return rebuild(ls, null, onValueChange)
    }

    internal fun toggleNumbered(onValueChange: (String) -> Unit): MarkdownEditorState {
        val r = selectionLineRange()
        val ls = lines()
        val all = r.all { i -> i < ls.size && numberedRegex.find(ls[i])?.range?.first == 0 }
        r.forEach { i ->
            if (i < ls.size) {
                var l = ls[i]
                if (l.startsWith(bulletToken)) l = l.removePrefix(bulletToken)
                ls[i] = l
            }
        }
        if (all) {
            r.forEach {
                    i ->
                if (i < ls.size) {
                    val m = numberedRegex.find(ls[i])
                    if (m != null && m.range.first == 0) ls[i] = ls[i].removeRange(m.range)
                }
            }
        } else {
            r.forEach {
                    i ->
                if (i < ls.size) {
                    val m = numberedRegex.find(ls[i])
                    if (m == null || m.range.first != 0) ls[i] = "1. " + ls[i]
                }
            }
        }
        var num = 1
        ls.indices.forEach {
                i ->
            val m = numberedRegex.find(ls[i])
            if (m != null && m.range.first == 0) {
                val rest = ls[i].removeRange(m.range)
                ls[i] = "$num. " + rest
                num++
            }
        }
        return rebuild(ls, null, onValueChange)
    }

    internal fun toggleBold(onValueChange: (String) -> Unit): MarkdownEditorState {
        if (selection.collapsed) return this
        val selText = raw.substring(selection.start, selection.end)
        val wrapped = selText.startsWith("**") && selText.endsWith("**") && selText.length >= 4
        val new = if (wrapped) raw.substring(0, selection.start) + selText.removePrefix("**").removeSuffix("**") + raw.substring(selection.end) else raw.substring(0, selection.start) + "**" + selText + "**" + raw.substring(selection.end)
        val delta = if (wrapped) -4 else 4
        val newSel = TextRange(selection.start, selection.end + delta)
        onValueChange(new)
        return copy(raw = new, selection = newSel)
    }

    internal fun toggleItalic(onValueChange: (String) -> Unit): MarkdownEditorState {
        if (selection.collapsed) return this
        val selText = raw.substring(selection.start, selection.end)
        if (selText.startsWith("**") && selText.endsWith("**")) return this
        val wrapped = selText.startsWith("*") && selText.endsWith("*") && selText.length >= 2 && !(selText.startsWith("**") && selText.endsWith("**"))
        val new = if (wrapped) raw.substring(0, selection.start) + selText.removePrefix("*").removeSuffix("*") + raw.substring(selection.end) else raw.substring(0, selection.start) + "*" + selText + "*" + raw.substring(selection.end)
        val delta = if (wrapped) -2 else 2
        val newSel = TextRange(selection.start, selection.end + delta)
        onValueChange(new)
        return copy(raw = new, selection = newSel)
    }
}
