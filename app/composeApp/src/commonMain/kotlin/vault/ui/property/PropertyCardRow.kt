package vault.ui.property

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

// Measure metadata first. Artwork's native pixel dimensions must never
// determine card height; the cover gets exactly the measured text height.
@Composable
internal fun PropertyCardRow(cover: @Composable () -> Unit, content: @Composable () -> Unit) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            cover()
            Box { content() }
        },
    ) { measurables, constraints ->
        val coverWidth = minOf(104.dp.roundToPx(), constraints.maxWidth)
        val spacing = minOf(12.dp.roundToPx(), constraints.maxWidth - coverWidth)
        val metadataWidth = constraints.maxWidth - coverWidth - spacing
        val metadata = measurables[1].measure(
            constraints.copy(minWidth = metadataWidth, maxWidth = metadataWidth, minHeight = 0),
        )
        val cover = measurables[0].measure(Constraints.fixed(coverWidth, metadata.height))
        layout(constraints.maxWidth, metadata.height) {
            cover.placeRelative(0, 0)
            metadata.placeRelative(coverWidth + spacing, 0)
        }
    }
}
