package app.aaps.ui.compose.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.LocalSnackbarHostState
import app.aaps.core.ui.compose.dialogs.OkCancelDialog
import app.aaps.core.ui.compose.dialogs.ThreeButtonDialog
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.compose.aboutDialog.AboutAlertDialog
import app.aaps.ui.compose.aboutDialog.AboutDialogData
import app.aaps.ui.compose.loopSheet.LoopActionBottomSheet
import app.aaps.ui.compose.loopSheet.LoopActionViewModel
import app.aaps.ui.compose.maintenance.ImportSource
import app.aaps.ui.compose.maintenance.MaintenanceDialogs
import app.aaps.ui.compose.maintenance.MaintenanceViewModel
import app.aaps.ui.compose.scenesSheet.ScenesBottomSheet
import app.aaps.ui.compose.scenesSheet.ScenesViewModel
import app.aaps.ui.compose.treatmentsSheet.TreatmentBottomSheet
import app.aaps.ui.compose.treatmentsSheet.TreatmentViewModel

/**
 * The sheets and dialogs of the main screen: treatments, automation, loop accept, the shared
 * confirmation dialog, maintenance and About. Used by [MainScreen] and by the redesign screen
 * (`RedesignMainScreen`), so both always show the same dialogs.
 *
 * @param showCarbs false hides the carbs entry of the treatment sheet, whatever its setting says
 *   (the redesign has a Carbs tile instead).
 */
@Composable
internal fun MainScreenSheets(
    mainViewModel: MainViewModel,
    uiState: MainUiState,
    aboutDialogData: AboutDialogData?,
    treatmentViewModel: TreatmentViewModel,
    scenesViewModel: ScenesViewModel,
    loopActionViewModel: LoopActionViewModel,
    maintenanceViewModel: MaintenanceViewModel,
    showTreatmentSheet: Boolean,
    onTreatmentSheetDismiss: () -> Unit,
    showAutomationSheet: Boolean,
    onAutomationSheetDismiss: () -> Unit,
    showLoopActionSheet: Boolean,
    onLoopActionSheetDismiss: () -> Unit,
    treatmentButtonsDef: PreferenceSubScreenDef,
    showCarbs: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    onAboutDialogDismiss: () -> Unit,
    onOpenBatteryHelp: (() -> Unit)?,
    onMaintenanceSheetDismiss: () -> Unit,
    onDirectoryClick: () -> Unit,
    onLaunchBrowser: (String) -> Unit,
    onBringToForeground: () -> Unit,
    onImportSettingsNavigate: (ImportSource) -> Unit,
    onRecreateActivity: () -> Unit
) {
    val snackbarHostState = LocalSnackbarHostState.current

    // Treatment bottom sheet
    if (showTreatmentSheet) {
        val treatmentState by treatmentViewModel.uiState.collectAsStateWithLifecycle()
        TreatmentBottomSheet(
            onDismiss = onTreatmentSheetDismiss,
            showCgm = treatmentState.showCgm,
            showCalibration = treatmentState.showCalibration,
            showTreatment = treatmentState.showTreatment,
            showInsulin = treatmentState.showInsulin,
            showCarbs = showCarbs && treatmentState.showCarbs,
            showCalculator = treatmentState.showCalculator,
            isDexcomSource = treatmentState.isDexcomSource,
            showSettingsIcon = treatmentState.showSettingsIcon,
            quickWizardItems = treatmentState.quickWizardItems,
            onNavigate = onNavigate,
            treatmentButtonsDef = treatmentButtonsDef,
        )
    }

    // Automation bottom sheet
    if (showAutomationSheet) {
        val automationState by scenesViewModel.uiState.collectAsStateWithLifecycle()
        ScenesBottomSheet(
            onDismiss = onAutomationSheetDismiss,
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
            onDismiss = onLoopActionSheetDismiss
        )
    }

    // Shared confirmation dialog (automation actions, TT presets, scene end — from toolbar or
    // bottom sheets). When the confirmation carries a secondary action (e.g., scene chain skip),
    // render a 3-button dialog; otherwise the standard 2-button OK/Cancel.
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
