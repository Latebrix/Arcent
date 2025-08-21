package tech.arcent.auth.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** secure storage for cached user profile */
object UserProfileStore {
    private const val PREFS = "user_profile_store"
    private const val KEY_NAME = "name"
    private const val KEY_AVATAR_PATH = "avatar_path"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_FETCHED = "fetched"

    private fun prefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveProfile(context: Context, name: String?, avatarUrl: String?, provider: String) {
        prefs(context).edit().apply {
            if (name != null) putString(KEY_NAME, name) else remove(KEY_NAME)
            if (avatarUrl != null) putString(KEY_AVATAR_PATH, avatarUrl) else remove(KEY_AVATAR_PATH)
            putString(KEY_PROVIDER, provider)
            putBoolean(KEY_FETCHED, true)
            apply()
        }
    }

    fun markFetched(context: Context) { prefs(context).edit().putBoolean(KEY_FETCHED, true).apply() }

    fun load(context: Context): UserProfile? {
        val p = prefs(context)
        val name = p.getString(KEY_NAME, null)
        val avatar = p.getString(KEY_AVATAR_PATH, null)
        val provider = p.getString(KEY_PROVIDER, null) ?: return null
        return UserProfile(name, avatar, provider, p.getBoolean(KEY_FETCHED, false))
    }
}

data class UserProfile(
    val name: String?,
    val avatarPath: String?,
    val provider: String,
    val fetched: Boolean
)

