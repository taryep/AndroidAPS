package app.aaps.ui.compose.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.data.model.ActiveSceneState
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TT
import app.aaps.core.interfaces.notifications.AapsNotification
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.core.interfaces.pump.BolusProgressState
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.core.ui.compose.LocalConfig
import app.aaps.core.ui.compose.dialogs.OkCancelDialog
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.main.TempTargetChipState
import app.aaps.ui.compose.overview.aapsClient.AapsClientStatusCard
import app.aaps.ui.compose.overview.chips.ChipsViewModel
import app.aaps.ui.compose.overview.chips.SensitivityUiState
import app.aaps.ui.compose.overview.graphs.GraphViewModel
import app.aaps.ui.compose.overview.statusLights.StatusViewModel
import app.aaps.ui.compose.scenes.ActiveSceneBanner

/** The stock overview values the home page shows, as `OverviewScreen` has them. */
class RedesignHomeStock(
    val profileName: String,
    val isProfileModified: Boolean,
    val tempTargetText: String,
    val tempTargetState: TempTargetChipState,
    val tempTargetReason: TT.Reason?,
    val runningMode: RM.Mode?,
    val runningModeText: String,
    val runningModeRemaining: String,
    val tbrState: TbrState,
    val commandsAllowed: Boolean,
    val statusLightsDef: PreferenceSubScreenDef,
    val activeSceneState: ActiveSceneState?,
    val sceneExpired: Boolean,
    val activeSceneChainTargetName: String?,
    val onEndScene: () -> Unit,
    val onDismissScene: () -> Unit,
    val endSceneEnabled: Boolean,
    val formatDuration: (Long) -> String,
    val notifications: List<AapsNotification>,
    val pumpActivity: Boolean,
    val bolusState: BolusProgressState?,
    /** What the pump does now, in the stock words of the pump status banner (empty when it is idle). */
    val pumpStatusText: String = ""
)

/**
 * Page 1 of the redesign overview (SPEC 3.1 to 3.6), from top to bottom: the scene banner, the circle
 * row (glucose circle with two slots on each side), the consumables bar, on a client the status of
 * the master, the full-width graph card, the sensitivity line (when shown) and the 2 × 2 tiles.
 *
 * The page is a column at least as high as the screen. The two flexible gaps (circle row to
 * consumables, graph to tiles) share what is left, each 12 dp at least. The plot height is what
 * remains after the other blocks and the two minimum gaps, between 160 and 290 dp. With less room the
 * page scrolls.
 */
