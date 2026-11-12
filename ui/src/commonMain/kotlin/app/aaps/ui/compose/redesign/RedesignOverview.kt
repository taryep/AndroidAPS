package app.aaps.ui.compose.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.overview.chips.ChipsViewModel
import app.aaps.ui.compose.overview.graphs.GraphViewModel
import app.aaps.ui.compose.overview.graphs.GraphsSection
import app.aaps.ui.compose.redesign.RedesignOverviewState.Companion.GRAPHS_PAGE

/**
 * Space the floating buttons take over the end of the graphs page, so the last graph stays readable
 * above them. The notification button: 56 dp, 16 dp above the bottom bar (see `MainScreen`). The pump
 * activity button, while shown: 40 dp, 72 dp above the bottom bar.
 */
fun redesignFloatingButtonsHeight(pumpButtonShown: Boolean): Dp = if (pumpButtonShown) 128.dp else 72.dp

/**
 * The phone overview: the stock stacked screen, or with [redesign] the two redesign pages.
 *
 * @param stacked the stock stacked screen, called with its padding, whether it shows the graphs,
 *   and the modifier of its glucose value
 */
@Composable
fun RedesignOverviewOrStock(
    redesign: RedesignOverviewState?,
    paddingValues: PaddingValues,
    graphViewModel: GraphViewModel,
    chipsViewModel: ChipsViewModel,
    isSimpleMode: Boolean,
    overlaidBarsHeight: Dp,
    /** True while the floating pump activity button shows (pump communicating, SMB being given). */
    pumpButtonShown: Boolean,
    stacked: @Composable (paddingValues: PaddingValues, showGraphs: Boolean, bgModifier: Modifier) -> Unit
) {
    if (redesign == null) {
        stacked(paddingValues, true, Modifier)
        return
    }
    val openGraphs = stringResource(UiStrings.redesign_open_graphs)
    RedesignOverviewPager(
        state = redesign,
        modifier = Modifier.padding(paddingValues),
        // Page 1 is the stock screen without its graphs until phase 3 builds the new home page.
        homePage = {
            stacked(PaddingValues(0.dp), false, Modifier.clickable(onClickLabel = openGraphs) { redesign.page = GRAPHS_PAGE })
        },
        graphsPage = { editMode ->
            RedesignGraphsPage(graphViewModel, chipsViewModel, isSimpleMode, editMode, overlaidBarsHeight + redesignFloatingButtonsHeight(pumpButtonShown))
        }
    )
}

/** Page 2: the stock 4.0 graphs, with their edit buttons only in edit mode. */
@Composable
private fun RedesignGraphsPage(
    graphViewModel: GraphViewModel,
    chipsViewModel: ChipsViewModel,
    isSimpleMode: Boolean,
    editMode: Boolean,
    /** Space to leave after the last graph: the bars and the floating buttons over the end of the page. */
    endSpace: Dp
) {
    val iobUiState by chipsViewModel.iobUiState.collectAsStateWithLifecycle()
    val cobUiState by chipsViewModel.cobUiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GraphsSection(
            graphViewModel = graphViewModel,
            isSimpleMode = isSimpleMode,
            iobText = iobUiState.text,
            cobText = cobUiState.text,
            overlaidBarsHeight = endSpace,
            editMode = editMode
        )
    }
}
