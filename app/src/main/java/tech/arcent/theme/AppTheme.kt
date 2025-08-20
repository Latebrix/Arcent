package tech.arcent.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* Color palette */
private val PrimaryTeal = Color(0xFF00A99D)
private val DarkBackground = Color(0xFF1C1C1E)
private val OnDarkBackground = Color.White
private val LightGray = Color(0xFF8A8A8E)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkBackground,
    onSurface = OnDarkBackground,
    onPrimary = Color.White,
    secondaryContainer = LightGray,
    onSurfaceVariant = LightGray
)

/* Typography */
val CustomTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CustomTypography,
        content = content
    )
}
