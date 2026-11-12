package app.aaps.ui.compose.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.overview.chips.ChipsViewModel
import app.aaps.ui.compose.overview.graphs.GraphViewModel
import app.aaps.ui.compose.overview.graphs.GraphsSection
import app.aaps.ui.compose.overview.statusLights.StatusViewModel

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
    val graphConfig by graphViewModel.graphConfigFlow.collectAsStateWithLifecycle()
    // Simple mode has no edit mode (as the stock graphs): turning it on ends an open edit mode.
    LaunchedEffect(isSimpleMode) { if (isSimpleMode) redesign.editMode = false }
    RedesignOverviewContent(
        state = redesign,
        modifier = Modifier.padding(paddingValues),
        // Step 9: the (+) of the edit pill adds a Small graph at the bottom, up to the stock maximum.
        canAddGraph = RedesignGraphEdit.canAdd(graphConfig),
        onAddGraph = { graphViewModel.updateGraphConfig(RedesignGraphEdit.add(graphViewModel.graphConfigFlow.value)) },
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
                onNotificationsClick = onNotificationsClick,
                onPumpActivityClick = onPumpActivityClick,
                graphRange = redesign.homeGraphRange,
                onGraphRangeChange = { redesign.homeGraphRange = it }
            )
        },
        graphsPage = { editMode ->
            RedesignGraphsPage(graphViewModel, chipsViewModel, isSimpleMode, editMode, redesign, endSpace = overlaidBarsHeight)
        }
    )
}

/**
 * Page 2: the title row with the edit button, then the stock 4.0 graphs (SPEC 4). The graph heights come from
 * [GraphHeights]: Small is a quarter of the page, Medium and Grand are worth 1.5 and 2 Small.
 *
 * Edit mode (step 9, SPEC 4.3): the edit pill floats over the title, which turns grey. The graphs are dimmed and
 * locked, each with its controls (see [RedesignGraphEditOverlay]). The changes apply at once; the settings of the
 * start are kept in [RedesignOverviewState.editSnapshot] so (âœ•) and Back can put them back.
 */
@Composable
private fun RedesignGraphsPage(
    graphViewModel: GraphViewModel,
    chipsViewModel: ChipsViewModel,
    isSimpleMode: Boolean,
    editMode: Boolean,
    redesign: RedesignOverviewState,
    /** Space to leave after the last graph: the bars drawn over the end of the page (preview mode only). */
    endSpace: Dp
) {
    val iobUiState by chipsViewModel.iobUiState.collectAsStateWithLifecycle()
    val cobUiState by chipsViewModel.cobUiState.collectAsStateWithLifecycle()
    val graphConfig by graphViewModel.graphConfigFlow.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    // Once per install: the stock default BG height 100 becomes Grand (SPEC 4.2).
    LaunchedEffect(Unit) { graphViewModel.migrateRedesignGraphHeights() }
    // The settings when the edit mode starts, for (âœ•) and Back.
    LaunchedEffect(editMode) { if (editMode) redesign.editSnapshot = graphViewModel.graphConfigFlow.value }
    LaunchedEffect(redesign.restoreGraphs) {
        redesign.restoreGraphs?.let {
            graphViewModel.updateGraphConfig(it)
            redesign.restoreGraphs = null
        }
    }
    // A graph added with (+) is at the bottom: the page scrolls to it.
    var graphCount by remember { mutableIntStateOf(graphConfig.secondaryGraphs.size) }
    LaunchedEffect(graphConfig.secondaryGraphs.size) {
        val added = graphConfig.secondaryGraphs.size > graphCount
        graphCount = graphConfig.secondaryGraphs.size
        if (added && editMode) scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(Modifier.fillMaxSize()) {
        RedesignGraphsTitleRow(canEdit = !isSimpleMode && !editMode, dimmed = editMode, onEdit = { redesign.editScreen() })
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val layout = GraphsRedesignLayout(
                smallHeight = GraphHeights.small(maxHeight.value, GRAPH_GAP.value).dp,
                graphGap = GRAPH_GAP,
                locked = editMode,
                blockOverlay = if (editMode) {
                    { block ->
                        RedesignGraphEditOverlay(block, graphConfig, graphConfig.secondaryGraphs.size) { graphViewModel.updateGraphConfig(it) }
                    }
                } else null
            )
            Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                GraphsSection(
                    graphViewModel = graphViewModel,
                    isSimpleMode = isSimpleMode,
                    iobText = iobUiState.text,
                    cobText = cobUiState.text,
                    overlaidBarsHeight = endSpace,
                    // The stock pencils and "Add graph" stay hidden: the redesign controls replace them.
                    editMode = false,
                    redesignLayout = layout
                )
            }
        }
    }
}

private val GRAPH_GAP = 6.dp
private val TITLE_ROW_HEIGHT = 64.dp
private val EDIT_BUTTON_SIZE = 44.dp
private val EDIT_BUTTON_TOUCH_SIZE = 48.dp

/** "Graphiques" and the grey round edit button on the right. The button is hidden in simple mode and in edit mode; in edit mode the title is grey under the edit pill ([dimmed]). */
@Composable
private fun RedesignGraphsTitleRow(canEdit: Boolean, dimmed: Boolean, onEdit: () -> Unit) {
    val colors = redesignColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TITLE_ROW_HEIGHT)
            .padding(start = 20.dp, end = 16.dp - (EDIT_BUTTON_TOUCH_SIZE - EDIT_BUTTON_SIZE) / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(UiStrings.redesign_graphs_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            // Grey under the edit pill (SPEC 4.3).
            color = if (dimmed) colors.textPrimary.copy(alpha = 0.25f) else colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (canEdit) {
            // A 48 dp touch target around the 44 dp button.
            Box(
                modifier = Modifier
                    .size(EDIT_BUTTON_TOUCH_SIZE)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(EDIT_BUTTON_SIZE)
                        .background(colors.editButton, CircleShape)
                        .border(1.dp, colors.editButtonBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(UiStrings.redesign_edit_graphs),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
