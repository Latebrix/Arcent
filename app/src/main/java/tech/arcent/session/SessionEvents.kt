package tech.arcent.session

/*
 Central session change broadcaster. Emitted after logout or successful login so screens/viewmodels can refresh their cached user scoped data.
 */

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SessionEvents {
    private val _authChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val authChanges = _authChanges.asSharedFlow()
    fun fireAuthChanged() { _authChanges.tryEmit(Unit) }
}

