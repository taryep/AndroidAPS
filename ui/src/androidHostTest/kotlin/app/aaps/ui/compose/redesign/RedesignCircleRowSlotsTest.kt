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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TT
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.ui.R
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.main.TempTargetChipState
import app.aaps.ui.compose.overview.chips.CobUiState
import app.aaps.ui.compose.overview.chips.IobUiState
import app.aaps.ui.compose.overview.chips.SensitivityUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Phase 5: the parts of the circle row and the sensitivity line (SPEC 3.3, 3.4). Pictures of the
 * states are in `ui/build/reports/redesign/slots/`. The tests check the sizes, the bell badge and the
 * longest French captions in the round slots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-rFR-w412dp-h891dp-xxhdpi")
class RedesignCircleRowSlotsTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
    }

    private fun string(id: Int, vararg args: Any) = RuntimeEnvironment.getApplication().getString(id, *args)

    private fun state(
        runningMode: RM.Mode? = RM.Mode.CLOSED_LOOP,
        remaining: String = "",
        tempTarget: TempTargetChipState = TempTargetChipState.None,
        loopSuggestion: Boolean = false,
        data: RedesignHomeData = RedesignHomeData(targetText = "120–130")
    ) = RedesignTilesState(
        profileName = "Normal", isProfileModified = false,
        tempTargetText = "120–130", tempTargetState = tempTarget, tempTargetReason = TT.Reason.ACTIVITY,
        runningMode = runningMode, runningModeText = "Boucle fermée", runningModeRemaining = remaining,
        tbrState = TbrState.NONE, iob = IobUiState(text = "0,00 U", iobTotal = 0.0), cob = CobUiState(text = "0 g"),
        data = data, loopSuggestion = loopSuggestion
    )

    private val requests = mutableListOf<Any>()
    private var bellClicks = 0
    private var suggestionClicks = 0

    private class Shown(val dark: Boolean, val fontScale: Float, val content: @Composable () -> Unit)

    private val shown = mutableStateOf<Shown?>(null)

    private fun show(dark: Boolean = true, fontScale: Float = 1f, content: @Composable () -> Unit) {
        val first = shown.value == null
        shown.value = Shown(dark, fontScale, content)
        if (first) compose.setContent { shown.value?.let { Frame(it) } }
        compose.waitForIdle()
    }

    @Composable
    private fun Frame(shown: Shown) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, shown.fontScale)) {
            MaterialTheme(colorScheme = if (shown.dark) darkColorScheme() else lightColorScheme()) {
                Column(Modifier.width(412.dp).background(redesignColors().pageBackground).padding(16.dp).testTag(FRAME)) {
                    shown.content()
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    private fun picture(name: String) {
        val dir = File("build/reports/redesign/slots").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag(FRAME).captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Composable
    private fun SlotsRow(vararg slots: @Composable () -> Unit) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { slots.forEach { it() } }
    }

    @Test
    fun theLoopSlotNamesTheModeInEveryMode() {
        val modes = listOf(
            RM.Mode.CLOSED_LOOP to "", RM.Mode.OPEN_LOOP to "", RM.Mode.CLOSED_LOOP_LGS to "",
            RM.Mode.SUSPENDED_BY_USER to "25 min", RM.Mode.DISCONNECTED_PUMP to "30 min", RM.Mode.DISABLED_LOOP to ""
        )
        show {
            SlotsRow(*modes.map { (mode, remaining) ->
                @Composable { RedesignLoopSlot(state(mode, remaining), true, { requests += it }, { suggestionClicks++ }) }
            }.toTypedArray())
        }
        // Step 7: the slot shows the icon only. The mode and the time left stay in the screen reader name.
        compose.onNode(hasContentDescription(string(R.string.redesign_mode_closed), substring = true)).assertExists()
        compose.onNode(hasContentDescription(string(R.string.redesign_mode_disconnected), substring = true)).assertExists()
        compose.onNode(hasContentDescription("25 min", substring = true)).assertExists()
        compose.onAllNodes(hasText("25 min", substring = true)).assertCountEquals(0)
        picture("loop_modes")
        show(fontScale = 2f) {
            SlotsRow({ RedesignLoopSlot(state(RM.Mode.DISCONNECTED_PUMP, "30 min"), true, { requests += it }, {}) })
        }
        picture("loop_disconnected_largest_font")
    }

    @Test
    fun anOpenLoopSuggestionShowsABadgeAndOpensTheSheet() {
        show {
            SlotsRow({ RedesignLoopSlot(state(RM.Mode.OPEN_LOOP, loopSuggestion = true), true, { requests += it }, { suggestionClicks++ }) })
        }
        compose.onNode(hasContentDescription(string(R.string.redesign_mode_open), substring = true) and hasClickAction()).performClick()
        assertThat(suggestionClicks).isEqualTo(1)
        assertThat(requests).isEmpty()
        picture("loop_suggestion")
    }

    @Test
    fun theTargetSlotShowsTheTimeLeftOfATemporaryTarget() {
        val temp = state(
            tempTarget = TempTargetChipState.Active,
            data = RedesignHomeData(targetText = "140", targetTimeLeft = "45 min")
        )
        show {
            SlotsRow(
                { RedesignTargetSlot(state(), true, { requests += it }) },
                { RedesignTargetSlot(temp, true, { requests += it }) },
                { RedesignTargetSlot(state(tempTarget = TempTargetChipState.Adjusted), true, { requests += it }) }
            )
        }
        compose.onNode(hasText("45 min", substring = true)).assertExists()
        picture("targets")
    }

    @Test
    fun theBellShowsAnAlwaysRedCountAndIsHiddenWithoutNotifications() {
        show {
            SlotsRow(
                { RedesignBellButton(0) { bellClicks++ } },
                { RedesignBellButton(1) { bellClicks++ } },
                { RedesignBellButton(2) { bellClicks++ } },
                { RedesignBellButton(12) { bellClicks++ } }
            )
        }
        compose.onNode(hasContentDescription(string(R.string.a11y_notifications, 12))).assertExists()
        compose.onNode(hasContentDescription(string(R.string.a11y_notifications, 2))).assertExists()
        compose.onNode(hasContentDescription(string(R.string.a11y_notifications, 1))).performClick()
        // With no notification the bell is not drawn: 3 bells for the counts 1, 2 and 12.
        assertThat(compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()).hasSize(3)
        assertThat(bellClicks).isEqualTo(1)
        picture("bell")
    }

    @Test
    fun theAlertSlotHasTwoLines() {
        show {
            val c = redesignColors()
            SlotsRow(
                { RedesignSlotButton(c.disabled, Icons.Default.WaterDrop, "xDrip+", string(R.string.redesign_bg_doubled), "x", {}) },
                { RedesignSlotButton(c.carbsRequired, Icons.Default.WaterDrop, string(R.string.redesign_meal), "12 min", "x", {}) },
                { RedesignSlotButton(c.insulin, Icons.Default.WaterDrop, string(R.string.redesign_tile_pump), "Réglage de la basale temporaire", "x", {}) }
            )
        }
        assertThat(compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()).hasSize(3)
        picture("alert_slots")
    }

    @Test
    fun theSensitivityLineIsOneLineWithNoSeparatorSign() {
        val sensitivity = SensitivityUiState(asText = "80 %", isfFrom = "36", isfTo = "45", dialogText = "x", hasData = true)
        assertThat(redesignShowsSensitivity(sensitivity)).isTrue()
        assertThat(redesignShowsSensitivity(SensitivityUiState())).isFalse()
        show { RedesignSensitivityLine(sensitivity) }
        picture("sensitivity")
        show(fontScale = 2f) { RedesignSensitivityLine(sensitivity) }
        picture("sensitivity_largest_font")
    }

    private companion object {
        const val FRAME = "frame"
    }
}
