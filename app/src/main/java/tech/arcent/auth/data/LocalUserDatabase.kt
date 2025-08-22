package tech.arcent.auth.data

import android.content.Context
import android.util.Base64
import androidx.room.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Entity(tableName = "user_profile")
internal data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "name_enc") val nameEnc: String,
)

@Dao
internal interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}

@Database(entities = [UserProfileEntity::class], version = 1, exportSchema = false)
internal abstract class LocalUserRoomDb : RoomDatabase() {
    abstract fun dao(): UserProfileDao

    companion object {
        @Volatile private var inst: LocalUserRoomDb? = null

        fun get(context: Context): LocalUserRoomDb =
            inst ?: synchronized(this) {
                inst ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalUserRoomDb::class.java,
                    "local_user_room.db",
                ).fallbackToDestructiveMigration().build().also { inst = it }
            }
    }
}

private object RoomFieldCrypto {
    private const val PREFS = "room_user_key_store"
    private const val KEY = "k"
    private const val KEY_LEN = 32

    private fun keyBytes(context: Context): ByteArray {
        val master = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs =
            EncryptedSharedPreferences.create(
                context,
                PREFS,
                master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        val existing = prefs.getString(KEY, null)
        if (existing != null) return Base64.decode(existing, Base64.NO_WRAP)
        val gen = ByteArray(KEY_LEN).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY, Base64.encodeToString(gen, Base64.NO_WRAP)).apply()
        return gen
    }

    private fun secret(context: Context): SecretKey = SecretKeySpec(keyBytes(context), "AES")

    fun enc(
        context: Context,
        plain: String,
    ): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, secret(context), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    }

    fun dec(
        context: Context,
        enc: String,
    ): String? =
        runCatching {
            val bytes = Base64.decode(enc, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, 12)
            val ct = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secret(context), GCMParameterSpec(128, iv))
            val pt = cipher.doFinal(ct)
            String(pt, Charsets.UTF_8)
        }.getOrNull()
}

internal object LocalUserStore {
    suspend fun saveLocalUserName(
        context: Context,
        name: String,
    ) {
        val db = LocalUserRoomDb.get(context)
        val enc = RoomFieldCrypto.enc(context, name.trim())
        db.dao().insert(UserProfileEntity(nameEnc = enc))
        UserProfileStore.saveProfile(context, name = name.trim(), avatarUrl = null, provider = "local")
    }

    suspend fun loadLocalUserName(context: Context): String? {
        val db = LocalUserRoomDb.get(context)
        val entity = db.dao().getProfile() ?: return null
        return RoomFieldCrypto.dec(context, entity.nameEnc)
    }
}
