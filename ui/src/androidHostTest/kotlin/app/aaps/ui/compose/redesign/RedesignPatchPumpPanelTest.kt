package app.aaps.ui.compose.redesign

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import app.aaps.ui.R
import org.robolectric.RuntimeEnvironment
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.overview.statusLights.StatusItem
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Consumables of a patch pump (SPEC 3.5.1), with the values the status view model builds:
 * patch 2j17h (and 96 U used since the change, which must not show), reservoir 73 U (the insulin
 * change is 2j16h old, which must not show), sensor 2h.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-rFR-w412dp-h891dp-xxhdpi")
class RedesignPatchPumpPanelTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
    }

    private val patch = StatusItem(label = "Pompe Patch", age = "2j17h", ageMax = "3j", agePercent = 0.6f, level = "96 U", icon = Icons.Filled.Add, compactLevel = false)
    private val insulin = StatusItem(label = "Insuline", age = "2j16h", level = "73 U", levelValue = 73.0, reservoirReadingLimit = 200.0, icon = Icons.Filled.Add, compactAge = false)
    private val sensor = StatusItem(label = "Capteur", age = "2h", ageMax = "15j", agePercent = 0.1f, icon = Icons.Filled.Add, compactLevel = false)
    private val statusLightsDef = PreferenceSubScreenDef(key = "status_lights", title = CoreUiStrings.prime_fill, items = emptyList())

    private fun show(expanded: Boolean, pumpHasMenu: Boolean = true, onNavigate: (NavigationRequest) -> Unit = {}) {
        composeRule.setContent {
            MaterialTheme {
                RedesignConsumablesBar(
                    sensorStatus = sensor,
                    insulinStatus = insulin,
                    cannulaStatus = patch,
                    batteryStatus = null,
                    showFill = false, // as on the phone: a patch pump has no fill / change buttons here
                    showPumpBatteryChange = false,
                    commandsAllowed = true,
                    onNavigate = onNavigate,
                    statusLightsDef = statusLightsDef,
                    expanded = expanded,
                    onExpandedChange = {},
                    isPatchPump = true,
                    pumpHasMenu = pumpHasMenu
                )
            }
        }
    }

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun countInRow(rowTag: String, text: String, substring: Boolean = false) =
        composeRule.onAllNodes(hasAnyAncestor(hasTestTag(rowTag)) and hasText(text, substring = substring), useUnmergedTree = true)
            .fetchSemanticsNodes().size

    private fun countAnywhere(text: String) =
        composeRule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun reservoirBar_is_left_over_capacity() {
        assertThat(PATCH_RESERVOIR_MIN_CAPACITY_U).isEqualTo(200.0)
        assertThat(reservoirFraction(73.0, 200.0)!!).isWithin(1e-6f).of(0.365f)
        // A 300 U pump: the driver limit is the size.
        assertThat(reservoirFraction(150.0, 300.0)!!).isWithin(1e-6f).of(0.5f)
        // A pump with a 50 U display limit (Omnipod): the capacity is still 200 U, and at "50+" the level is not known.
        assertThat(reservoirFraction(30.0, 50.0)!!).isWithin(1e-6f).of(0.15f)
        assertThat(reservoirFraction(50.0, 50.0)).isNull()
        assertThat(reservoirFraction(250.0, 200.0)).isEqualTo(1f)
        assertThat(reservoirFraction(-3.0, 200.0)).isEqualTo(0f)
    }

    @Test
    fun openPanel_shows_one_value_per_row() {
        show(expanded = true)
        // Pompe Patch: time in use only, no U count.
        assertThat(countInRow(TAG_PATCH_ROW, "2j17h / 3j")).isEqualTo(1)
        assertThat(countInRow(TAG_PATCH_ROW, " U", substring = true)).isEqualTo(0)
        // Insuline: reservoir only, no age.
        assertThat(countInRow(TAG_INSULIN_ROW, "73 U")).isEqualTo(1)
        assertThat(countAnywhere("2j16h")).isEqualTo(0)
        // Capteur: time in use, and the round (+) instead of the "Ajouter" text button.
        assertThat(countInRow(TAG_SENSOR_ROW, "2h / 15j")).isEqualTo(1)
        composeRule.onNodeWithTag(TAG_ADD_SENSOR, useUnmergedTree = true).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        // No value shown twice.
        assertThat(countAnywhere("96 U")).isEqualTo(0)
        assertThat(countAnywhere("2j17h / 3j")).isEqualTo(1)
    }

    @Test
    fun patchRow_opens_the_pump_menu() {
        val requests = mutableListOf<NavigationRequest>()
        show(expanded = true, onNavigate = { requests += it })
        composeRule.onNodeWithTag(TAG_PATCH_ROW).performSemanticsAction(SemanticsActions.OnClick)
        assertThat(requests).containsExactly(NavigationRequest.Element(ElementType.PUMP))
    }

    @Test
    fun values_end_on_the_same_right_edge_and_patch_row_has_a_chevron() {
        show(expanded = true)
        fun right(text: String) = composeRule.onAllNodes(hasText(text), useUnmergedTree = true)
            .fetchSemanticsNodes().single().boundsInRoot.right
        val patchRight = right("2j17h / 3j")
        assertThat(right("73 U")).isWithin(0.5f).of(patchRight)
        assertThat(right("2h / 15j")).isWithin(0.5f).of(patchRight)
        // The label is not cut: the row has room for "Pompe Patch".
        val layouts = mutableListOf<TextLayoutResult>()
        val labelNode = composeRule.onAllNodes(hasText("Pompe Patch"), useUnmergedTree = true).fetchSemanticsNodes().single()
        labelNode.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val label = layouts.single()
        assertWithMessage("label width %s px, text needs %s px", label.size.width, label.multiParagraph.maxIntrinsicWidth)
            .that(label.isLineEllipsized(0)).isFalse()
        assertThat(
            composeRule.onAllNodes(hasContentDescription(string(R.string.redesign_open_pump_menu)), useUnmergedTree = true)
                .fetchSemanticsNodes().size
        ).isEqualTo(1)
    }

    @Test
    fun patchRow_is_not_clickable_without_a_pump_menu() {
        show(expanded = true, pumpHasMenu = false)
        composeRule.onNodeWithTag(TAG_PATCH_ROW).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun collapsedBar_shows_patch_time_then_reservoir_then_sensor_time() {
        show(expanded = false)
        fun left(text: String) = composeRule.onAllNodes(hasText(text), useUnmergedTree = true)
            .fetchSemanticsNodes().single().boundsInRoot.left
        val patchX = left("2j17h")
        val reservoirX = left("73 U")
        val sensorX = left("2h")
        assertThat(patchX).isLessThan(reservoirX)
        assertThat(reservoirX).isLessThan(sensorX)
        assertThat(countAnywhere("96 U")).isEqualTo(0)
        assertThat(countAnywhere("2j16h")).isEqualTo(0)
        // The maximum is shown in the open panel only.
        assertThat(countAnywhere("2j17h / 3j")).isEqualTo(0)
    }

    @Test
    fun chevron_and_plus_sit_right_after_the_name() {
        show(expanded = true)
        val label = composeRule.onAllNodes(hasText("Pompe Patch"), useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        val chevron = composeRule.onAllNodes(hasContentDescription(string(R.string.redesign_open_pump_menu)), useUnmergedTree = true)
            .fetchSemanticsNodes().single().boundsInRoot
        val value = composeRule.onAllNodes(hasText("2j17h / 3j"), useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        assertThat(chevron.left - label.right).isLessThan(24f * 3)   // a few dp at xxhdpi (3 px per dp)
        assertThat(chevron.right).isLessThan(value.left)
        val sensorLabel = composeRule.onAllNodes(hasText("Capteur"), useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        val plus = composeRule.onNodeWithTag(TAG_ADD_SENSOR, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertThat(plus.left - sensorLabel.right).isLessThan(24f * 3)
    }
}
