package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.ui.property.PropertyFeaturedCoverHeight

@Composable
internal fun PropertyDetailSkeleton() {
    SkeletonCard(padding = 12.dp) {
        SkeletonBlock(Modifier.fillMaxWidth().height(PropertyFeaturedCoverHeight), MaterialTheme.shapes.medium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SkeletonLine(0.58f, 24.dp)
            SkeletonLine(0.78f, 14.dp)
        }
        SkeletonInlineActions(112)
    }
    SkeletonAction(height = 56.dp)
    SkeletonCard {
        Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SkeletonLine(0.75f, 16.dp)
                SkeletonLine(0.58f, 12.dp)
            }
            SkeletonPill(78.dp, 20.dp)
        }
    }
    SkeletonLine(0.52f, 20.dp)
    RecordPanelSkeleton()
}

@Composable
internal fun AddressFormSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(2) { SkeletonAction(height = 56.dp) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonAction(Modifier.weight(1f), 56.dp)
            SkeletonAction(Modifier.weight(2f), 56.dp)
        }
        SkeletonAction(height = 56.dp)
    }
}

@Composable
internal fun SettingsRowSkeleton(withSwitch: Boolean) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) { SkeletonLine(0.55f, 16.dp) }
            if (withSwitch) {
                SkeletonPill(52.dp, 32.dp)
            } else {
                repeat(2) { SkeletonBlock(Modifier.size(40.dp), CircleShape) }
            }
        }
    }
    if (withSwitch) content() else SkeletonCard(padding = 0.dp, shape = MaterialTheme.shapes.medium) { content() }
}

@Composable
internal fun ComparePairSkeleton(sideBySide: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SkeletonLine(0.8f, 14.dp)
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        ComparePhotoSkeleton()
                        SkeletonLine(0.38f, 14.dp)
                    }
                }
            }
        } else {
            ComparePhotoSkeleton()
        }
        SkeletonInlineActions(86, 82, 72)
    }
}

@Composable
private fun ComparePhotoSkeleton() {
    SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(ComparePhotoAspectRatio), MaterialTheme.shapes.medium)
}

@Composable
internal fun DecisionRowSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SkeletonLine(0.48f, 16.dp)
        SkeletonLine(0.72f, 14.dp)
        SkeletonInlineActions(88, 88)
    }
}

@Composable
internal fun OccurrenceRowSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SkeletonLine(0.54f, 16.dp)
        SkeletonLine(0.78f, 14.dp)
        SkeletonLine(0.20f, 14.dp)
        SkeletonAction()
    }
}

@Composable
internal fun InspectorPhotoSkeleton() {
    SkeletonCard(padding = 14.dp, spacing = 6.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonThumbnail(40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SkeletonLine(0.32f, 16.dp)
                SkeletonLine(0.64f, 12.dp)
            }
        }
        SkeletonInlineActions(86)
        SkeletonLine(0.51f, 14.dp)
        SkeletonInlineActions(122, 114)
    }
}

@Composable
internal fun SkeletonInlineActions(vararg widths: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        widths.forEach { SkeletonPill(it.dp, 36.dp) }
    }
}
