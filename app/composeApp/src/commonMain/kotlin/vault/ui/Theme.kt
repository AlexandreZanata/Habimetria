package vault.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
