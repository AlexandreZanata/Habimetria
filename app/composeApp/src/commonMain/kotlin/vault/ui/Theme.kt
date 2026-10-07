package vault.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Vault identity: restrained Material 3, seal-green brand cue, system font
// (zero font deps). All screens use these roles; no hardcoded colors.
private val SealGreen = Color(0xFF1B7A4D)
private val SealGreenDark = Color(0xFF7BC9A2)
private val SealContainer = Color(0xFFDDF2E5)
private val SealContainerDark = Color(0xFF0D3B26)
private val Paper = Color(0xFFF7F4EC)
private val PaperDark = Color(0xFF141311)
private val Ink = Color(0xFF1A1A1A)
private val InkSoft = Color(0xFF4A4640)
private val PaperSoft = Color(0xFFECE7DA)
private val InkRaised = Color(0xFF23211D)
private val Danger = Color(0xFF9C3B2E)
private val DangerDark = Color(0xFFE8A79D)
private val CardLight = Color(0xFFFFFFFF)
private val CardDark = Color(0xFF23211D)

private val LightColors = lightColorScheme(
    primary = SealGreen,
    onPrimary = Color.White,
    primaryContainer = SealContainer,
    onPrimaryContainer = Ink,
    secondary = InkSoft,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperSoft,
    onSurfaceVariant = InkSoft,
    surfaceContainer = PaperSoft,
    surfaceContainerHighest = CardLight,
    error = Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = SealGreenDark,
    onPrimary = Ink,
    primaryContainer = SealContainerDark,
    onPrimaryContainer = Paper,
    secondary = PaperSoft,
    background = PaperDark,
    onBackground = Paper,
    surface = PaperDark,
    onSurface = Paper,
    surfaceVariant = InkRaised,
    onSurfaceVariant = PaperSoft,
    surfaceContainer = InkRaised,
    surfaceContainerHighest = CardDark,
    error = DangerDark,
    onError = Ink,
)

private val VaultTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
)

private val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VaultTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = VaultTypography,
        shapes = VaultShapes,
        content = content,
    )
}
