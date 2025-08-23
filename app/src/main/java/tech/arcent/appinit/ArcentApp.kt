package tech.arcent.appinit

/*
 app start stuff: init sentry only when dsn not blank + set global handlers kinda simple but effective
 */

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineExceptionHandler
import tech.arcent.BuildConfig
import tech.arcent.crash.CrashReporting
import tech.arcent.crash.safeInstallDefaultHandler
import javax.inject.Inject

@HiltAndroidApp
class ArcentApp : Application() {
    @Inject
    lateinit var globalCoroutineExceptionHandler: CoroutineExceptionHandler

    override fun onCreate() {
        super.onCreate()
        initSentryIfEnabled()
        installGlobalHandlers()
    }

    /* only initialize sentry when enabled + dsn non blank else skip completely */
    private fun initSentryIfEnabled() {
        val enabled = BuildConfig.SENTRY_ENABLED && BuildConfig.SENTRY_DSN.isNotBlank()
        if (enabled) {
            SentryAndroid.init(this) { opts ->
                opts.dsn = BuildConfig.SENTRY_DSN
                opts.isSendDefaultPii = true
                opts.tracesSampleRate = 1.0
                opts.isEnableUserInteractionTracing = true
                opts.isAttachScreenshot = true
                opts.isAttachViewHierarchy = true
                opts.setDiagnosticLevel(SentryLevel.ERROR)
                /* Session Replay configuration */
                opts.sessionReplay.onErrorSampleRate = 1.0
                opts.sessionReplay.sessionSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.1
            }
        } else {
            CrashReporting.disable()
        }
    }

    /* set default handlers for any uncaught exception + forward to sentry if active */
    private fun installGlobalHandlers() {
        safeInstallDefaultHandler { throwable ->
            CrashReporting.capture(throwable)
        }
        CrashReporting.setCoroutineHandler(globalCoroutineExceptionHandler)
    }
}
