package tech.arcent.domain

/*
small mapping tests just to be sure nothing silly breaks later
 */

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.arcent.achievements.data.repo.toUi
import tech.arcent.achievements.domain.AchievementDomain

class DomainMappingsTest {
    @Test
    fun domainToUiCopiesCoreFields() {
        val d =
            AchievementDomain(
                id = "id1",
                title = "My Title",
                details = "dets",
                achievedAt = 1234L,
                photoUrl = "http://x",
                categories = listOf("c1"),
                tags = listOf("t1", "t2"),
            )
        val ui = d.toUi()
        assertEquals(d.id, ui.id)
        assertEquals(d.title, ui.title)
        assertEquals(d.achievedAt, ui.achievedAt)
        assertEquals(d.tags, ui.tags)
        assertEquals(d.photoUrl, ui.photoUrl)
    }
}
