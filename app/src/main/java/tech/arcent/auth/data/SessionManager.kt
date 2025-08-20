package tech.arcent.auth.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists appwrite auth artifacts locally (encrypted at rest).
 * + Added Lazy Migration -_->
 */
object SessionManager {
    private const val PREFS = "appwrite_session_store"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_SESSION_SECRET = "session_secret"

    private fun encryptedPrefs(context: Context) : android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun legacyPrefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun prefs(context: Context): android.content.SharedPreferences {
        return encryptedPrefs(context)
    }

    fun save(context: Context, sessionId: String, sessionSecret: String?) {
        prefs(context).edit().apply {
            putString(KEY_SESSION_ID, sessionId)
            if (sessionSecret != null) putString(KEY_SESSION_SECRET, sessionSecret) else remove(KEY_SESSION_SECRET)
            apply()
        }
        legacyPrefs(context).edit().clear().apply()
    }

    fun load(context: Context): Pair<String, String?>? {
        val encrypted = prefs(context)
        val id = encrypted.getString(KEY_SESSION_ID, null)
        return if (id != null) {
            id to encrypted.getString(KEY_SESSION_SECRET, null)
        } else {
            // Attempt migration from legacy plain-text prefs
            val legacy = legacyPrefs(context)
            val legacyId = legacy.getString(KEY_SESSION_ID, null) ?: return null
            val legacySecret = legacy.getString(KEY_SESSION_SECRET, null)
            // Persist into encrypted store
            encrypted.edit().apply {
                putString(KEY_SESSION_ID, legacyId)
                if (legacySecret != null) putString(KEY_SESSION_SECRET, legacySecret) else remove(KEY_SESSION_SECRET)
                apply()
            }
            // clear legacy
            legacy.edit().clear().apply()
            legacyId to legacySecret
        }
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        legacyPrefs(context).edit().clear().apply()
    }
}
