package app.aaps.ui.compose.redesign

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.aaps.core.data.format.NumberFormat
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.interfaces.ClientRole
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.LocalConfig
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.ui.R
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.insulinDialog.InsulinDialogContent
import app.aaps.ui.compose.insulinDialog.InsulinDialogUiState
import app.aaps.ui.compose.overview.chips.CobUiState
import app.aaps.ui.compose.overview.chips.IobUiState
import app.aaps.ui.compose.overview.graphs.BgInfoUiState
import app.aaps.ui.compose.tempBasalDialog.TempBasalDialogContent
import app.aaps.ui.compose.tempBasalDialog.TempBasalDialogUiState
import app.aaps.ui.compose.treatmentsSheet.TreatmentSelectionContent
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import app.aaps.core.interfaces.configuration.Config as AppConfig
import app.aaps.core.ui.R as CoreUiR

/**
 * Phase 6 (SPEC 3.2.2 to 3.2.4): what the Active insulin and Basal tiles open, when they only show the
 * stock dialog, the lines on top of the two screens, and the Treatments sheet without carbs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-rFR")
class RedesignTileActionsTest {

    @get:Rule
    val compose = createComposeRule()

    // The redesign strings are read through the "ui" id table; without it they show as "[name]".
    @Before
    fun setUp() {
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
    }

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    // --- Tile taps ---

    private val requests = mutableListOf<NavigationRequest>()
    private var iobInfo = 0
    private var tbrInfo = 0

    private fun tapBoth(insulinAllowed: Boolean, tempBasalAllowed: Boolean) {
        val taps = redesignTileTaps(insulinAllowed, tempBasalAllowed, { requests += it }, { iobInfo++ }, { tbrInfo++ })
        taps.onIobClick()
        taps.onBasalClick()
    }

    @Test
    fun allowedTilesOpenTheScreensThroughTheProtectedNavigation() {
        tapBoth(insulinAllowed = true, tempBasalAllowed = true)
        assertThat(requests).containsExactly(
            NavigationRequest.Element(ElementType.INSULIN),
            NavigationRequest.Element(ElementType.TEMP_BASAL)
        ).inOrder()
        assertThat(iobInfo).isEqualTo(0)
        assertThat(tbrInfo).isEqualTo(0)
    }

    @Test
    fun blockedTilesShowTheStockDialogs() {
        tapBoth(insulinAllowed = false, tempBasalAllowed = false)
        assertThat(requests).isEmpty()
        assertThat(iobInfo).isEqualTo(1)
        assertThat(tbrInfo).isEqualTo(1)
    }

    @Test
    fun aMasterMayOpenBoth() {
        assertThat(redesignTileRules(masterOrPairedClient = true, role = ClientRole.Admin, tempBasalOffered = true)).isEqualTo(true to true)
    }

    @Test
    fun anUnpairedClientMayOpenNeither() {
        assertThat(redesignTileRules(masterOrPairedClient = false, role = ClientRole.Admin, tempBasalOffered = true)).isEqualTo(false to false)
    }

    @Test
    fun theRoleDecidesLikeTheStockSheets() {
        // A meal assistant may bolus but not set a temp basal; read only may do neither.
        assertThat(redesignTileRules(masterOrPairedClient = true, role = ClientRole.MealAssistant, tempBasalOffered = true)).isEqualTo(true to false)
        assertThat(redesignTileRules(masterOrPairedClient = true, role = ClientRole.ReadOnly, tempBasalOffered = true)).isEqualTo(false to false)
    }

    @Test
    fun aPumpThatCannotSetATempBasalNowKeepsTheInfoDialog() {
        // Suspended, disconnected, not initialized or not able to: Manage offers neither the editor nor Cancel.
        assertThat(redesignTileRules(masterOrPairedClient = true, role = ClientRole.Admin, tempBasalOffered = false)).isEqualTo(true to false)
    }

    // --- Lines on top of the screens ---

    @Test
    fun iobLineWithANegativeBasalPart() {
        compose.setContent { MaterialTheme { RedesignIobLineContent(RedesignIobParts(bolus = 1.5, basal = -1.3)) } }
        compose.onNodeWithText("Bolus 1,50 U + Basale -1,30 U = 0,20 U").assertIsDisplayed()
    }

    @Test
    fun tbrLineWhileATempBasalRunsHasTheCancelButton() {
        var cancels = 0
        val line = RuntimeEnvironment.getApplication().getString(R.string.redesign_tbr_line, "0,00 U/h", "15:58", 5, 60)
        assertThat(line).isEqualTo("0,00 U/h @15:58 5/60 min")
        val data = RedesignHomeData(basalRate = "0,85 U/h", tempBasalRunning = true, tempBasalLine = line)
        compose.setContent { MaterialTheme { RedesignTbrLineContent(data, showCancel = true, onCancelClick = { cancels++ }) } }
        compose.onNodeWithText(line).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.redesign_cancel_temp_basal_description)).performClick()
        assertThat(cancels).isEqualTo(1)
    }

    @Test
    fun tbrLineWithoutATempBasalShowsTheProfileBasalAndNoCancel() {
        val data = RedesignHomeData(basalRate = "0,85 U/h")
        compose.setContent { MaterialTheme { RedesignTbrLineContent(data, showCancel = false, onCancelClick = {}) } }
        compose.onNodeWithText("Basale du profil 0,85 U/h").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.redesign_cancel_temp_basal_description)).assertDoesNotExist()
    }

    @Test
    fun insulinScreenShowsTopInfo() {
        compose.setContent {
            MaterialTheme {
                InsulinDialogContent(
                    uiState = InsulinDialogUiState(),
                    bgInfo = BgInfoUiState(bgInfo = null, timeAgoText = ""),
                    iob = IobUiState(),
                    cob = CobUiState(),
                    dateString = "2024-01-01",
                    timeString = "12:00",
                    bolusFormat = NumberFormat.DECIMAL_1,
                    formatAmount = { it.toString() },
                    onEatingSoonChange = {},
                    onRecordOnlyChange = {},
                    onInsulinChange = {},
                    onAddInsulin = {},
                    onTimeOffsetChange = {},
                    onNotesChange = {},
                    onDateClick = {},
                    onTimeClick = {},
                    onInsulinTypeSelect = {},
                    onSettingsClick = null,
                    onNavigateBack = {},
                    onConfirmClick = {},
                    topInfo = { Text(TOP_INFO) }
                )
            }
        }
        compose.onNodeWithText(TOP_INFO).assertIsDisplayed()
    }

    @Test
    fun tempBasalScreenShowsTopInfo() {
        compose.setContent {
            MaterialTheme {
                TempBasalDialogContent(
                    uiState = TempBasalDialogUiState(),
                    onBasalPercentChange = {},
                    onBasalAbsoluteChange = {},
                    onDurationChange = {},
                    onNavigateBack = {},
                    onConfirmClick = {},
                    topInfo = { Text(TOP_INFO) }
                )
            }
        }
        compose.onNodeWithText(TOP_INFO).assertIsDisplayed()
    }

    @Test
    fun valuesAreColouredWhereTheyAreAndTheTextIsKept() {
        val text = "Bolus 0,50 U + Basale 0,50 U = 1,00 U"
        val result = coloredValues(text, listOf("0,50 U" to Color.Red, "0,50 U" to Color.Blue, "1,00 U" to Color.Green, "absent" to Color.Black))
        assertThat(result.text).isEqualTo(text)
        // Two equal values get one place each, in the order found.
        assertThat(result.spanStyles.map { it.item.color to text.substring(it.start, it.end) }).containsExactly(
            Color.Red to "0,50 U", Color.Blue to "0,50 U", Color.Green to "1,00 U"
        )
        assertThat(result.spanStyles.map { it.start }).containsExactly(6, 22, 31)
    }

    // --- Treatments sheet ---

    @Test
    fun theGearOfTheSheetHasNoCarbsSwitch() {
        val def = PreferenceSubScreenDef(
            key = "treatment_button_settings",
            title = CoreUiStrings.treatments,
            items = listOf(BooleanKey.OverviewShowInsulinButton, BooleanKey.OverviewShowCarbsButton, BooleanKey.OverviewShowWizardButton)
        )
        assertThat(def.withoutCarbsButton().items).containsExactly(BooleanKey.OverviewShowInsulinButton, BooleanKey.OverviewShowWizardButton).inOrder()
    }

    @Test
    fun treatmentsSheetWithoutCarbsKeepsInsulin() {
        compose.setContent {
            CompositionLocalProvider(LocalConfig provides mock<AppConfig>()) {
                MaterialTheme {
                    TreatmentSelectionContent(
                        onDismiss = {},
                        onNavigate = {},
                        quickWizardItems = emptyList(),
                        showCgm = true,
                        showCalibration = true,
                        showTreatment = true,
                        showInsulin = true,
                        showCarbs = false,
                        showCalculator = true,
                        isDexcomSource = false,
                        showSettingsIcon = true,
                        onSettingsClick = {}
                    )
                }
            }
        }
        compose.onNodeWithText(string(CoreUiR.string.treatment_insulin_desc)).assertExists()
        compose.onNodeWithText(string(CoreUiR.string.treatment_carbs_desc)).assertDoesNotExist()
    }

    private companion object {

        const val TOP_INFO = "top info"
    }
}
