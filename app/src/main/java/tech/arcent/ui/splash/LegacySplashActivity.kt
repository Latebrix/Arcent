package tech.arcent.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import java.util.Timer
import kotlin.concurrent.schedule
import tech.arcent.MainActivity
import tech.arcent.R

class LegacySplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legacy_splash)

        splashScreen.setKeepOnScreenCondition { true }

        Timer().schedule(1500){
            routeToNextActivity()
        }
    }

    private fun routeToNextActivity() {
        val intent = Intent(this@LegacySplashActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}