@Composable
fun RedesignHomePage(
    stock: RedesignHomeStock,
    inputs: RedesignHomeInputs,
    graphViewModel: GraphViewModel,
    chipsViewModel: ChipsViewModel,
    statusViewModel: StatusViewModel,
    onNavigate: (NavigationRequest) -> Unit,
    onIobClick: () -> Unit,
    onBasalClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPumpActivityClick: () -> Unit,
    graphRange: HomeGraphRange,
    onGraphRangeChange: (HomeGraphRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgInfoState by graphViewModel.bgInfoState.collectAsStateWithLifecycle()
    val iob by chipsViewModel.iobUiState.collectAsStateWithLifecycle()
    val stockCob by chipsViewModel.cobUiState.collectAsStateWithLifecycle()
    val cob = inputs.cob ?: stockCob
    val sensitivity by chipsViewModel.sensitivityUiState.collectAsStateWithLifecycle()
    val status by statusViewModel.uiState.collectAsStateWithLifecycle()
    var statusExpanded by rememberSaveable { mutableStateOf(false) }
    val config = LocalConfig.current
    val density = LocalDensity.current
    // The graph shows the target in the colour of the Target slot when it is not the profile target.
    val redesign = redesignColors()
    val targetAccent = when (stock.tempTargetState) {
        TempTargetChipState.Active   -> redesign.targetTemp.accent
        TempTargetChipState.Adjusted -> AapsTheme.generalColors.adjusted
        TempTargetChipState.None     -> null
    }
    val tilesState = RedesignTilesState(
        profileName = stock.profileName,
        isProfileModified = stock.isProfileModified,
        tempTargetText = stock.tempTargetText,
        tempTargetState = stock.tempTargetState,
        tempTargetReason = stock.tempTargetReason,
        runningMode = stock.runningMode,
        runningModeText = stock.runningModeText,
        runningModeRemaining = stock.runningModeRemaining,
        tbrState = stock.tbrState,
        iob = iob,
        cob = cob,
        data = inputs.data,
        loopSuggestion = inputs.loopSuggestion
    )
    // Heights of the blocks that come and go or grow (scene banner, client card, open consumables panel).
    var sceneHeightPx by remember { mutableIntStateOf(0) }
    var clientHeightPx by remember { mutableIntStateOf(0) }
    var consumablesHeightPx by remember { mutableIntStateOf(0) }
    val showSensitivity = redesignShowsSensitivity(sensitivity)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val extra = with(density) {
            sceneHeightPx.toDp() + (if (config.AAPSCLIENT) clientHeightPx.toDp() + BLOCK_GAP else 0.dp) +
                (consumablesHeightPx.toDp() - CONSUMABLES_HEIGHT).coerceAtLeast(0.dp)
        }
        // The blocks of fixed height, without the two flexible gaps (each 12 dp at least).
        val fixed = TOP_GAP + CIRCLE_ROW_HEIGHT + CONSUMABLES_HEIGHT + GRAPH_GAP + GRAPH_CHROME_HEIGHT +
            TILES_HEIGHT + (if (showSensitivity) SENSITIVITY_HEIGHT + BLOCK_GAP else 0.dp) + BOTTOM_PADDING + extra
        val plotHeight = (maxHeight - fixed - MIN_GAP * 2).coerceIn(PLOT_MIN_HEIGHT, PLOT_MAX_HEIGHT)

        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
        ) {
            // The page starts under the status bar (the padding of MainScreen), then this gap.
            Spacer(Modifier.height(TOP_GAP))

            // A running scene stays visible and can be ended from here, as before.
            Box(Modifier.padding(horizontal = PAGE_PADDING).onSizeChanged { sceneHeightPx = it.height }) {
                ActiveSceneBanner(
                    activeState = stock.activeSceneState,
                    expired = stock.sceneExpired,
                    chainTargetName = stock.activeSceneChainTargetName,
                    onEndClick = stock.onEndScene,
                    onDismiss = stock.onDismissScene,
                    endEnabled = stock.endSceneEnabled,
                    formatDuration = stock.formatDuration
                )
            }

            // A. Circle row: [alert / loop] [circle] [bell / target]. The side columns keep their width, so the circle never moves.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(CIRCLE_ROW_HEIGHT).padding(horizontal = PAGE_PADDING)
            ) {
                Column(Modifier.weight(1f)) {
                    SlotCell {
                        RedesignLeftSlotContent(inputs, stock.pumpActivity, stock.bolusState, stock.pumpStatusText, onPumpActivityClick)
                    }
                    SlotCell {
                        RedesignLoopSlot(tilesState, stock.commandsAllowed, onNavigate, inputs.onLoopSuggestionClick)
                    }
                }
                val circle = inputs.circle ?: bgInfoState
                RedesignBgCircle(bgInfo = circle.bgInfo, timeAgoText = circle.timeAgoText)
                Column(Modifier.weight(1f)) {
                    SlotCell { RedesignBellButton(stock.notifications.size, onNotificationsClick) }
                    SlotCell { RedesignTargetSlot(tilesState, stock.commandsAllowed, onNavigate) }
                }
            }

            FlexibleGap()

            // B. Consumables bar, 6 dp from each screen edge.
            Box(Modifier.padding(horizontal = CONSUMABLES_SIDE).onSizeChanged { consumablesHeightPx = it.height }) {
                RedesignConsumablesBar(
                    sensorStatus = status.sensorStatus,
                    insulinStatus = status.insulinStatus,
                    cannulaStatus = status.cannulaStatus,
                    batteryStatus = status.batteryStatus,
                    showFill = status.showFill,
                    showPumpBatteryChange = status.showPumpBatteryChange,
                    commandsAllowed = stock.commandsAllowed,
                    onNavigate = onNavigate,
                    statusLightsDef = stock.statusLightsDef,
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = it },
                    isPatchPump = status.isPatchPump,
                    pumpHasMenu = status.pumpHasMenu
                )
            }

            // On a client the status of the master, as on the stock screen.
            if (config.AAPSCLIENT) {
                Spacer(Modifier.height(BLOCK_GAP))
                val nsClientStatus by graphViewModel.nsClientStatusFlow.collectAsStateWithLifecycle()
                val flavorTint = when {
                    config.AAPSCLIENT3 -> AapsTheme.generalColors.flavorClient3Tint
                    config.AAPSCLIENT2 -> AapsTheme.generalColors.flavorClient2Tint
                    else               -> AapsTheme.generalColors.flavorClient1Tint
                }
                Box(Modifier.padding(horizontal = PAGE_PADDING).onSizeChanged { clientHeightPx = it.height }) {
                    AapsClientStatusCard(statusData = nsClientStatus, flavorTint = flavorTint)
                }
            }

            Spacer(Modifier.height(GRAPH_GAP))

            // C. The home graph (SPEC 3.6), full width. A drawing of fixed geometry: its text does not grow
            // with the system font, or the axis and hour labels would overlap.
            RedesignFontScaleCap(1f) {
                RedesignHomeGraph(
                    graphViewModel = graphViewModel,
                    range = graphRange,
                    onRangeChange = onGraphRangeChange,
                    targetAccent = targetAccent,
                    modifier = Modifier.fillMaxWidth().height(plotHeight + GRAPH_CHROME_HEIGHT)
                )
            }

            // The sensitivity line sits 6 dp under the graph (step 2, decided 10 Nov; it was under the tiles). The
            // flexible gap then runs from it to the tiles.
            if (showSensitivity) {
                Spacer(Modifier.height(BLOCK_GAP))
                RedesignSensitivityLine(sensitivity)
            }

            FlexibleGap()

            // D. Tiles, 316 dp wide at most and centred.
            RedesignTiles(
                state = tilesState,
                commandsAllowed = stock.commandsAllowed,
                onNavigate = onNavigate,
                onIobClick = onIobClick,
                onBasalClick = onBasalClick,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = PAGE_PADDING).widthIn(max = TILES_WIDTH)
            )
            Spacer(Modifier.height(BOTTOM_PADDING))
        }
    }
}

