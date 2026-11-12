package app.aaps.ui.compose.redesign

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TT
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.ui.R
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.main.TempTargetChipState
import app.aaps.ui.compose.overview.chips.CobUiState
import app.aaps.ui.compose.overview.chips.IobUiState
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 3: the six home tiles. Each tap does what the stock chip does, every state of SPEC 3.2 has a
 * picture in `ui/build/reports/redesign/tiles/`, and the longest French values fit without "â€¦".
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-rFR-w412dp-h891dp-xxhdpi")
class RedesignTilesTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
    }

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun tiles(
        isProfileModified: Boolean = false,
        tempTargetState: TempTargetChipState = TempTargetChipState.None,
        runningMode: RM.Mode? = RM.Mode.CLOSED_LOOP,
        runningModeRemaining: String = "",
        tbrState: TbrState = TbrState.NONE,
        iob: IobUiState = IobUiState(text = "0,00 U", iobTotal = 0.0),
        cob: CobUiState = CobUiState(text = "0 g"),
        data: RedesignHomeData = RedesignHomeData(profileName = "Normal", targetText = "120â€“130", basalRate = "0,85 U/h"),
        loopSuggestion: Boolean = false
    ) = RedesignTilesState(
        profileName = "Normal", isProfileModified = isProfileModified,
        tempTargetText = "120â€“130", tempTargetState = tempTargetState, tempTargetReason = TT.Reason.EATING_SOON,
        runningMode = runningMode, runningModeText = "Boucle fermÃ©e", runningModeRemaining = runningModeRemaining,
        tbrState = tbrState, iob = iob, cob = cob, data = data, loopSuggestion = loopSuggestion
    )

    private val requests = mutableListOf<NavigationRequest>()
    private var iobClicks = 0
    private var basalClicks = 0
    private var suggestionClicks = 0

    private class Shown(val state: RedesignTilesState, val commandsAllowed: Boolean, val dark: Boolean, val fontScale: Float)

    // A test may call setContent only once; later pictures change this state instead.
    private val shown = mutableStateOf<Shown?>(null)

    private fun show(state: RedesignTilesState, commandsAllowed: Boolean = true, dark: Boolean = true, fontScale: Float = 1f) {
        val first = shown.value == null
        shown.value = Shown(state, commandsAllowed, dark, fontScale)
        if (first) compose.setContent { shown.value?.let { Tiles(it) } }
        compose.waitForIdle()
    }

    @Composable
    private fun Tiles(shown: Shown) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, shown.fontScale)) {
            MaterialTheme(colorScheme = if (shown.dark) darkColorScheme() else lightColorScheme()) {
                val colors = redesignColors()
                Column(Modifier.width(412.dp).background(colors.pageBackground).padding(16.dp).testTag(TILES)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RedesignLoopSlot(shown.state, shown.commandsAllowed, { requests += it }, { suggestionClicks++ })
                        RedesignTargetSlot(shown.state, shown.commandsAllowed, { requests += it })
                    }
                    Spacer(Modifier.height(16.dp))
                    RedesignTiles(
                        state = shown.state,
                        commandsAllowed = shown.commandsAllowed,
                        onNavigate = { requests += it },
                        onIobClick = { iobClicks++ },
                        onBasalClick = { basalClicks++ }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    private fun picture(name: String) {
        val dir = File("build/reports/redesign/tiles").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag(TILES).captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(text: String) = compose.onNode(hasText(text, substring = true) and hasClickAction(), useUnmergedTree = false).performClick()

    /** How many times [text] is written on the tiles, counting every text node. */
    private fun timesShown(text: String): Int =
        compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }
            .sumOf { it.text.windowed(text.length).count { part -> part == text } }

    @Test
    fun carbsRequiredIsShownOnceAndGoesAwayWhenCarbsAreEnteredAfterTheLastLoopRun() {
        val required = RuntimeEnvironment.getApplication().getString(R.string.redesign_carbs_required, 13)
        assertThat(required).isEqualTo("13g requis")

        // Carbs entered before the last loop run: the loop still asks for 13 g.
        show(tiles(cob = redesignCob(cobText = "0 g", cobValue = 0.0, carbsReq = 13, lastApsRun = 2_000L, newestCarbsTime = 1_000L)))
        assertThat(timesShown(required)).isEqualTo(1)
        assertThat(timesShown("requis")).isEqualTo(1)

        // Carbs entered after the last loop run: as on the stock chip, nothing is required any more.
        show(tiles(cob = redesignCob(cobText = "0 g", cobValue = 0.0, carbsReq = 13, lastApsRun = 2_000L, newestCarbsTime = 3_000L)))
        assertThat(timesShown("requis")).isEqualTo(0)
    }

    @Test
    fun eachTileDoesWhatItsChipDoes() {
        show(tiles())
        tap(string(R.string.redesign_tile_profile))
        tap("120â€“130")
        compose.onNode(hasContentDescription(string(R.string.redesign_mode_closed), substring = true) and hasClickAction()).performClick()
        tap(string(R.string.redesign_tile_active_insulin))
        tap(string(R.string.redesign_tile_basal))
        tap(string(R.string.redesign_tile_carbs))
        assertThat(requests).containsExactly(
            NavigationRequest.Element(ElementType.PROFILE_MANAGEMENT),
            NavigationRequest.Element(ElementType.TEMP_TARGET_MANAGEMENT),
            NavigationRequest.Element(ElementType.RUNNING_MODE),
            // The Carbs tile opens carbs entry, as the Carbs button of the treatment sheet does.
            NavigationRequest.Element(ElementType.CARBS)
        ).inOrder()
        assertThat(iobClicks).isEqualTo(1)
        assertThat(basalClicks).isEqualTo(1)
    }

    @Test
    fun aWaitingLoopSuggestionOpensTheSheetToAcceptIt() {
        show(tiles(runningMode = RM.Mode.OPEN_LOOP, loopSuggestion = true))
        compose.onNode(hasContentDescription(string(R.string.redesign_mode_open), substring = true) and hasClickAction()).performClick()
        assertThat(suggestionClicks).isEqualTo(1)
        assertThat(requests).isEmpty()
        picture("loop_suggestion")
    }

    @Test
    fun withASuggestionWaitingALongPressChangesTheModeAndTheSlotShowsOnlyItsIcon() {
        // Step 7 (decided 12 Nov): tap = the sheet to accept the suggestion, long press = the running mode dialog.
        show(tiles(runningMode = RM.Mode.OPEN_LOOP, loopSuggestion = true))
        val slot = compose.onNode(hasContentDescription(string(R.string.redesign_mode_open), substring = true) and hasClickAction())
        slot.performTouchInput { longClick() }
        compose.waitForIdle()
        assertThat(requests).containsExactly(NavigationRequest.Element(ElementType.RUNNING_MODE))
        assertThat(suggestionClicks).isEqualTo(0)
        // Icon only (decided 9 Nov): the mode word is not written in the slot, only said by the screen reader.
        assertThat(compose.onAllNodes(hasText(string(R.string.redesign_mode_open)), useUnmergedTree = true).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun onAnUnpairedClientTheCommandTilesAreOff() {
        show(tiles(), commandsAllowed = false)
        compose.onNode(hasText("120â€“130", substring = true) and hasClickAction()).assertIsNotEnabled()
        compose.onNode(hasContentDescription(string(R.string.redesign_mode_closed), substring = true) and hasClickAction()).assertIsNotEnabled()
        // Carbs entry is blocked like the Treatments item: the tile has no action at all.
        assertThat(compose.onAllNodes(hasText(string(R.string.redesign_tile_carbs), substring = true) and hasClickAction()).fetchSemanticsNodes()).isEmpty()
        // Profile management opens read-only there, as with the stock chip.
        compose.onNode(hasText(string(R.string.redesign_tile_profile), substring = true) and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun picturesOfEveryRunningMode() {
        val modes = listOf(
            RM.Mode.CLOSED_LOOP to "", RM.Mode.OPEN_LOOP to "", RM.Mode.CLOSED_LOOP_LGS to "",
            RM.Mode.SUSPENDED_BY_USER to "25 min", RM.Mode.DISCONNECTED_PUMP to "30 min", RM.Mode.DISABLED_LOOP to "",
            RM.Mode.SUPER_BOLUS to "10 min"
        )
        for ((mode, remaining) in modes) {
            show(tiles(runningMode = mode, runningModeRemaining = remaining))
            picture("mode_${mode.name.lowercase()}")
        }
    }

    @Test
    fun picturesOfTheHighlightedStates() {
        val active = tiles(
            isProfileModified = true,
            tempTargetState = TempTargetChipState.Active,
            tbrState = TbrState.LOW,
            iob = IobUiState(text = "2,35 U", iobTotal = 2.35),
            cob = CobUiState(text = "24 g", cobValue = 24.0),
            data = RedesignHomeData(
                profileName = "Normal (90%)", profileTimeLeft = "9 min",
                targetText = "140", targetTimeLeft = "45 min",
                basalRate = "0,85 U/h", tempBasalRunning = true, tempBasalRate = "0,00 U/h", tempBasalPercent = "0%", tempBasalTimeLeft = "25 min"
            )
        )
        show(active)
        picture("highlighted_dark")
        show(active, dark = false)
        picture("highlighted_light")
        show(tiles(cob = CobUiState(text = "0 g", carbsReq = 13)))
        picture("carbs_required")
        show(tiles(tempTargetState = TempTargetChipState.Adjusted))
        picture("target_adjusted")
    }

    @Test
    fun insulinIsTintedOnlyFromOneUnit() {
        // Below the threshold, negative, and at it: only the last is tinted (checked on the pictures).
        show(tiles(iob = IobUiState(text = "0,95 U", iobTotal = 0.95)))
        picture("insulin_0_95")
        show(tiles(iob = IobUiState(text = "-0,40 U", iobTotal = -0.4)))
        picture("insulin_negative")
        show(tiles(iob = IobUiState(text = "1,00 U", iobTotal = 1.0)))
        picture("insulin_1_00")
        assertThat(REDESIGN_INSULIN_TINT_THRESHOLD_U).isEqualTo(1.0)
    }

    @Test
    fun theLongestValuesFitWithoutEllipsis() {
        // The worst cases of SPEC 3.2 at the default font: they may get smaller, never cut.
        show(worstCase())
        picture("worst_case")
        assertThat(cutTexts()).isEmpty()
    }

    @Test
    fun withTheLargestFontNothingOverlapsAndTheTilesShrink() {
        // At 200 % some text may end with "â€¦" (the SPEC allows it), but nothing may spill out of a tile.
        show(worstCase(), fontScale = 2f)
        picture("worst_case_font200")
        val report = File("build/reports/redesign/tiles").apply { mkdirs() }
        File(report, "worst_case_font200.txt").writeText(cutTexts().joinToString("\n").ifEmpty { "nothing cut" })
        assertThat(spilledTexts()).isEmpty()
    }

    private fun worstCase() = tiles(
        isProfileModified = true,
        runningMode = RM.Mode.DISCONNECTED_PUMP,
        runningModeRemaining = "30 min",
        tbrState = TbrState.HIGH,
        cob = CobUiState(text = "0 g", carbsReq = 13),
        data = RedesignHomeData(
            profileName = "Normal (90%)", profileTimeLeft = "9 min",
            targetText = "120â€“130", basalRate = "0,85 U/h",
            tempBasalRunning = true, tempBasalRate = "1,02 U/h", tempBasalPercent = "120%", tempBasalTimeLeft = "25 min"
        )
    )

    private fun layouts(): List<Pair<String, TextLayoutResult>> =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
                val shown = node.config.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty()
                results.map { shown to it }
            }

    private fun cutTexts(): List<String> = layouts().mapNotNull { (shown, result) ->
        if (result.lineCount > 0 && result.isLineEllipsized(result.lineCount - 1)) "\"$shown\" ends with an ellipsis" else null
    }

    private fun spilledTexts(): List<String> = layouts().mapNotNull { (shown, result) ->
        if (result.size.height < result.multiParagraph.height) "\"$shown\" is taller than its place" else null
    }

    private companion object {

        const val TILES = "tiles"
    }
}
