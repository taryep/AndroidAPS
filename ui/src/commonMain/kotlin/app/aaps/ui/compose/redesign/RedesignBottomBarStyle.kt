package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import app.aaps.core.ui.compose.AapsTheme

/**
 * The look of the bottom bar for the redesign (SPEC 2.3), without a change in `MainNavigationBar`:
 * the bar reads its colours from `MaterialTheme.colorScheme`, so they are set here, for the bar only,
 * and a 1 dp line is drawn on its top edge.
 *
 * Only colours can be reached this way. The sizes of the bar (item height, icon, indicator) and the
 * semi-bold selected label are fixed inside the Material 3 navigation bar.
 *
 * @param enabled false keeps the stock look (the redesign is off, or this is not the phone layout)
 */
@Composable
fun RedesignBottomBarStyle(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val base = MaterialTheme.colorScheme
    val colors = redesignColors()
    val dark = base.surface.luminance() < 0.5f
    // The in-range glucose colour is the accent of the selected item.
    val accent = AapsTheme.generalColors.bgInRange
    val scheme = base.copy(
        surface = if (dark) BAR_DARK else colors.surface,
        // The selected label (and the bar's content colour).
        onSurface = accent,
        // Labels and icons that are not selected.
        onSurfaceVariant = if (dark) BAR_LABEL_DARK else colors.textSecondary,
        // The pill behind the selected icon, and the selected icon.
        secondaryContainer = if (dark) BAR_PILL_DARK else accent.copy(alpha = 0.14f).compositeOver(colors.surface),
        onSecondaryContainer = accent
    )
    val line = if (dark) BAR_LINE_DARK else colors.border
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        Box(
            Modifier.drawBehind { drawRect(line, Offset.Zero, Size(size.width, 1.dp.toPx())) }
        ) {
            // The labels do not grow with the system font: with five items a larger label breaks in the
            // middle of a word ("Traitem / ents") and makes the bar very high.
            RedesignFontScaleCap(1f) { content() }
        }
    }
}

private val BAR_DARK = Color(0xFF121318)
private val BAR_LINE_DARK = Color(0xFF22252D)
private val BAR_LABEL_DARK = Color(0xFFC9CED8)
private val BAR_PILL_DARK = Color(0xFF1B2733)
