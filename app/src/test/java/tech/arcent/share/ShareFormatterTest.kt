package tech.arcent.share

/* unit tests for ShareFormatter covering markdown stripping and empty details */

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.arcent.home.Achievement

class ShareFormatterTest {
    @Test
    fun strips_markdown_markers() {
        val ach = Achievement(
            id = "1",
            title = "My Win",
            achievedAt = 0L,
            tags = emptyList(),
            photoUrl = null,
            details = "# Heading\n* bullet item\n> quote line\nNormal text",
            categories = emptyList(),
        )
        val text = ShareFormatter.formatText(ach)
        assertTrue(text.startsWith("My Win"))
        assertTrue(!text.contains("# Heading"))
        assertTrue(text.contains("bullet item"))
        assertTrue(text.contains("quote line"))
    }

    @Test
    fun empty_details_only_title() {
        val ach = Achievement("2", "Title Only", 0L, emptyList(), null, null, emptyList())
        val text = ShareFormatter.formatText(ach)
        assertEquals("Title Only", text)
    }
}

