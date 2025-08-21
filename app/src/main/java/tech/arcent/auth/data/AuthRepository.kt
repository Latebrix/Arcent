package tech.arcent.auth.data

import android.content.Context

interface AuthRepository {
    suspend fun loginWithGoogle(context: Context, idToken: String)
    suspend fun hasActiveSession(context: Context): Boolean
    suspend fun fetchAndCacheProfile(context: Context)
}
