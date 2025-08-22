package tech.arcent.auth.data

import android.content.Context
import io.appwrite.Client
import tech.arcent.BuildConfig

/**
 * Appwrite Client singleton provider to preserve session across calls
 */
object AppwriteClientProvider {
    @Volatile private var instance: Client? = null

    fun get(context: Context): Client =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(context: Context): Client =
        Client(context)
            .setEndpoint(BuildConfig.appwrite_endpoint)
            .setProject(BuildConfig.appwrite_project_id)
            .setSelfSigned(true)

    fun reset() {
        instance = null
    }
}
