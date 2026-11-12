package app.aaps.ui.compose.redesign

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How the stock graphs are laid out on the redesign graphs page (SPEC 4). Passed to `GraphsSection`
 * as one parameter; null there keeps the stock layout.
 *
 * Every graph has a fixed height from its size (see [GraphHeights]). The height includes the legend, so
 * the page holds exactly four Small graphs, or Grand + Small + Small.
 *
 * @param smallHeight the height of a Small graph (legend + plot). A Medium graph and a Grand graph are
 *   computed from it by `GraphHeights.of`.
 * @param graphGap space between two graphs
 * @param topPadding space above the first graph
 * @param bottomPadding space under the last graph: 0, so the graphs end at the bottom bar
 * @param quickLaunchSpace keep the space the stock overview leaves at the end for the QuickLaunch
 *   toolbar (a spacer and the hidden "Add graph" button). The redesign has no such toolbar.
 * @param legendHeight height of the legend line at the top of every graph
 */
@Immutable
data class GraphsRedesignLayout(
    val smallHeight: Dp,
    val graphGap: Dp = 12.dp,
    val topPadding: Dp = 0.dp,
    val bottomPadding: Dp = 0.dp,
    val quickLaunchSpace: Boolean = false,
    val legendHeight: Dp = 24.dp
)
