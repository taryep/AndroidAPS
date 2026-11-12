package app.aaps.ui.compose.redesign

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How the stock graphs are laid out on the redesign graphs page (SPEC 4). Passed to `GraphsSection`
 * as one parameter; null there keeps the stock layout.
 *
 * @param fillHeight the graphs grow (never shrink, at most to `GraphConfig.MAX_GRAPH_HEIGHT_DP` each)
 *   so the whole section is this high. Null keeps the saved heights, as in edit mode, so the height
 *   slider matches what is seen.
 * @param graphGap space between two graphs
 * @param topPadding space above the first graph (the treatment belt)
 * @param quickLaunchSpace keep the space the stock overview leaves at the end for the QuickLaunch
 *   toolbar (a spacer and the hidden "Add graph" button). The redesign has no such toolbar.
 */
@Immutable
data class GraphsRedesignLayout(
    val fillHeight: Dp?,
    val graphGap: Dp = 12.dp,
    val topPadding: Dp = 12.dp,
    val quickLaunchSpace: Boolean = false
)
