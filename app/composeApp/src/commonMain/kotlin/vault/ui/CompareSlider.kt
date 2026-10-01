package vault.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// Drag slider: exit photo full-frame, entry photo clipped to the left
// portion. Dragging never mutates data; tagging happens below.
@Composable
fun CompareSlider(
    entry: ImageBitmap?,
    exit: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    var fraction by remember { mutableFloatStateOf(0.5f) }
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().aspectRatio(4f / 3f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    fraction = (fraction + dragAmount / size.width).coerceIn(0.02f, 0.98f)
                }
            },
    ) {
        if (exit != null) {
            Image(exit, Strings.compareExit, Modifier.fillMaxSize())
        } else {
            Placeholder(Strings.compareNoPhoto)
        }
        if (entry != null) {
            Image(
                entry,
                Strings.compareEntry,
                Modifier.fillMaxSize().drawWithContent {
                    clipRect(right = size.width * fraction) {
                        this@drawWithContent.drawContent()
                    }
                },
            )
        }
        // Divider handle.
        Box(
            Modifier.align(Alignment.TopStart)
                .offset(x = maxWidth * fraction - 1.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            Strings.compareEntry,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)).padding(4.dp),
        )
        Text(
            Strings.compareExit,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)).padding(4.dp),
        )
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
