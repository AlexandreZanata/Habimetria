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
// Light and dark tokens are independent: changing a Light* constant never
// recolors dark mode, and dark keeps its baseline. Light follows the
// white/green contract: white backgrounds/surfaces/header/dialogs,
// seal-green primary/selection, pale-green selected containers and neutral
// #F3F4F6 supporting surfaces (no beige overlays).
private val LightPrimary = Color(0xFF1B7A4D)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFDDF2E5)
private val LightOnPrimaryContainer = Color(0xFF1A1A1A)
private val LightSecondary = Color(0xFF4B5563)
private val LightBackground = Color(0xFFFFFFFF)
private val LightOnBackground = Color(0xFF1A1A1A)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF1A1A1A)
private val LightSurfaceVariant = Color(0xFFF3F4F6)
private val LightOnSurfaceVariant = Color(0xFF4B5563)
private val LightSurfaceDim = Color(0xFFF3F4F6)
private val LightSurfaceBright = Color(0xFFFFFFFF)
private val LightSurfaceLowest = Color(0xFFFFFFFF)
private val LightSurfaceLow = Color(0xFFFFFFFF)
private val LightSurfaceContainer = Color(0xFFF3F4F6)
private val LightSurfaceHigh = Color(0xFFFFFFFF)
private val LightSurfaceHighest = Color(0xFFF3F4F6)
private val LightError = Color(0xFF9C3B2E)

private val DarkPrimary = Color(0xFF7BC9A2)
private val DarkOnPrimary = Color(0xFF1A1A1A)
private val DarkPrimaryContainer = Color(0xFF0D3B26)
private val DarkOnPrimaryContainer = Color(0xFFF7F4EC)
private val DarkSecondary = Color(0xFFECE7DA)
private val DarkBackground = Color(0xFF141311)
private val DarkOnBackground = Color(0xFFF7F4EC)
private val DarkSurface = Color(0xFF141311)
private val DarkOnSurface = Color(0xFFF7F4EC)
private val DarkSurfaceVariant = Color(0xFF23211D)
private val DarkOnSurfaceVariant = Color(0xFFECE7DA)
private val DarkSurfaceDim = Color(0xFF141311)
private val DarkSurfaceBright = Color(0xFF23211D)
private val DarkSurfaceLowest = Color(0xFF141311)
private val DarkSurfaceLow = Color(0xFF141311)
private val DarkSurfaceContainer = Color(0xFF23211D)
private val DarkSurfaceHigh = Color(0xFF23211D)
private val DarkSurfaceHighest = Color(0xFF23211D)
private val DarkError = Color(0xFFE8A79D)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceLowest,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    error = LightError,
    onError = LightOnPrimary,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    error = DarkError,
    onError = DarkOnPrimary,
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
