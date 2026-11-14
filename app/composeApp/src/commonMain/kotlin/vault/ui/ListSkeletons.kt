package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vault.ui.property.PropertyCardRow
import vault.ui.property.PropertyFeaturedCoverHeight

@Composable
internal fun PropertyCardSkeleton(featured: Boolean = false) {
    SkeletonCard(padding = 12.dp) {
        if (featured) {
            SkeletonBlock(Modifier.fillMaxWidth().height(PropertyFeaturedCoverHeight), MaterialTheme.shapes.medium)
            PropertyMetadataSkeleton()
            SkeletonAction()
        } else {
            PropertyCardRow(cover = { SkeletonBlock(Modifier.fillMaxSize(), MaterialTheme.shapes.medium) }) {
                PropertyMetadataSkeleton()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                SkeletonLine(0.32f, 14.dp)
            }
        }
    }
}

@Composable
private fun PropertyMetadataSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) { SkeletonLine(0.82f, 16.dp) }
            SkeletonBlock(Modifier.size(18.dp), CircleShape)
        }
        SkeletonLine(0.9f, 12.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SkeletonPill(74.dp)
            SkeletonPill(100.dp)
        }
    }
}

@Composable
internal fun InspectionRowSkeleton() {
    SkeletonCard(spacing = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) { SkeletonLine(0.65f, 16.dp) }
            SkeletonBlock(Modifier.size(18.dp), CircleShape)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonLine(0.18f, 14.dp)
            SkeletonPill(105.dp)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(modifier = Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonLine(0.30f, 14.dp)
        }
    }
}

@Composable
internal fun ReportRowSkeleton() {
    SkeletonCard(padding = 12.dp, spacing = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonThumbnail(42.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(0.56f, 16.dp)
                SkeletonLine(0.8f, 12.dp)
            }
            SkeletonBlock(Modifier.size(40.dp), CircleShape)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonAction(Modifier.weight(1f), height = 48.dp)
            SkeletonAction(Modifier.weight(1f), height = 48.dp)
        }
    }
}

@Composable
internal fun RecordPanelSkeleton(featured: Boolean = false, iconSize: Dp = 42.dp) {
    SkeletonCard(spacing = 14.dp, inverse = featured) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            SkeletonThumbnail(iconSize)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(0.65f, 16.dp)
                SkeletonLine(0.94f, 13.dp)
                SkeletonLine(0.62f, 13.dp)
            }
        }
        if (featured) SkeletonAction(height = 48.dp)
    }
}
