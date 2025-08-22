package tech.arcent.auth.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists appwrite auth artifacts locally (encrypted at rest).
 */
object SessionManager {
    private const val PREFS = "appwrite_session_store"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_SESSION_SECRET = "session_secret"

    private fun prefs(context: Context): android.content.SharedPreferences {
        val masterKey =
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(
        context: Context,
        sessionId: String,
        sessionSecret: String?,
    ) {
        prefs(context).edit().apply {
            putString(KEY_SESSION_ID, sessionId)
            if (sessionSecret != null) putString(KEY_SESSION_SECRET, sessionSecret) else remove(KEY_SESSION_SECRET)
            apply()
        }
    }

    fun load(context: Context): Pair<String, String?>? {
        val p = prefs(context)
        val id = p.getString(KEY_SESSION_ID, null) ?: return null
        val secret = p.getString(KEY_SESSION_SECRET, null)
        return id to secret
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
