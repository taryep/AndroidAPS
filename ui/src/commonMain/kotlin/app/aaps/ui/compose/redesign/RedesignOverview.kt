package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.ui.compose.overview.chips.ChipsViewModel
import app.aaps.ui.compose.overview.graphs.GraphViewModel
import app.aaps.ui.compose.overview.graphs.GraphsSection
import app.aaps.ui.compose.overview.statusLights.StatusViewModel
import app.aaps.ui.compose.redesign.RedesignOverviewState.Companion.GRAPHS_PAGE

/**
 * The phone overview of the redesign: the home page and the graphs page (SPEC 2, 3 and 4). It has no
 * floating buttons (`OverviewScreen` hides them): notifications and pump activity are beside the
 * glucose circle on the home page, and the graphs page has none (SPEC correction 3.3).
 */
@Composable
fun RedesignOverview(
    redesign: RedesignOverviewState,
    stock: RedesignHomeStock,
    inputs: RedesignHomeInputs,
    paddingValues: PaddingValues,
    graphViewModel: GraphViewModel,
    chipsViewModel: ChipsViewModel,
    statusViewModel: StatusViewModel,
    isSimpleMode: Boolean,
    overlaidBarsHeight: Dp,
    onNavigate: (NavigationRequest) -> Unit,
    onIobClick: () -> Unit,
    onBasalClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPumpActivityClick: () -> Unit
) {
    RedesignOverviewPager(
        state = redesign,
        modifier = Modifier.padding(paddingValues),
        homePage = {
            RedesignHomePage(
                stock = stock,
                inputs = inputs,
                graphViewModel = graphViewModel,
                chipsViewModel = chipsViewModel,
                statusViewModel = statusViewModel,
                onNavigate = onNavigate,
                onIobClick = onIobClick,
                onBasalClick = onBasalClick,
                onOpenGraphs = { redesign.page = GRAPHS_PAGE },
                onNotificationsClick = onNotificationsClick,
                onPumpActivityClick = onPumpActivityClick
            )
        },
        graphsPage = { editMode ->
            RedesignGraphsPage(graphViewModel, chipsViewModel, isSimpleMode, editMode, overlaidBarsHeight)
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
    /** Space to leave after the last graph: the bars drawn over the end of the page (preview mode only). */
    endSpace: Dp
) {
    val iobUiState by chipsViewModel.iobUiState.collectAsStateWithLifecycle()
    val cobUiState by chipsViewModel.cobUiState.collectAsStateWithLifecycle()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The graphs fill the page. Not in edit mode: there they keep their saved height, so the
        // height slider matches what the user sees.
        val layout = GraphsRedesignLayout(fillHeight = if (editMode) null else maxHeight)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GraphsSection(
                graphViewModel = graphViewModel,
                isSimpleMode = isSimpleMode,
                iobText = iobUiState.text,
                cobText = cobUiState.text,
                overlaidBarsHeight = endSpace,
                editMode = editMode,
                redesignLayout = layout
            )
        }
    }
}
