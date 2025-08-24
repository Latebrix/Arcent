package tech.arcent.profile

/*
 simple broadcaster to notify ui when profile updated (name/avatar)
 */

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProfileEvents {
    private val _ticks = MutableStateFlow(0L)
    val ticks = _ticks.asStateFlow()

    fun refresh(@Suppress("UNUSED_PARAMETER") context: Context) { _ticks.value = System.currentTimeMillis() }
}