/** One of the two 100 dp rows of a side column of the circle row; the slot is centred in it. */
@Composable
private fun SlotCell(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(SLOT_ROW_HEIGHT)) { content() }
}

/** A gap of 12 dp at least that also takes an equal share of the free height. */
@Composable
private fun ColumnScope.FlexibleGap() {
    Spacer(Modifier.height(MIN_GAP))
    Spacer(Modifier.weight(1f))
}

/** True when the sensitivity line has something to show (the same test as the line itself). */
fun redesignShowsSensitivity(state: SensitivityUiState): Boolean = state.asText.isNotEmpty() || state.isfFrom.isNotEmpty()

/**
 * One line between the graph and the tiles (SPEC 3.4): "⇅ Sensitivity 80 % · ISF 36 → 45". Shown only when the stock
 * sensitivity chip would show something; a tap opens the same dialog.
 */
@Composable
fun RedesignSensitivityLine(state: SensitivityUiState) {
    if (!redesignShowsSensitivity(state)) return
    val colors = redesignColors()
    var showDialog by remember { mutableStateOf(false) }
    val label = TextStyle(fontSize = 12.sp, color = colors.textSecondary)
    val value = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontFeatureSettings = "tnum")
    val description = if (state.asText.isNotEmpty() && state.isfFrom.isNotEmpty())
        stringResource(UiStrings.redesign_sensitivity_description, state.asText, state.isfFrom, state.isfTo)
    else null
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clickable(enabled = state.dialogText.isNotEmpty()) { showDialog = true }
            .semantics(mergeDescendants = true) { if (description != null) contentDescription = description }
    ) {
        Icon(Icons.Default.SwapVert, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
        if (state.asText.isNotEmpty()) {
            Text(stringResource(UiStrings.redesign_sensitivity), style = label)
            Text(state.asText, style = value)
        }
        // A gap, no separator sign, between the two parts.
        if (state.asText.isNotEmpty() && state.isfFrom.isNotEmpty()) Spacer(Modifier.width(8.dp))
        if (state.isfFrom.isNotEmpty()) {
            Text(stringResource(UiStrings.redesign_isf), style = label)
            Text(state.isfFrom, style = value)
            if (state.isfTo.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(12.dp))
                Text(state.isfTo, style = value)
            }
        }
    }
    if (showDialog) {
        OkCancelDialog(
            title = stringResource(CoreUiStrings.sensitivity),
            message = state.dialogText,
            onConfirm = { showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

private val PAGE_PADDING = 16.dp
private val CONSUMABLES_SIDE = 6.dp
private val BLOCK_GAP = 6.dp
private val TOP_GAP = 12.dp
private val BOTTOM_PADDING = 14.dp

/** Smallest space of each of the two flexible gaps. */
private val MIN_GAP = 12.dp
private val GRAPH_GAP = 6.dp
private val CIRCLE_ROW_HEIGHT = 200.dp
private val SLOT_ROW_HEIGHT = 100.dp
private val CONSUMABLES_HEIGHT = 32.dp
private val TILES_HEIGHT = 112.dp
private val TILES_WIDTH = 316.dp
private val SENSITIVITY_HEIGHT = 24.dp

/**
 * What the graph card needs besides the plot: the range buttons (40), the hour labels (16), the space
 * above and under (4 + 6) and the two 1 dp borders.
 */
private val GRAPH_CHROME_HEIGHT = 68.dp
private val PLOT_MIN_HEIGHT = 160.dp
private val PLOT_MAX_HEIGHT = 290.dp
