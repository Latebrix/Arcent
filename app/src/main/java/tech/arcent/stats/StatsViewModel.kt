package tech.arcent.stats

/*
 * ViewModel for stats screen: holds simple counter and exposes actions
 */

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tech.arcent.crash.CrashReporting

@HiltViewModel
class StatsViewModel @Inject constructor() : ViewModel() {
    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    fun increment() {
        _counter.update { it + 1 }
        CrashReporting.breadcrumb("stats", "counter_incremented:${_counter.value}")
    }

    fun testCrash() {
        try {
            throw RuntimeException("Manual test crash")
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
    }

    fun testNonFatal(message: String) {
        CrashReporting.nonFatal(message)
    }
}
