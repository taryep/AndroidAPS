package app.aaps.ui.widget.glance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import app.aaps.core.interfaces.overview.graph.BgDataPoint
import app.aaps.core.interfaces.overview.graph.BgRange
import app.aaps.core.interfaces.overview.graph.BgType
import app.aaps.core.ui.compose.DarkGeneralColors

/**
 * Inputs for [BgGraphBitmapRenderer]. All BG values are in user units
 * (mg/dL or mmol/L), matching [BgDataPoint.value].
 */
data class BgGraphInput(
    val bucketed: List<BgDataPoint>,
    val fromTimeMs: Long,
    val toTimeMs: Long,
    val yMinUserUnits: Double,
    val yMaxUserUnits: Double
)

data class BgGraphColors(
    val veryLow: Int,
    val low: Int,
    val inRange: Int,
    val high: Int,
    val veryHigh: Int
) {

    /** The color for a BG range. Used for the graph dots and the BG value of every widget. */
    fun forRange(range: BgRange): Int = when (range) {
        BgRange.VERY_LOW  -> veryLow
        BgRange.LOW       -> low
        BgRange.IN_RANGE  -> inRange
        BgRange.HIGH      -> high
        BgRange.VERY_HIGH -> veryHigh
    }

    companion object {

        /**
         * The colors a widget draws BG in.
         *
         * A widget sits on the launcher over unknown wallpaper, not inside the app, so it does not
         * follow the app's light and dark themes. It always uses the dark theme BG colors of
         * [DarkGeneralColors], which are made to be read on a dark background.
         */
        val WIDGET = BgGraphColors(
            veryLow = DarkGeneralColors.bgVeryLow.toArgb(),
            low = DarkGeneralColors.bgLow.toArgb(),
            inRange = DarkGeneralColors.bgInRange.toArgb(),
            high = DarkGeneralColors.bgHigh.toArgb(),
            veryHigh = DarkGeneralColors.bgVeryHigh.toArgb()
        )
    }
}

/**
 * Minimal Canvas BG-graph renderer producing a transparent bitmap.
 * Draws only bucketed BG dots colored by [BgRange].
 */
class BgGraphBitmapRenderer {

    fun render(widthPx: Int, heightPx: Int, input: BgGraphInput, colors: BgGraphColors): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val left = PADDING_PX
        val right = widthPx - PADDING_PX
        val top = PADDING_PX
        val bottom = heightPx - PADDING_PX
        if (right <= left || bottom <= top) return bitmap
        if (input.bucketed.isEmpty()) return bitmap

        val xSpan = (input.toTimeMs - input.fromTimeMs).coerceAtLeast(1L).toFloat()
        val ySpan = (input.yMaxUserUnits - input.yMinUserUnits).coerceAtLeast(0.001).toFloat()

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        for (p in input.bucketed) {
            if (p.type != BgType.BUCKETED) continue
            if (p.timestamp !in input.fromTimeMs..input.toTimeMs) continue
            val x = left + (p.timestamp - input.fromTimeMs).toFloat() / xSpan * (right - left)
            val y = bottom - ((p.value - input.yMinUserUnits).toFloat() / ySpan) * (bottom - top)
            paint.color = colors.forRange(p.range)
            paint.alpha = if (p.filledGap) GAP_ALPHA else 255
            canvas.drawCircle(x, y, BG_RADIUS_PX, paint)
        }

        return bitmap
    }

    companion object {

        private const val PADDING_PX = 4
        private const val BG_RADIUS_PX = 6.0f
        private const val GAP_ALPHA = 110
    }
}
