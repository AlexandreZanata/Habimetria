package vault.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import vault.composeapp.generated.resources.Res
import vault.composeapp.generated.resources.manrope_bold
import vault.composeapp.generated.resources.manrope_medium
import vault.composeapp.generated.resources.manrope_regular
import vault.composeapp.generated.resources.manrope_semibold

// Habimetria identity: Pinho #123D35, Marfim #F4F3EE, Grafite #28332F and
// Sálvia #A7B8A7 (docs/05-mobile/brand-identity.md). All screens use these
// roles; no hardcoded colors. Dark roles are independent, so light edits
// never recolor dark mode. Manrope ships as static 400/500/600/700.
private val LightPrimary = Color(0xFF123D35)
internal val LightNavigationWhite = Color.White
private val LightOnPrimary = Color(0xFFF4F3EE)
private val LightPrimaryContainer = Color(0xFFA7B8A7)
private val LightOnPrimaryContainer = Color(0xFF123D35)
private val LightSecondary = Color(0xFFA7B8A7)
private val LightOnSecondary = Color(0xFF28332F)
private val LightBackground = Color(0xFFF4F3EE)
private val LightOnBackground = Color(0xFF28332F)
private val LightSurface = Color(0xFFFBFAF6)
private val LightOnSurface = Color(0xFF28332F)
private val LightSurfaceVariant = Color(0xFFE6EAE3)
private val LightOnSurfaceVariant = Color(0xFF28332F)
private val LightSurfaceDim = Color(0xFFE6EAE3)
private val LightSurfaceBright = Color(0xFFF4F3EE)
private val LightSurfaceLowest = Color(0xFFFBFAF6)
private val LightSurfaceLow = Color(0xFFF7F6F1)
private val LightSurfaceContainer = Color(0xFFEDEEE7)
private val LightSurfaceHigh = Color(0xFFF4F3EE)
private val LightSurfaceHighest = Color(0xFFE6EAE3)
private val LightOutline = Color(0xFF6B7A72)
private val LightOutlineVariant = Color(0xFFC9D2CA)
private val LightError = Color(0xFF9C3B2E)
private val LightOnError = Color(0xFFF4F3EE)

private val DarkPrimary = Color(0xFFBFD3C4)
private val DarkOnPrimary = Color(0xFF123D35)
private val DarkPrimaryContainer = Color(0xFF1E3A32)
private val DarkOnPrimaryContainer = Color(0xFFE8EFE9)
private val DarkSecondary = Color(0xFFA7B8A7)
private val DarkOnSecondary = Color(0xFF28332F)
private val DarkBackground = Color(0xFF0E1512)
private val DarkOnBackground = Color(0xFFF4F3EE)
private val DarkSurface = Color(0xFF15201C)
private val DarkOnSurface = Color(0xFFF4F3EE)
private val DarkSurfaceVariant = Color(0xFF1E2C27)
private val DarkOnSurfaceVariant = Color(0xFFC2CEC5)
private val DarkSurfaceDim = Color(0xFF0E1512)
private val DarkSurfaceBright = Color(0xFF1E2C27)
private val DarkSurfaceLowest = Color(0xFF0E1512)
private val DarkSurfaceLow = Color(0xFF0E1512)
private val DarkSurfaceContainer = Color(0xFF15201C)
private val DarkSurfaceHigh = Color(0xFF1E2C27)
private val DarkSurfaceHighest = Color(0xFF1E2C27)
private val DarkOutline = Color(0xFF7C8C83)
private val DarkOutlineVariant = Color(0xFF2C3C35)
private val DarkError = Color(0xFFE8A79D)
private val DarkOnError = Color(0xFF28332F)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    inverseSurface = LightPrimary,
    inverseOnSurface = LightOnPrimary,
    inversePrimary = LightPrimaryContainer,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceLowest,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    inverseSurface = LightPrimary,
    inverseOnSurface = LightOnPrimary,
    inversePrimary = LightPrimaryContainer,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
)

@Composable
private fun habimetriaTypography(): Typography {
    val manrope = FontFamily(
        Font(Res.font.manrope_regular, FontWeight.Normal),
        Font(Res.font.manrope_medium, FontWeight.Medium),
        Font(Res.font.manrope_semibold, FontWeight.SemiBold),
        Font(Res.font.manrope_bold, FontWeight.Bold),
    )
    return Typography(
        displaySmall = TextStyle(
            fontFamily = manrope, fontSize = 38.sp, lineHeight = 42.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (-1.5).sp,
        ),
        labelSmall = TextStyle(
            fontFamily = manrope, fontSize = 11.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = manrope, fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        headlineSmall = TextStyle(fontFamily = manrope, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
        headlineMedium = TextStyle(fontFamily = manrope, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 38.sp, letterSpacing = (-1).sp),
        titleLarge = TextStyle(fontFamily = manrope, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = manrope, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = manrope, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = manrope, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = manrope, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = manrope, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = manrope, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    )
}

private val HabimetriaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

internal val LocalHabimetriaDarkTheme = staticCompositionLocalOf { false }

@Composable
fun HabimetriaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = habimetriaTypography(),
        shapes = HabimetriaShapes,
    ) {
        CompositionLocalProvider(LocalHabimetriaDarkTheme provides dark, content = content)
    }
}
