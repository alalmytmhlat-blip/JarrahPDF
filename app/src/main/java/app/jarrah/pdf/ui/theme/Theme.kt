package app.jarrah.pdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection

val Ink = Color(0xFF0F1B2D)
val Teal = Color(0xFF0B7A75)
val TealBright = Color(0xFF3CCFC6)
val Scalpel = Color(0xFFE5484D)
val Bone = Color(0xFFF5F7F9)

private val LightColors = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    secondary = Scalpel, onSecondary = Color.White,
    background = Bone, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFE3EAEE), onSurfaceVariant = Color(0xFF44525C),
)

private val DarkColors = darkColorScheme(
    primary = TealBright, onPrimary = Ink,
    secondary = Color(0xFFFF7A7E), onSecondary = Ink,
    background = Color(0xFF0B1420), onBackground = Color(0xFFE8EEF2),
    surface = Color(0xFF142131), onSurface = Color(0xFFE8EEF2),
    surfaceVariant = Color(0xFF1E2F42), onSurfaceVariant = Color(0xFFB4C2CC),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun JarrahTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // الواجهة عربية: اتجاه من اليمين لليسار دائماً
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
