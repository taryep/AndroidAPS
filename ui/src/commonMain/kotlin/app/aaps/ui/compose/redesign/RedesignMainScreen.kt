package app.aaps.ui.compose.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.aaps.core.interfaces.bgQualityCheck.BgQualityCheck
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.interfaces.notifications.AapsNotification
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.pump.BolusProgressState
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.LocalDateUtil
import app.aaps.core.ui.compose.LocalSnackbarHostState
import app.aaps.core.ui.compose.dialogs.OkCancelDialog
import app.aaps.core.ui.compose.dialogs.ThreeButtonDialog
import app.aaps.core.ui.compose.metroViewModel
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.compose.aboutDialog.AboutAlertDialog
import app.aaps.ui.compose.aboutDialog.AboutDialogData
import app.aaps.ui.compose.loopSheet.LoopActionBottomSheet
import app.aaps.ui.compose.loopSheet.LoopActionViewModel
import app.aaps.ui.compose.main.MainUiState
import app.aaps.ui.compose.main.MainViewModel
import app.aaps.ui.compose.maintenance.ImportSource
import app.aaps.ui.compose.maintenance.MaintenanceDialogs
import app.aaps.ui.compose.maintenance.MaintenanceViewModel
import app.aaps.ui.compose.manageSheet.ManageSheetState
import app.aaps.ui.compose.manageSheet.ManageViewModel
import app.aaps.ui.compose.overview.OverviewScreen
import app.aaps.ui.compose.overview.chips.ChipsViewModel
import app.aaps.ui.compose.overview.graphs.GraphViewModel
import app.aaps.ui.compose.overview.statusLights.StatusViewModel
import app.aaps.ui.compose.scenesSheet.ScenesBottomSheet
import app.aaps.ui.compose.scenesSheet.ScenesViewModel
import app.aaps.ui.compose.treatmentsSheet.TreatmentBottomSheet
import app.aaps.ui.compose.treatmentsSheet.TreatmentViewModel
import app.aaps.ui.search.SearchIndexEntry
import app.aaps.ui.search.SearchUiState

/**
 * True when the redesign chrome replaces the stock one: the flag is on and the layout is the phone layout (the
 * split and tablet layouts keep the stock screen). `MainScreen` asks this once, at its start.
 */
@Composable
fun redesignChromeActive(): Boolean =
    RedesignFlags.ENABLED &&
        isRedesignPhoneLayout(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() })

/**
 * The whole main screen of the redesign (docs/redesign/SPEC.md), so `MainScreen` only has one `if` for it.
 *
 * One content area shows one of three items of the bottom bar: Home, Graphs or Menu. The item is kept in
 * [RedesignOverviewState]. The overview (home and graphs) stays composed while the Menu is shown, but is not
 * placed, so the graphs and the edit mode keep their state.
 *
 * It has no drawer, no top bar, no quick launch toolbar and no version label (the version is in Menu > About).
 * The sheets and dialogs are the ones of the stock screen.
 */
