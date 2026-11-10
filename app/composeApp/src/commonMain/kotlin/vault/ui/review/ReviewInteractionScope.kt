package vault.ui.review

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalMaterial3Api::class)
private val ReviewRipple = RippleConfiguration(
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.16f,
        focusedAlpha = 0.18f,
        hoveredAlpha = 0.10f,
        pressedAlpha = 0.14f,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewInteractionScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleConfiguration provides ReviewRipple, content = content)
}
