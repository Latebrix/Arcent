package tech.arcent

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import tech.arcent.auth.AuthEvent
import tech.arcent.auth.AuthViewModel
import tech.arcent.crash.CrashReporting
import tech.arcent.theme.AppTheme
import tech.arcent.ui.RootApp
import java.util.Timer
import kotlin.concurrent.schedule

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    var contentHasLoaded = false
    private val authViewModel: AuthViewModel by viewModels()

    // lifecycle create doing splash install + auth session check
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen: SplashScreen =
            try {
                installSplashScreen()
            } catch (
                e: Exception,
            ) {
                CrashReporting.capture(e)
                installSplashScreen()
            }
        try {
            super.onCreate(savedInstanceState)
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
        try {
            startLoadingContent()
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
        try {
            setupSplashScreen(splashScreen)
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
        try {
            authViewModel.onEvent(AuthEvent.CheckSession, applicationContext)
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
        try {
            splashScreen.setKeepOnScreenCondition { authViewModel.uiState.value.isChecking }
        } catch (
            e: Exception,
        ) {
            CrashReporting.capture(e)
        }
        try {
            setTheme(R.style.Theme_Arcent)
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
        try {
            setContent { AppTheme { RootApp(externalVm = authViewModel) } }
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
    }

    // start timer to fake loading kinda cheap timer :)
    private fun startLoadingContent() {
        try {
            Timer().schedule(2000) { contentHasLoaded = true }
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
    }

    // configure splash exit animation and pre draw gating small risk view null so wrapped
    private fun setupSplashScreen(splashScreen: SplashScreen) {
        try {
            val content: View = findViewById(android.R.id.content)
            content.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        return if (contentHasLoaded) {
                            content.viewTreeObserver.removeOnPreDrawListener(this)
                            true
                        } else {
                            false
                        }
                    }
                },
            )
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                val slideBack =
                    ObjectAnimator.ofFloat(
                        splashScreenView.view,
                        View.TRANSLATION_X,
                        0f,
                        -splashScreenView.view.width.toFloat(),
                    ).apply {
                        interpolator = DecelerateInterpolator()
                        duration = 800L
                        doOnEnd { splashScreenView.remove() }
                    }
                slideBack.start()
            }
        } catch (e: Exception) {
            CrashReporting.capture(e)
        }
    }
}
