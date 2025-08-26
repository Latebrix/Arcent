package tech.arcent.ui

/*
 Central system bar styling with a controller.
*/

import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

data class SystemBarStyle(val status: Color, val nav: Color, val darkIcons: Boolean)

data class SystemBarController internal constructor(private val setter: (SystemBarStyle) -> Unit) { fun set(style: SystemBarStyle) = setter(style) }

private val LocalCurrentSystemBarStyle = staticCompositionLocalOf { SystemBarStyle(Color.Transparent, Color.Transparent, false) }
val LocalSystemBarController = staticCompositionLocalOf { SystemBarController { } }

@Composable
fun SystemBarHost(content: @Composable () -> Unit) {
    val systemUi = rememberSystemUiController()
    val styleState = remember { mutableStateOf<SystemBarStyle>(SystemBarStyle(Color.Transparent, Color.Transparent, false)) }
    val controller = remember { SystemBarController { styleState.value = it } }
    val style = styleState.value
    DisposableEffect(style) {
        systemUi.setStatusBarColor(style.status, darkIcons = style.darkIcons)
        systemUi.setNavigationBarColor(style.nav, darkIcons = style.darkIcons)
        onDispose { }
    }
    CompositionLocalProvider(LocalCurrentSystemBarStyle provides style, LocalSystemBarController provides controller) { content() }
}

@Composable
fun DefaultAuthenticatedBars(): SystemBarStyle = SystemBarStyle(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background, false)
