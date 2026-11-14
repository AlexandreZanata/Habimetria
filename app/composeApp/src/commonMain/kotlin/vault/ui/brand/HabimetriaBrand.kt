package vault.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Outline traced from the monogram reference on a 120-unit square.
@Composable
fun HabimetriaMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(size)) {
        val unit = this.size.minDimension / 120f
        val outline = Path().apply {
            moveTo(27f * unit, 31f * unit)
            lineTo(43.6f * unit, 31f * unit)
            lineTo(43.6f * unit, 53f * unit)
            lineTo(76.4f * unit, 53f * unit)
            lineTo(76.4f * unit, 31f * unit)
            lineTo(93f * unit, 31f * unit)
            lineTo(93f * unit, 89f * unit)
            lineTo(76.4f * unit, 89f * unit)
            lineTo(76.4f * unit, 70.4f * unit)
            lineTo(65.4f * unit, 70.4f * unit)
            lineTo(65.4f * unit, 64.4f * unit)
            lineTo(54.6f * unit, 64.4f * unit)
            lineTo(54.6f * unit, 70.4f * unit)
            lineTo(43.6f * unit, 70.4f * unit)
            lineTo(43.6f * unit, 89f * unit)
            lineTo(27f * unit, 89f * unit)
            close()
        }
        drawPath(outline, tint)
    }
}