@Composable
fun RedesignMainScreen(
    mainViewModel: MainViewModel,
    uiState: MainUiState,
    aboutDialogData: AboutDialogData?,
    manageSheetState: ManageSheetState,
    manageViewModel: ManageViewModel,
    maintenanceViewModel: MaintenanceViewModel,
    statusViewModel: StatusViewModel,
    treatmentViewModel: TreatmentViewModel,
    scenesViewModel: ScenesViewModel,
    loopActionViewModel: LoopActionViewModel,
    searchUiState: SearchUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchResultClick: (SearchIndexEntry) -> Unit,
    onSearchPluginToggle: (PluginBase) -> Unit,
    onConfirmSearchPluginSwitch: () -> Unit,
    onDismissSearchPluginSwitch: () -> Unit,
    onConfirmSearchHardwarePump: () -> Unit,
    onDismissSearchHardwarePump: () -> Unit,
    onNavigate: (NavigationRequest) -> Unit,
    onDrawerClosed: () -> Unit,
    onAboutDialogDismiss: () -> Unit,
    onOpenBatteryHelp: (() -> Unit)?,
    onMaintenanceSheetDismiss: () -> Unit,
    onDirectoryClick: () -> Unit,
    onLaunchBrowser: (String) -> Unit,
    onBringToForeground: () -> Unit,
    onImportSettingsNavigate: (ImportSource) -> Unit,
    onRecreateActivity: () -> Unit,
    notifications: List<AapsNotification>,
    onDismissNotification: (AapsNotification) -> Unit,
    onNotificationActionClick: (AapsNotification) -> Unit,
    autoShowNotificationSheet: Boolean,
    onAutoShowConsumed: () -> Unit,
    pumpSetupPlugin: PluginBase?,
    bgSetupPlugin: PluginBase?,
    bgQualityState: BgQualityCheck.State?,
    objectivesSetupPlugin: PluginBase?,
    objectivesProgressText: String?,
    objectivesProgressDescription: String?,
    permissionsMissing: Boolean,
    onPermissionsClick: () -> Unit,
    calcProgress: Int,
    graphViewModel: GraphViewModel,
    chipsViewModel: ChipsViewModel,
    statusLightsDef: PreferenceSubScreenDef,
    treatmentButtonsDef: PreferenceSubScreenDef,
    bolusState: BolusProgressState?,
    pumpStatusText: String,
    queueStatusText: AnnotatedString?,
    isPumpCommunicating: Boolean,
    onStopBolus: () -> Unit,
    modifier: Modifier = Modifier
) {
    LocalDateUtil.current
    var showTreatmentSheet by remember { mutableStateOf(false) }
    var showAutomationSheet by remember { mutableStateOf(false) }
    var showLoopActionSheet by remember { mutableStateOf(false) }
    val automationState by scenesViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current

    // Chosen item of the bar (home, graphs, Menu) and edit mode. A view model: kept over a rotation and while a screen
    // opened in the Menu is shown (Back comes back to the Menu), reset by a new start.
    val overview: RedesignOverviewState = viewModel { RedesignOverviewState() }
    val menuOpen = overview.menuOpen
    val closeMenu = {
        if (searchUiState.isSearchActive) {
            onSearchClear()
            onSearchActiveChange(false)
        }
        overview.menuOpen = false
    }

    // There is no drawer here: if something asks for it, say at once that it is closed.
    LaunchedEffect(uiState.isDrawerOpen) { if (uiState.isDrawerOpen) onDrawerClosed() }

    // Tile values the stock state has not as separate texts (time left, basal rates).
    val homeViewModel = metroViewModel<RedesignHomeViewModel>()
    val homeData = homeViewModel.data.collectAsStateWithLifecycle().value
    val homeCob = homeViewModel.cob.collectAsStateWithLifecycle().value
    // Objectives, pump set-up, permissions and the loop suggestion are listed in the Menu. The BG source warning
    // goes beside the glucose circle (RedesignHomeInputs).
    val loopActionAvailable = loopActionViewModel.uiState.collectAsStateWithLifecycle().value.actionAvailable
    val attentionItems = redesignAttentionItems(
        objectivesSetupPlugin = objectivesSetupPlugin,
        objectivesProgressText = objectivesProgressText,
        objectivesProgressDescription = objectivesProgressDescription,
        pumpSetupPlugin = pumpSetupPlugin,
        permissionsMissing = permissionsMissing,
        onNavigate = onNavigate,
        onPermissionsClick = onPermissionsClick
    )

    val density = LocalDensity.current
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }

    // The bars are drawn over the content, not in the Scaffold slots: the scrims below read the raw system bar
    // insets, which a filled Scaffold slot would consume.
    Scaffold(modifier = modifier) { scaffoldPadding ->
        val contentPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding(),
            bottom = scaffoldPadding.calculateBottomPadding() + with(density) { bottomBarHeightPx.toDp() }
        )
        val eatReminderState by mainViewModel.eatReminderState.collectAsStateWithLifecycle()
        val activeSceneState by mainViewModel.activeSceneState.collectAsStateWithLifecycle()
        val sceneExpired by mainViewModel.sceneExpired.collectAsStateWithLifecycle()
        val activeSceneChainTargetName by mainViewModel.activeSceneChainTargetName.collectAsStateWithLifecycle()
        val masterReachable by mainViewModel.masterReachable.collectAsStateWithLifecycle()
        // Stable pairing signal: hides the mutating nav buttons on an unpaired client.
        val masterOrPairedClient by mainViewModel.masterOrPairedClient.collectAsStateWithLifecycle()
        // The Active insulin and Basal tiles open the insulin and temp basal screens (SPEC 3.2.2, 3.2.3).
        val tileTaps = rememberRedesignTileTaps(manageViewModel, masterOrPairedClient, onNavigate, chipsViewModel::showIobInfo, mainViewModel::showTbrInfo)

        Box(modifier = Modifier.fillMaxSize()) {
            // Home and Graphs. While the Menu is the chosen item this is composed but not placed.
            Box(Modifier.fillMaxSize().hiddenWhen(menuOpen)) {
                OverviewScreen(
                    profileName = uiState.profileName,
                    profilePsId = uiState.profilePsId,
                    isProfileModified = uiState.isProfileModified,
                    profileProgress = uiState.profileProgress,
                    tempTargetText = uiState.tempTargetText,
                    tempTargetState = uiState.tempTargetState,
                    tempTargetProgress = uiState.tempTargetProgress,
                    tempTargetReason = uiState.tempTargetReason,
                    tempTargetRecordId = uiState.tempTargetRecordId,
                    runningMode = uiState.runningMode,
                    runningModeText = uiState.runningModeText,
                    runningModeRemaining = uiState.runningModeRemaining,
                    runningModeProgress = uiState.runningModeProgress,
                    runningModeRecordId = uiState.runningModeRecordId,
                    tbrState = uiState.tbrState,
                    smbEnabled = uiState.smbEnabled,
                    isSimpleMode = uiState.isSimpleMode,
                    calcProgress = calcProgress,
                    graphViewModel = graphViewModel,
                    chipsViewModel = chipsViewModel,
                    manageViewModel = manageViewModel,
                    statusViewModel = statusViewModel,
                    statusLightsDef = statusLightsDef,
                    onNavigate = onNavigate,
                    onTbrChipClick = tileTaps.onBasalClick,
                    onIobChipClick = tileTaps.onIobClick,
                    notifications = notifications,
                    onDismissNotification = onDismissNotification,
                    onNotificationActionClick = onNotificationActionClick,
                    autoShowNotificationSheet = autoShowNotificationSheet,
                    onAutoShowConsumed = onAutoShowConsumed,
                    activeSceneState = activeSceneState,
                    sceneExpired = sceneExpired,
                    activeSceneChainTargetName = activeSceneChainTargetName,
                    onEndScene = { mainViewModel.requestSceneDeactivation() },
                    onDismissScene = { mainViewModel.dismissExpiredScene() },
                    endSceneEnabled = masterReachable,
                    commandsAllowed = masterOrPairedClient,
                    formatDuration = mainViewModel::formatDuration,
                    paddingValues = contentPadding,
                    redesign = overview,
                    redesignHome = RedesignHomeInputs(
                        data = homeData,
                        cob = homeCob,
                        bgSourceName = bgSetupPlugin?.let { plugin -> plugin.pluginDescription.pluginName?.let { stringResource(it) } ?: plugin.name },
                        bgQualityState = bgQualityState,
                        onBgSourceClick = { bgSetupPlugin?.let { onNavigate(NavigationRequest.Plugin(it::class.simpleName.orEmpty())) } },
                        eatReminder = eatReminderState,
                        onEatReminderStop = mainViewModel::stopEatReminder,
                        onEatReminderExtend = mainViewModel::extendEatReminder,
                        eatReminderControlsEnabled = masterReachable,
                        loopSuggestion = loopActionAvailable,
                        onLoopSuggestionClick = { showLoopActionSheet = true }
                    ),
                    bolusState = bolusState,
                    pumpStatusText = pumpStatusText,
                    queueStatusText = queueStatusText,
                    isPumpCommunicating = isPumpCommunicating,
                    onStopBolus = onStopBolus
                )
            }

            // Plugin enable/disable confirmations raised from search results (same dialogs as Config Builder)
            searchUiState.pluginSwitchConfirmation?.let { confirmation ->
                OkCancelDialog(
                    title = stringResource(CoreUiStrings.configbuilder_switch_confirmation_title),
                    message = stringResource(
                        CoreUiStrings.configbuilder_switch_confirmation,
                        confirmation.fromName,
                        confirmation.toName
                    ),
                    onConfirm = onConfirmSearchPluginSwitch,
                    onDismiss = onDismissSearchPluginSwitch
                )
            }
            searchUiState.hardwarePumpConfirmation?.let { confirmation ->
                OkCancelDialog(
                    title = stringResource(CoreUiStrings.confirmation),
                    message = confirmation.message,
                    onConfirm = onConfirmSearchHardwarePump,
                    onDismiss = onDismissSearchHardwarePump
                )
            }

            // Status bar protection scrim: keeps the system icons legible
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.surface)
            )

            // Navigation bar protection scrim
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(MaterialTheme.colorScheme.surface)
            )

            // Menu: the third item of the bar. Search results are shown inside it.
            if (menuOpen) {
                RedesignMenuScreen(
                    versionName = mainViewModel.versionName,
                    isSimpleMode = uiState.isSimpleMode,
                    isTreatmentsEnabled = uiState.isProfileLoaded,
                    showExit = mainViewModel.showExit,
                    searchUiState = searchUiState,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchClear = onSearchClear,
                    onSearchActiveChange = onSearchActiveChange,
                    onSearchResultClick = onSearchResultClick,
                    onSearchPluginToggle = onSearchPluginToggle,
                    onNavigate = onNavigate,
                    onClose = closeMenu,
                    bottomPadding = contentPadding.calculateBottomPadding(),
                    attentionItems = attentionItems
                )
            }

            // Bottom bar: the colours and the top line come through the theme (RedesignBottomBarStyle)
            RedesignBottomBarArea(
                bottomInset = scaffoldPadding.calculateBottomPadding(),
                onHeightChange = { if (it != bottomBarHeightPx) bottomBarHeightPx = it }
            ) {
                RedesignBottomBar(
                    onTreatmentClick = {
                        treatmentViewModel.refreshState()
                        showTreatmentSheet = true
                    },
                    onAutomationClick = {
                        scenesViewModel.refreshState()
                        showAutomationSheet = true
                    },
                    onManageClick = { manageSheetState.show() },
                    masterOrPairedClient = masterOrPairedClient,
                    quickWizardCount = uiState.quickWizardItems.size,
                    // Total drives the visibility of the item, the count drives the badge (only what the user can start now).
                    automationTotal = automationState.items.size + automationState.sceneItems.size,
                    automationCount = automationState.items.count { it.activationReason == null } +
                        automationState.sceneItems.count { it.activationReason == null },
                    graphsSelected = overview.page == RedesignOverviewState.GRAPHS_PAGE && !menuOpen,
                    onGraphsClick = {
                        // Read before closing the Menu: a re-tap on Graphs (Menu closed) goes home.
                        val menuWasOpen = menuOpen
                        closeMenu()
                        if (menuWasOpen) overview.page = RedesignOverviewState.GRAPHS_PAGE else overview.toggleGraphs()
                    },
                    menuSelected = menuOpen,
                    // Only what needs the user lights the dot: Objectives is a progress and does not.
                    menuBadge = attentionItems.any { it.needsAction },
                    onMenuClick = {
                        // A re-tap on the open Menu goes home. Menu and page change in one state: no flash.
                        if (menuOpen) {
                            closeMenu()
                            overview.showHome()
                        } else overview.toggleMenu()
                    },
                )
            }
        }
    }

    // Treatment bottom sheet. Carbs entry is the Carbs tile (SPEC 3.2.4), so no carbs button here or in the gear.
    if (showTreatmentSheet) {
        val treatmentState by treatmentViewModel.uiState.collectAsStateWithLifecycle()
        TreatmentBottomSheet(
            onDismiss = { showTreatmentSheet = false },
            showCgm = treatmentState.showCgm,
            showCalibration = treatmentState.showCalibration,
            showTreatment = treatmentState.showTreatment,
            showInsulin = treatmentState.showInsulin,
            showCarbs = false,
            showCalculator = treatmentState.showCalculator,
            isDexcomSource = treatmentState.isDexcomSource,
            showSettingsIcon = treatmentState.showSettingsIcon,
            quickWizardItems = treatmentState.quickWizardItems,
            onNavigate = onNavigate,
            treatmentButtonsDef = treatmentButtonsDef.withoutCarbsButton(),
        )
    }

    // Automation bottom sheet
    if (showAutomationSheet) {
        ScenesBottomSheet(
            onDismiss = { showAutomationSheet = false },
            automationItems = automationState.items,
            onItemClick = { item -> mainViewModel.requestAutomationConfirmation(item.eventId) },
            sceneItems = automationState.sceneItems,
            onSceneClick = { sceneId -> mainViewModel.requestSceneConfirmation(sceneId) },
            onEditScenesClick = { onNavigate(NavigationRequest.Element(ElementType.SCENE_MANAGEMENT)) }
        )
    }

    // Loop accept action bottom sheet
    if (showLoopActionSheet) {
        val loopState by loopActionViewModel.uiState.collectAsStateWithLifecycle()
        LoopActionBottomSheet(
            state = loopState,
            onPerform = { mainViewModel.performLoopAccept() },
            onDismiss = { showLoopActionSheet = false }
        )
    }

    // Shared confirmation dialog (automation actions, TT presets, scene end). With a secondary action
    // (e.g. scene chain skip) it has three buttons, otherwise OK/Cancel.
    val actionConfirmation by mainViewModel.actionConfirmation.collectAsStateWithLifecycle()
    actionConfirmation?.let { confirmation ->
        val secondaryAction = confirmation.secondaryAction
        val secondaryLabel = confirmation.secondaryLabel
        if (secondaryAction != null && secondaryLabel != null) {
            ThreeButtonDialog(
                title = confirmation.title,
                message = confirmation.message,
                icon = confirmation.icon,
                primaryLabel = confirmation.confirmLabel ?: stringResource(CoreUiStrings.ok),
                onPrimary = { mainViewModel.executeConfirmableAction(confirmation.onConfirmAction) },
                secondaryLabel = secondaryLabel,
                onSecondary = { mainViewModel.executeConfirmableAction(secondaryAction) },
                onDismiss = { mainViewModel.dismissActionConfirmation() }
            )
        } else {
            OkCancelDialog(
                title = confirmation.title,
                message = confirmation.message,
                icon = confirmation.icon,
                onConfirm = { mainViewModel.executeConfirmableAction(confirmation.onConfirmAction) },
                onDismiss = { mainViewModel.dismissActionConfirmation() }
            )
        }
    }

    // Maintenance dialogs (sheets, confirmations, export chain)
    MaintenanceDialogs(
        maintenanceViewModel = maintenanceViewModel,
        showMaintenanceSheet = uiState.showMaintenanceSheet,
        onMaintenanceSheetDismiss = onMaintenanceSheetDismiss,
        onDirectoryClick = onDirectoryClick,
        onImportSettingsNavigate = onImportSettingsNavigate,
        onRecreateActivity = onRecreateActivity,
        onLaunchBrowser = onLaunchBrowser,
        onBringToForeground = onBringToForeground,
        onSnackbar = { snackbarHostState.showSnackbar(it) }
    )

    // About dialog
    if (uiState.showAboutDialog && aboutDialogData != null) {
        AboutAlertDialog(
            data = aboutDialogData,
            onDismiss = onAboutDialogDismiss,
            onOpenBatteryHelp = onOpenBatteryHelp
        )
    }
}
