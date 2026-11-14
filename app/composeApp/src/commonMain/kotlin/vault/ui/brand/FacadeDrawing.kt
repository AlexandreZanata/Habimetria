package vault.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

// Decorative elevation on a drawing grid, independent of Property photos.
@Composable
internal fun FacadeDrawing(modifier: Modifier, ink: Color) {
    Canvas(modifier) {
        val unit = minOf(size.width / 220f, size.height / 152f)
        val origin = Offset((size.width - 220f * unit) / 2, (size.height - 152f * unit) / 2)
        fun point(x: Int, y: Int) = origin + Offset(x * unit, y * unit)

        fun line(x1: Int, y1: Int, x2: Int, y2: Int, color: Color = ink) {
            drawLine(color, point(x1, y1), point(x2, y2), 1.dp.toPx(), StrokeCap.Round)
        }

        clipRect {
            val grid = ink.copy(alpha = ink.alpha * 0.23f)
            for (x in 0..220 step 16) line(x, 4, x, 148, grid)
            for (y in 4..148 step 16) line(0, y, 220, y, grid)

            line(24, 120, 192, 120)
            line(43, 60, 149, 22)
            line(149, 22, 185, 35)
            line(43, 69, 149, 32)
            line(149, 32, 185, 45)
            line(43, 60, 43, 69)
            line(185, 35, 185, 45)
            line(53, 66, 53, 120)
            line(149, 32, 149, 120)
            line(177, 42, 177, 120)
            line(53, 94, 149, 64)
            line(149, 64, 177, 73)
            line(53, 100, 149, 70)
            line(149, 70, 177, 79)
            line(63, 90, 63, 72)
            line(63, 72, 136, 46)
            line(136, 46, 136, 68)
            line(87, 63, 87, 83)
            line(112, 54, 112, 75)
            line(73, 120, 73, 105)
            line(73, 105, 104, 96)
            line(104, 96, 104, 120)
            line(117, 120, 117, 94)
            line(117, 94, 138, 88)
            line(138, 88, 138, 120)

            line(53, 134, 177, 134)
            line(53, 130, 53, 138)
            line(177, 130, 177, 138)
            line(199, 35, 199, 120)
            line(195, 35, 203, 35)
            line(195, 120, 203, 120)
        }
    }
}
