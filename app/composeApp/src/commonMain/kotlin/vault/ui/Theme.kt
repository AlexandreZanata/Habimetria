package vault.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Vault palette: paper, ink, seal-green. System font (zero font deps).
private val SealGreen = Color(0xFF1B7A4D)
private val Paper = Color(0xFFF7F4EC)
private val Ink = Color(0xFF1A1A1A)

private val LightColors = lightColorScheme(
    primary = SealGreen,
    background = Paper,
    surface = Paper,
    onBackground = Ink,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = SealGreen,
    background = Ink,
    surface = Ink,
    onBackground = Paper,
    onSurface = Paper,
)

@Composable
fun VaultTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
