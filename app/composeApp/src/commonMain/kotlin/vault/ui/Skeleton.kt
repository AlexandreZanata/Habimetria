package vault.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Skeleton loading primitives: shimmer blocks in the shape of the real
// content, theme-aware and offline (no new dependencies). Blocks are
// decorative; the loading announcement stays with ProgressStatus.
@Composable
fun skeletonBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceContainerLow
    val transition = rememberInfiniteTransition(label = "skeleton")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonOffset",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset - 600f, 0f),
        end = Offset(offset, 0f),
    )
}

@Composable
fun SkeletonLine(fraction: Float, height: Dp = 16.dp, brush: Brush = skeletonBrush()) {
    Box(
        modifier = Modifier.fillMaxWidth(fraction).height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
            .clearAndSetSemantics {},
    )
}

@Composable
fun SkeletonThumbnail(size: Dp, brush: Brush = skeletonBrush()) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp))
            .background(brush)
            .clearAndSetSemantics {},
    )
}

@Composable
fun SkeletonPill(width: Dp, brush: Brush = skeletonBrush()) {
    Box(
        modifier = Modifier.width(width).height(30.dp).clip(CircleShape)
            .background(brush)
            .clearAndSetSemantics {},
    )
}

// Stands in for PropertyCardView while rows load: thumbnail, title,
// address lines and chips in the same positions.
@Composable
fun PropertyCardSkeleton(brush: Brush = skeletonBrush()) {
    OutlinedCard(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonThumbnail(88.dp, brush = brush)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SkeletonLine(0.6f, 20.dp, brush = brush)
                SkeletonLine(0.9f, brush = brush)
                SkeletonLine(0.7f, brush = brush)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonPill(110.dp, brush = brush)
                    SkeletonPill(80.dp, brush = brush)
                }
            }
        }
    }
}

// Stands in for an inspection row while lists load: title plus status.
@Composable
fun InspectionRowSkeleton(brush: Brush = skeletonBrush()) {
    OutlinedCard(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonLine(0.5f, 20.dp, brush = brush)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonLine(0.25f, brush = brush)
                SkeletonPill(90.dp, brush = brush)
            }
        }
    }
}

// Stands in for the home banner: title, copy lines and the action block.
@Composable
fun HomeBannerSkeleton(brush: Brush = skeletonBrush()) {
    OutlinedCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonLine(0.55f, 28.dp, brush = brush)
            SkeletonLine(0.85f, brush = brush)
            SkeletonLine(0.65f, brush = brush)
            Box(
                modifier = Modifier.fillMaxWidth().height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
                    .clearAndSetSemantics {},
            )
        }
    }
}

// Stands in for a room list row: icon box, title and progress line.
@Composable
fun RoomRowSkeleton(brush: Brush = skeletonBrush()) {
    OutlinedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(brush)
                    .clearAndSetSemantics {},
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SkeletonLine(0.5f, 20.dp, brush = brush)
                SkeletonLine(0.7f, brush = brush)
            }
        }
    }
}

// Stands in for the capture screen while coverage loads: coverage card
// with category rows plus the photo grid and the shutter action.
@Composable
fun RoomCaptureSkeleton(brush: Brush = skeletonBrush()) {
    OutlinedCard(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonLine(0.5f, 20.dp, brush = brush)
                SkeletonLine(0.15f, 20.dp, brush = brush)
            }
            SkeletonLine(1f, 8.dp, brush = brush)
            SkeletonLine(0.7f, brush = brush)
            SkeletonLine(0.7f, brush = brush)
            SkeletonLine(0.7f, brush = brush)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkeletonThumbnail(96.dp, brush)
        SkeletonThumbnail(96.dp, brush)
        SkeletonThumbnail(96.dp, brush)
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clearAndSetSemantics {},
    )
}
