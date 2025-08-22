package tech.arcent.util

/*
* test parseEpoch handles number
* + iso string and null
*/

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import tech.arcent.achievements.data.remote.util.parseEpoch

class ParseEpochTest {
    @Test
    fun numberPassThrough() {
        val now = 123456789L
        assertEquals(now, parseEpoch(now))
    }

    @Test
    fun isoParses() {
        val sample = "2024-01-02T03:04:05Z"
        val parsed = parseEpoch(sample)
        assertNotNull(parsed)
    }

    @Test
    fun nullReturnsNull() {
        assertNull(parseEpoch(null))
    }
}
