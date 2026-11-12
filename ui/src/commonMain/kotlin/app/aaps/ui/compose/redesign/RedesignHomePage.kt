package app.aaps.ui.compose.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.aaps.ui.compose.notificationsSheet.NotificationFab
import app.aaps.ui.compose.overview.OverviewStatusSection
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
    val bolusState: BolusProgressState?
)

/**
 * Page 1 of the redesign overview (SPEC 3.1 to 3.5), from top to bottom: the scene banner, the
 * glucose circle with a button on each side, the tiles, the sensitivity line, the consumables and,
 * on a client, the status of the master. The home graph (block C) comes in phase 4, under them.
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
    onOpenGraphs: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPumpActivityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgInfoState by graphViewModel.bgInfoState.collectAsStateWithLifecycle()
    val iob by chipsViewModel.iobUiState.collectAsStateWithLifecycle()
    val cob by chipsViewModel.cobUiState.collectAsStateWithLifecycle()
    val sensitivity by chipsViewModel.sensitivityUiState.collectAsStateWithLifecycle()
    val status by statusViewModel.uiState.collectAsStateWithLifecycle()
    var statusExpanded by rememberSaveable { mutableStateOf(false) }
    val config = LocalConfig.current

    Column(
        verticalArrangement = Arrangement.spacedBy(BLOCK_GAP),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // The page starts under the status bar (the padding of MainScreen), then this gap.
            .padding(start = PAGE_PADDING, end = PAGE_PADDING, top = TOP_GAP, bottom = 8.dp)
    ) {
        // A running scene stays visible and can be ended from here, as before.
        ActiveSceneBanner(
            activeState = stock.activeSceneState,
            expired = stock.sceneExpired,
            chainTargetName = stock.activeSceneChainTargetName,
            onEndClick = stock.onEndScene,
            onDismiss = stock.onDismissScene,
            endEnabled = stock.endSceneEnabled,
            formatDuration = stock.formatDuration
        )

        // Block B: [left slot] [circle] [right slot]. The slots keep their width, so the circle never moves.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).sizeIn(minHeight = SLOT_SIZE)) {
                RedesignLeftSlotContent(inputs, stock.pumpActivity, stock.bolusState, onPumpActivityClick)
            }
            RedesignBgCircle(bgInfo = bgInfoState.bgInfo, timeAgoText = bgInfoState.timeAgoText, onClick = onOpenGraphs)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).sizeIn(minHeight = SLOT_SIZE)) {
                NotificationFab(
                    notificationCount = stock.notifications.size,
                    highestLevel = stock.notifications.minByOrNull { it.level.ordinal }?.level,
                    onClick = onNotificationsClick
                )
            }
        }

        // Block A: tiles, sensitivity line, consumables.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RedesignTiles(
                state = RedesignTilesState(
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
                ),
                commandsAllowed = stock.commandsAllowed,
                onNavigate = onNavigate,
                onIobClick = onIobClick,
                onBasalClick = onBasalClick,
                onLoopSuggestionClick = inputs.onLoopSuggestionClick
            )
            RedesignSensitivityLine(sensitivity)
            // The stock consumables pill, unchanged (decision of phase 3).
            OverviewStatusSection(
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
                onExpandedChange = { statusExpanded = it }
            )
        }

        // On a client the status of the master, as on the stock screen.
        if (config.AAPSCLIENT) {
            val nsClientStatus by graphViewModel.nsClientStatusFlow.collectAsStateWithLifecycle()
            val flavorTint = when {
                config.AAPSCLIENT3 -> AapsTheme.generalColors.flavorClient3Tint
                config.AAPSCLIENT2 -> AapsTheme.generalColors.flavorClient2Tint
                else               -> AapsTheme.generalColors.flavorClient1Tint
            }
            AapsClientStatusCard(statusData = nsClientStatus, flavorTint = flavorTint)
        }
    }
}

/**
 * One line under the tiles (SPEC 3.4): "⇅ Sensitivity 80 % · ISF 36 → 45". Shown only when the stock
 * sensitivity chip would show something; a tap opens the same dialog.
 */
@Composable
fun RedesignSensitivityLine(state: SensitivityUiState) {
    if (state.asText.isEmpty() && state.isfFrom.isEmpty()) return
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
        if (state.asText.isNotEmpty() && state.isfFrom.isNotEmpty()) Text("·", style = label)
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
private val BLOCK_GAP = 12.dp
private val TOP_GAP = 12.dp
