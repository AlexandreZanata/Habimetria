package vault.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.ui.SkeletonBlock
import vault.ui.SkeletonCard
import vault.ui.SkeletonInlineActions
import vault.ui.SkeletonLine
import vault.ui.SkeletonThumbnail

@Composable
internal fun ReviewRoomSkeleton() {
    SkeletonCard(spacing = 8.dp, shape = MaterialTheme.shapes.medium) {
        SkeletonLine(0.4f, 16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonThumbnail(72.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(0.84f, 16.dp)
                SkeletonLine(0.7f, 14.dp)
            }
            SkeletonBlock(Modifier.size(82.dp, 40.dp), MaterialTheme.shapes.medium)
        }
        SkeletonInlineActions(46, 48, 54, 54)
        SkeletonLine(0.9f, 12.dp)
    }
}

@Composable
internal fun ReviewOptionSkeleton() {
    SkeletonCard(shape = MaterialTheme.shapes.medium) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.size(48.dp), CircleShape)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(0.55f, 16.dp)
                SkeletonLine(0.85f, 14.dp)
                SkeletonLine(0.63f, 14.dp)
            }
            SkeletonBlock(Modifier.size(14.dp))
        }
    }
}
