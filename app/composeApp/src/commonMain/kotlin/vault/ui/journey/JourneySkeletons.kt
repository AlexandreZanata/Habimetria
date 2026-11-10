package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.ShotCategory
import vault.ui.SkeletonAction
import vault.ui.SkeletonBlock
import vault.ui.SkeletonCard
import vault.ui.SkeletonLine
import vault.ui.SkeletonThumbnail

@Composable
internal fun RoomRowSkeleton() {
    SkeletonCard(padding = 12.dp, spacing = 8.dp, shape = MaterialTheme.shapes.medium) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonThumbnail(48.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SkeletonLine(0.48f, 16.dp)
                SkeletonLine(0.9f, 13.dp)
            }
            SkeletonBlock(Modifier.size(18.dp), CircleShape)
        }
        SkeletonLine(1f, 4.dp)
    }
}

@Composable
internal fun RoomCaptureSkeleton() {
    SkeletonCard(tonal = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.size(44.dp), MaterialTheme.shapes.small)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(0.5f, 18.dp)
                SkeletonLine(0.94f, 14.dp)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(110, 82, 94, 58, 58).forEach {
                SkeletonBlock(Modifier.width(it.dp).height(48.dp), FilterChipDefaults.shape)
            }
        }
        SkeletonAction(height = 56.dp)
    }
    SkeletonCard(shape = MaterialTheme.shapes.medium) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            SkeletonLine(0.63f, 16.dp)
            SkeletonBlock(Modifier.size(36.dp, 20.dp))
        }
        SkeletonLine(1f, 4.dp)
        SkeletonLine(0.68f, 14.dp)
        Column {
            ShotCategory.entries.forEachIndexed { index, _ ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonBlock(Modifier.size(24.dp), CircleShape)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        SkeletonLine(0.47f, 14.dp)
                        SkeletonLine(0.61f, 12.dp)
                    }
                    SkeletonBlock(Modifier.size(36.dp, 16.dp))
                }
                if (index != ShotCategory.entries.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        SkeletonLine(0.8f, 12.dp)
    }
}
