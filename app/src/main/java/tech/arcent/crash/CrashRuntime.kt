package tech.arcent.crash

/*
 runtime helper to lazy init sentry if user opts in after launch
 */

import android.content.Context
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import tech.arcent.BuildConfig

object CrashRuntimeInit {
    fun ensureInitialized(context: Context) {
        if (CrashReporting.isEnabled()) return
        if (!BuildConfig.SENTRY_ENABLED || BuildConfig.SENTRY_DSN.isBlank()) return
        try {
            SentryAndroid.init(context) { opts ->
                opts.dsn = BuildConfig.SENTRY_DSN
                opts.isSendDefaultPii = true
                opts.tracesSampleRate = 1.0
                opts.isEnableUserInteractionTracing = true
                opts.isAttachScreenshot = true
                opts.isAttachViewHierarchy = true
                opts.setDiagnosticLevel(SentryLevel.ERROR)
                opts.sessionReplay.onErrorSampleRate = 1.0
                opts.sessionReplay.sessionSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.1
            }
            CrashReporting.enable()
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
    }
}

