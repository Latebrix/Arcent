package tech.arcent.crash

/*
 crash helper kinda central point so we dont splash sentry calls everywhere
 */

import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object CrashReporting {
    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var coroutineHandler: CoroutineExceptionHandler? = null

    // background scope for flush so ui thread never blocked
    private val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val FLUSH_TIMEOUT_MS = 2000L

    fun disable() {
        enabled = false
    }

    fun capture(t: Throwable) {
        try {
            if (enabled) {
                Sentry.captureException(t)
                scheduleFlush()
            }
        } catch (_: Exception) {
        }
    }

    // capture non fatal
    fun nonFatal(
        message: String,
        cause: Throwable? = null,
    ) {
        if (!enabled) return
        try {
            Sentry.withScope { scope ->
                scope.setTag("non_fatal", "true")
                scope.level = SentryLevel.WARNING
                scope.addBreadcrumb(Breadcrumb.info("non_fatal_event"))
                scope.setExtra("nf_message", message)
                val ex = cause ?: RuntimeException(message)
                Sentry.captureException(ex)
            }
            scheduleFlush()
        } catch (_: Exception) {
        }
    }

    fun setCoroutineHandler(handler: CoroutineExceptionHandler) {
        coroutineHandler = handler
    }

    fun handler(): CoroutineExceptionHandler = coroutineHandler ?: CoroutineExceptionHandler { _, throwable -> capture(throwable) }

    fun <T> runOrNull(block: () -> T): T? =
        try {
            block()
        } catch (e: Exception) {
            capture(e)
            null
        }

    fun <T> runCatching(
        block: () -> T,
        onError: (Throwable) -> T,
    ): T =
        try {
            block()
        } catch (e: Exception) {
            capture(e)
            onError(e)
        }

    // ah, schedule flush on background
    private fun scheduleFlush() {
        flushScope.launch {
            try {
                Sentry.flush(FLUSH_TIMEOUT_MS)
            } catch (_: Exception) {
            }
        }
    }
}

fun safeInstallDefaultHandler(forward: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        forward(throwable)
        previous?.uncaughtException(thread, throwable)
    }
}
