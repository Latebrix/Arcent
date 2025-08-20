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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import tech.arcent.auth.AuthEvent
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import tech.arcent.auth.AuthViewModel
import tech.arcent.theme.AppTheme
import tech.arcent.ui.RootApp
import java.util.Timer
import kotlin.concurrent.schedule

class MainActivity : ComponentActivity() {

    var contentHasLoaded = false
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        /*
        * Installs Splash Screen before OnCreatw
        */
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        startLoadingContent()
        setupSplashScreen(splashScreen)

        //kick off session check early
        authViewModel.onEvent(AuthEvent.CheckSession, applicationContext)

        splashScreen.setKeepOnScreenCondition { authViewModel.uiState.value.isChecking }

        //switch to normal app theme after splash is installed
        setTheme(R.style.Theme_Arcent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme { RootApp(externalVm = authViewModel) }
        }
    }

    private fun startLoadingContent() {
        Timer().schedule(2000) {
            contentHasLoaded = true
        }
    }

    private fun setupSplashScreen(splashScreen: SplashScreen) {
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (contentHasLoaded) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else false
                }
            }
        )
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideBack = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_X,
                0f,
                -splashScreenView.view.width.toFloat()
            ).apply {
                interpolator = DecelerateInterpolator()
                duration = 800L
                doOnEnd { splashScreenView.remove() }
            }

            slideBack.start()
        }
    }
}
