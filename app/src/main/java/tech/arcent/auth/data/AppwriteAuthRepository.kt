package tech.arcent.auth.data

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Functions
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import tech.arcent.BuildConfig

/*
 * NATIVE Google login functionality via Appwrite Cloud Function (While AppWrite's Google sign up performed through a webview).
 */
class AppwriteAuthRepository : AuthRepository {

    private fun client(context: Context): Client = AppwriteClientProvider.get(context)

    private fun applySessionSecret(c: Client, secret: String) {
        c.addHeader("X-Appwrite-Session", secret)
    }

    /*
     * Executing the function with the ID token.
     */
    override suspend fun loginWithGoogle(context: Context, idToken: String) {
        withTimeout(170_000L) {
            val c = client(context)
            val functions = Functions(c)
            val execution = functions.createExecution(
                functionId = BuildConfig.appwrite_function_id,
                body = """{"idToken":"${idToken}"}""",
                method = "POST",
                headers = mapOf("Content-Type" to "application/json")
            )
            val raw = execution.responseBody ?: return@withTimeout
            if (raw.isBlank()) return@withTimeout
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: return@withTimeout
            if (!json.optBoolean("success")) return@withTimeout
            val sessionObj = json.optJSONObject("session") ?: return@withTimeout
            val sessionSecret = sessionObj.optString("secret").takeIf { it.isNotBlank() } ?: return@withTimeout
            applySessionSecret(c, sessionSecret)
            // persist both id+ secret, maybe we need it later
            val sessionId = sessionObj.optString("id").takeIf { it.isNotBlank() } ?: ""
            SessionManager.save(context, sessionId, sessionSecret)
        }
    }

    /*
     * checking for an active session
     */
    override suspend fun hasActiveSession(context: Context): Boolean = runCatching {
        withTimeout(170_000L) {
            val (_, storedSecret) = SessionManager.load(context) ?: return@withTimeout false
            if (storedSecret.isNullOrBlank()) return@withTimeout false
            val c = client(context)
            applySessionSecret(c, storedSecret)
            Account(c).get()
            true
        }
    }.getOrDefault(false)
}
