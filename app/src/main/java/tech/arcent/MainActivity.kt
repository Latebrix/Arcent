package tech.arcent

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
    private var contentHasLoaded = false
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash: SplashScreen = try { installSplashScreen() } catch (e: Exception) { CrashReporting.capture(e); installSplashScreen() }
        try { super.onCreate(savedInstanceState) } catch (e: Exception) { CrashReporting.capture(e) }
        try { startLoadingContent() } catch (e: Exception) { CrashReporting.capture(e) }
        try { setupSplashScreen(splash) } catch (e: Exception) { CrashReporting.capture(e) }
        try { authViewModel.onEvent(AuthEvent.CheckSession, applicationContext) } catch (e: Exception) { CrashReporting.capture(e) }
        try { splash.setKeepOnScreenCondition { authViewModel.uiState.value.isChecking } } catch (e: Exception) { CrashReporting.capture(e) }
        try { setTheme(R.style.Theme_Arcent) } catch (e: Exception) { CrashReporting.capture(e) }
        try { enableEdgeToEdge() } catch (e: Exception) { CrashReporting.capture(e) }
        try { setContent { AppTheme { RootApp(authViewModel) } } } catch (e: Exception) { CrashReporting.capture(e) }
    }

    private fun startLoadingContent() { try { Timer().schedule(2000) { contentHasLoaded = true } } catch (e: Exception) { CrashReporting.capture(e) } }

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
            splashScreen.setOnExitAnimationListener { splashView -> splashView.remove() }
        } catch (e: Exception) { CrashReporting.capture(e) }
    }
}
