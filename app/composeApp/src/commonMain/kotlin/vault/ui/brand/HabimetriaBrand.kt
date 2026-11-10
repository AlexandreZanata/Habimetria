package vault.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun HabimetriaWordmark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    markSize: Dp = 34.dp,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Habimetria" },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabimetriaMark(size = markSize, tint = tint)
        Text(
            text = "habimetria",
            color = tint,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                letterSpacing = (-1).sp,
            ),
        )
    }
}
