package tech.arcent.crash

/*
 helper wrappers so we dont spam huge try catch blocks maybe a bit verbose but fine
 */

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend inline fun <T> safeIo(crossinline block: suspend () -> T): T? =
    try {
        withContext(Dispatchers.IO) { block() }
    } catch (e: Exception) {
        CrashReporting.capture(e)
        null
    }

fun CoroutineScope.safeLaunch(block: suspend CoroutineScope.() -> Unit): Job =
    this.launch(CrashReporting.handler()) {
        try {
            block()
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
    }

// run sync block safe returning null if fails
inline fun <T> safe(block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        CrashReporting.capture(e)
        null
    }

// run sync block safe returning fallback if fails
inline fun <T> safeOr(
    block: () -> T,
    fallback: T,
): T =
    try {
        block()
    } catch (e: Exception) {
        CrashReporting.capture(e)
        fallback
    }
