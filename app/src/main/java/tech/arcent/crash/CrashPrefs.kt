package tech.arcent.crash

/*
 stores user preference for crash reporting opt in;
 */

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object CrashPrefs {
    private const val PREFS = "crash_prefs"
    private const val KEY_OPT_IN = "opt_in"
    private const val KEY_SET = "opt_in_set"

    private fun prefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isSet(context: Context): Boolean = prefs(context).getBoolean(KEY_SET, false)

    fun optIn(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_OPT_IN, value).putBoolean(KEY_SET, true).apply()
    }

    // Changed: default is now ALWAYS true (previously depended on build type: true debug / false release)
    // The second parameter retained for binary compatibility but ignored for logic now.
    fun isEnabled(context: Context, @Suppress("UNUSED_PARAMETER") debug: Boolean): Boolean {
        val p = prefs(context)
        if (!p.getBoolean(KEY_SET, false)) {
            return true
        }
        return p.getBoolean(KEY_OPT_IN, true)
    }
}
