package tech.arcent.auth.data

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Functions
import io.appwrite.services.Storage
import java.net .SocketTimeoutException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import tech.arcent.BuildConfig
import tech.arcent.crash.CrashReporting
import java.io.File

/*
 * NATIVE Google login functionality via Appwrite Cloud Function (While AppWrite's Google sign up performed through a webview).
 */
class AppwriteAuthRepository : AuthRepository {
    private fun client(context: Context): Client = AppwriteClientProvider.get(context)

    private fun applySessionSecret(
        c: Client,
        secret: String,
    ) {
        c.addHeader("X-Appwrite-Session", secret)
    }

    /*
     * Executing the function with the ID token.
     */
    override suspend fun loginWithGoogle(
        context: Context,
        idToken: String,
    ) {
        CrashReporting.breadcrumb("auth", "google_login_start")
        withTimeout(170_000L) { performGoogleLogin(context, idToken) }
    }

    private suspend fun performGoogleLogin(context: Context, idToken: String) {
        val c = client(context)
        val functions = Functions(c)
        var attempt = 0
        var saved = false
        while (attempt < 3 && !saved) {
            attempt++
            CrashReporting.breadcrumb("auth", "google_login_attempt_${attempt}")
            val execution = try {
                functions.createExecution(
                    functionId = BuildConfig.appwrite_function_id,
                    body = """{"idToken":"$idToken"}""",
                    method = "POST",
                    headers = mapOf("Content-Type" to "application/json"),
                )
            } catch (e: Exception) {
                if (e is SocketTimeoutException) {
                    CrashReporting.nonFatal("google_login_socket_timeout_attempt_${attempt}", e)
                } else {
                    CrashReporting.capture(e)
                }
                CrashReporting.breadcrumb("auth", "google_login_function_call_failed_${attempt}")
                continue
            }
            val raw = execution.responseBody
            if (raw.isNullOrBlank()) {
                CrashReporting.breadcrumb("auth", "google_login_empty_response_${attempt}")
                continue
            }
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (json == null) {
                CrashReporting.breadcrumb("auth", "google_login_json_parse_fail_${attempt}")
                continue
            }
            if (!json.optBoolean("success")) {
                CrashReporting.breadcrumb("auth", "google_login_response_not_success_${attempt}")
                continue
            }
            val sessionObj = json.optJSONObject("session")
            if (sessionObj == null) {
                CrashReporting.breadcrumb("auth", "google_login_no_session_obj_${attempt}")
                continue
            }
            val sessionSecretRaw = sessionObj.optString("secret")
            val sessionSecret = if (sessionSecretRaw.isNotBlank()) sessionSecretRaw else null
            if (sessionSecret == null) {
                CrashReporting.breadcrumb("auth", "google_login_no_secret_${attempt}")
                continue
            }
            applySessionSecret(c, sessionSecret)
            val sessionId = sessionObj.optString("id").takeIf { it.isNotBlank() } ?: ""
            SessionManager.save(context, sessionId, sessionSecret)
            CrashReporting.breadcrumb("auth", "google_login_success_attempt_${attempt}")
            saved = true
        }
    }

    override suspend fun fetchAndCacheProfile(context: Context) {
        val existingLocal = UserProfileStore.load(context)?.avatarPath?.takeIf { it.contains("/avatars/") || it.startsWith("/data/") || it.startsWith("/storage/") }
        val maxAttempts = 6
        var attempt = 0
        while (attempt < maxAttempts) {
            attempt++
            CrashReporting.breadcrumb("profile_fetch", "attempt_$attempt")
            val (_, storedSecret) = SessionManager.load(context) ?: return
            if (storedSecret.isNullOrBlank()) return
            val c = client(context)
            applySessionSecret(c, storedSecret)
            val success =
                runCatching { Account(c).get() }.onSuccess { acc ->
                    val name = acc.name?.takeIf { it.isNotBlank() }
                    val avatarFromPrefs =
                        runCatching {
                            val prefsAny = acc.prefs
                            val prefsMap = (prefsAny as? Map<*, *>)
                            val url = prefsMap?.get("avatarUrl") as? String
                            val profileImageId = prefsMap?.get("profileImageId") as? String
                            val remoteUrl = if (!url.isNullOrBlank()) {
                                url
                            } else if (!profileImageId.isNullOrBlank()) {
                                /* previously would build remote URL; cloud avatar disabled now */
                                null
                            } else null
                            val localPath = remoteUrl?.let { downloadAvatarToLocal(context, it) }
                            val chosen = (localPath ?: remoteUrl) ?: existingLocal
                            chosen
                        }.getOrNull()
                    UserProfileStore.saveProfile(context, name = name, avatarUrl = avatarFromPrefs, provider = "google")
                    CrashReporting.breadcrumb("profile_fetch", "success")
                }.onFailure { e -> CrashReporting.capture(e); CrashReporting.breadcrumb("profile_fetch", "fail_${attempt}") }.isSuccess
            if (success) {
                return
            } else {
                val delayMs = (500L shl (attempt - 1)).coerceAtMost(8000L)
                delay(delayMs)
            }
        }
        /* fallback: keep existing local avatar if present */
        UserProfileStore.saveProfile(context, name = UserProfileStore.load(context)?.name, avatarUrl = existingLocal, provider = "google")
        CrashReporting.breadcrumb("profile_fetch", "fallback_cached_existing_local=${existingLocal != null}")
    }

    private fun downloadAvatarToLocal(context: Context, remoteUrl: String): String? {
        return try {
            val client = OkHttpClient()
            val req = Request.Builder().url(remoteUrl).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                val dir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
                val file = File(dir, "avatar_fetch_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { it.write(bytes) }
                file.absolutePath
            }
        } catch (e: Exception) {
            CrashReporting.capture(e); null
        }
    }

    /*
     * checking for an active session
     */
    override suspend fun hasActiveSession(context: Context): Boolean =
        runCatching {
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
