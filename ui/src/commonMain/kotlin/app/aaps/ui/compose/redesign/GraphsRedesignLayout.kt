package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
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
 * @param legendHeight least height of the legend line at the top of a graph; a longer legend (large font) takes more, from its own plot
 * @param locked edit mode (step 9): no inspect pill and no sideways scroll on the graphs. The page still scrolls.
 * @param blockOverlay edit mode (step 9): drawn over each graph block, on top of it. It dims the graph, takes
 *   its touches and holds its controls. Null: nothing is drawn.
 */
@Immutable
data class GraphsRedesignLayout(
    val smallHeight: Dp,
    val graphGap: Dp = 6.dp,
    val topPadding: Dp = 0.dp,
    val bottomPadding: Dp = 0.dp,
    val quickLaunchSpace: Boolean = false,
    val legendHeight: Dp = 22.dp,
    val locked: Boolean = false,
    val blockOverlay: (@Composable BoxScope.(GraphBlock) -> Unit)? = null
)

/**
 * The graph a [GraphsRedesignLayout.blockOverlay] is drawn over.
 *
 * @param kind the BG graph, the IOB / basal graph, or one of the other graphs
 * @param index for [Kind.OTHER], its place in `GraphConfig.secondaryGraphs`; 0 for the two fixed graphs
 * @param openMenu opens the stock menu of this graph (series, overlays), the one its pencil opened
 */
class GraphBlock(val kind: Kind, val index: Int, val openMenu: () -> Unit) {

    enum class Kind { BG, IOB, OTHER }
}
