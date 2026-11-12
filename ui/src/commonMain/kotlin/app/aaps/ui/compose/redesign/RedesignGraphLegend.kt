package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One entry of a graph legend: a name and, when [color] is not null, a coloured dot before it. */
@Immutable
data class GraphLegendItem(val name: String, val color: Color? = null)

/**
 * The text of a legend. Each entry ("● name") is one block that never breaks: its spaces are no-break spaces. The
 * entries are separated by normal spaces, so a line can only break between two entries.
 */
internal fun legendText(items: List<GraphLegendItem>): AnnotatedString = buildAnnotatedString {
    items.forEachIndexed { index, item ->
        if (index > 0) append("  ")
        item.color?.let { color ->
            withStyle(SpanStyle(color = color)) { append("● ") }
        }
        append(item.name.replace(' ', ' '))
    }
}

/**
 * The legend of a graph on the redesign graphs page (SPEC 4.1): its own line just above the graph, entries centred.
 *
 * The text is 12 sp with the font scale capped at 1.3. If it does not fit on one line it is 10 sp; if it still does not
 * fit, it wraps between entries (see [legendText]).
 *
 * The line is [minHeight] high or as high as the text needs. The real height is given to [onHeightChange], so the
 * caller can take the difference from the plot of the same graph and the page stays full.
 */
@Composable
fun RedesignGraphLegend(
    items: List<GraphLegendItem>,
    minHeight: Dp,
    onHeightChange: (Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val outer = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(outer.density, outer.fontScale.coerceAtMost(LEGEND_MAX_FONT_SCALE))) {
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        val text = legendText(items)
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .onSizeChanged { size -> onHeightChange(with(density) { size.height.toDp() }) }
                .padding(horizontal = AXIS_LABEL_WIDTH),
            contentAlignment = Alignment.BottomCenter
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val fitsAt12 = measurer.measure(text, TextStyle(fontSize = LEGEND_SIZE.sp), softWrap = false, maxLines = 1).size.width <= widthPx
            BasicText(
                text = text,
                style = TextStyle(
                    color = textColor,
                    textAlign = TextAlign.Center,
                    fontSize = (if (fitsAt12) LEGEND_SIZE else LEGEND_SMALL_SIZE).sp
                )
            )
        }
    }
}

private const val LEGEND_SIZE = 12f
private const val LEGEND_SMALL_SIZE = 10f
private const val LEGEND_MAX_FONT_SCALE = 1.3f

/** Space kept on the left and on the right of the legend for the axis labels (the top label of the right axis). */
private val AXIS_LABEL_WIDTH = 40.dp
