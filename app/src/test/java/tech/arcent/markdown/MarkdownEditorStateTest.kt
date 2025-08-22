package tech.arcent.markdown

/*
tests for markdown state toggles; kinda minimal but hits core paths
 */

import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.arcent.addachievement.markdown.MarkdownEditorState

class MarkdownEditorStateTest {
    // bold wrapping inserted around selection
    @Test
    fun boldToggleWrapsAndUnwraps() {
        var updated = ""
        val initial = MarkdownEditorState(raw = "hello", selection = TextRange(0, 5))
        val afterBold = initial.toggleBold { updated = it }
        assertTrue(afterBold.raw.startsWith("**") && afterBold.raw.endsWith("**"))
        val afterUn = afterBold.toggleBold { updated = it }
        assertEquals("hello", afterUn.raw)
    }

    // heading 1 adds token then removes when all lines already have it
    @Test
    fun heading1Toggle() {
        var capture = ""
        val st = MarkdownEditorState("alpha\nbeta", TextRange(0, 5))
        val h1 = st.toggleHeading(1) { capture = it }
        assertTrue(h1.raw.lines()[0].startsWith("# "))
        val h1again = h1.toggleHeading(1) { capture = it }
        assertTrue(!h1again.raw.lines()[0].startsWith("# "))
    }

    // italic simple wrap ignoring bold-overlap
    @Test
    fun italicToggle() {
        var c = ""
        val st = MarkdownEditorState("code", TextRange(0, 4))
        val it1 = st.toggleItalic { c = it }
        assertTrue(it1.raw.startsWith("*") && it1.raw.endsWith("*"))
    }
}
