package vault.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun rememberLoadingVisible(loading: Boolean, delayMs: Long = 150L): Boolean {
    var visible by remember(loading, delayMs) { mutableStateOf(false) }
    LaunchedEffect(loading, delayMs) {
        if (loading) {
            delay(delayMs)
            visible = true
        }
    }
    return visible
}

private val LocalSkeletonPhase = staticCompositionLocalOf<State<Float>?> { null }
private val LocalSkeletonInverse = staticCompositionLocalOf { false }

@Composable
internal fun LoadingSkeleton(
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Compose's animation duration scale also stops the shimmer at zero.
    // Each group owns one phase, read only during drawing.
    val phase = rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer",
    )
    CompositionLocalProvider(LocalSkeletonPhase provides phase) {
        Column(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {
                contentDescription = Strings.loading
                liveRegion = LiveRegionMode.Polite
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

@Composable
internal fun SkeletonBlock(
    modifier: Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
) {
    val phase = LocalSkeletonPhase.current
    val inverse = LocalSkeletonInverse.current
    val colors = MaterialTheme.colorScheme
    val base = if (inverse) colors.inverseOnSurface.copy(alpha = 0.15f) else colors.surfaceContainerHighest
    val highlight = if (inverse) colors.inverseOnSurface.copy(alpha = 0.12f) else colors.surface.copy(alpha = 0.76f)
    Box(
        modifier.clip(shape).drawWithCache {
            val band = size.width * 0.55f
            val glow = Brush.horizontalGradient(
                listOf(highlight.copy(alpha = 0f), highlight, highlight.copy(alpha = 0f)),
                endX = band.coerceAtLeast(1f),
            )
            onDrawBehind {
                drawRect(base)
                if (phase != null) {
                    translate(left = (size.width + band) * phase.value - band) {
                        drawRect(glow, size = Size(band, size.height))
                    }
                }
            }
        }.clearAndSetSemantics {},
    )
}

@Composable
fun SkeletonLine(fraction: Float, height: Dp = 16.dp) {
    SkeletonBlock(Modifier.fillMaxWidth(fraction).height(height))
}

@Composable
fun SkeletonThumbnail(size: Dp) {
    SkeletonBlock(Modifier.size(size), MaterialTheme.shapes.medium)
}

@Composable
fun SkeletonPill(width: Dp, height: Dp = 24.dp) {
    SkeletonBlock(Modifier.width(width).height(height), CircleShape)
}

@Composable
internal fun SkeletonAction(modifier: Modifier = Modifier.fillMaxWidth(), height: Dp = 52.dp) {
    SkeletonBlock(modifier.height(height), MaterialTheme.shapes.medium)
}

@Composable
internal fun SkeletonCard(
    padding: Dp = 16.dp,
    spacing: Dp = 12.dp,
    shape: Shape = MaterialTheme.shapes.large,
    inverse: Boolean = false,
    tonal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = when {
            inverse -> colors.inverseSurface
            tonal -> colors.surfaceContainer
            else -> colors.surface
        },
        border = if (inverse || tonal) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        CompositionLocalProvider(LocalSkeletonInverse provides inverse) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(padding),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content,
            )
        }
    }
}
