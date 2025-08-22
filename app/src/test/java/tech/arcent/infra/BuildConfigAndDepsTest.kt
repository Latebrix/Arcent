package tech.arcent.infra

/*
 * quick infra sanity: build config field
 * + coil class presence
 */

import org.junit.Assert.assertNotNull
import org.junit.Test
import tech.arcent.BuildConfig

class BuildConfigAndDepsTest {
    @Test
    fun hasSentryDsnField() {
        assertNotNull(BuildConfig.SENTRY_DSN)
    }

    @Test
    fun coilAsyncImageSymbolPresent() {
        val cls = Class.forName("coil.compose.AsyncImagePainter")
        assertNotNull(cls)
    }
}
