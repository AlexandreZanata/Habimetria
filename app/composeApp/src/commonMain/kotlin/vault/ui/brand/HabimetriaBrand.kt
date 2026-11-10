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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand mark from docs/05-mobile/assets/habimetria-mark.svg: a geometric H
// whose stems read as walls and central tab as a passage. Drawn from one
// 120-unit grid so header, icon and PDF stay proportional.
private data class MarkRect(val x: Float, val y: Float, val width: Float, val height: Float)

private val markRects = listOf(
    MarkRect(16f, 18f, 20f, 84f),
    MarkRect(84f, 18f, 20f, 84f),
    MarkRect(16f, 50f, 88f, 20f),
    MarkRect(50f, 70f, 20f, 18f),
)

@Composable
fun HabimetriaMark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(size)) {
        drawMark(tint)
    }
}

fun DrawScope.drawMark(tint: Color) {
    val unit = this.size.minDimension / 120f
    markRects.forEach { rect ->
        drawRect(
            color = tint,
            topLeft = Offset(rect.x * unit, rect.y * unit),
            size = Size(rect.width * unit, rect.height * unit),
        )
    }
}

@Composable
fun HabimetriaWordmark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    markSize: Dp = 24.dp,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Habimetria" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HabimetriaMark(size = markSize, tint = tint)
        Text(
            text = "habimetria",
            color = tint,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
            ),
        )
    }
}
