package tech.arcent

/*
 Duplication suffix provider disabled this version.

 /* TODO: */

 Original implementation retained below commented for future restoration.

/*
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface DuplicationSuffixProvider {
    fun getSuffix(): String
    fun setSuffix(value: String)
}

internal class DuplicationSuffixCore(private val getter: () -> String?, private val setter: (String?) -> Unit) {
    private val defaultSuffix = "(✦ +1)"
    fun get(): String = getter()?.takeIf { it.isNotBlank() } ?: defaultSuffix
    fun put(s: String) { setter(s.trim().ifBlank { null }) }
}

class DuplicationSuffixProviderImpl(private val context: Context) : DuplicationSuffixProvider {
    private val prefs by lazy {
        val master = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "dup_suffix_prefs",
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    private val key = "suffix"
    private val core = DuplicationSuffixCore({ prefs.getString(key, null) }, { v -> prefs.edit().putString(key, v).apply() })
    override fun getSuffix(): String = core.get()
    override fun setSuffix(value: String) { core.put(value) }
}
*/
*/
