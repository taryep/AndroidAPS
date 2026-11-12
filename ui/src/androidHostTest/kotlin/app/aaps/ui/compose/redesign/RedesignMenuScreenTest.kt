package app.aaps.ui.compose.redesign

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.state.ToggleableState
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.plugin.PluginDescription
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.ui.R
import app.aaps.ui.UiStringIds
import app.aaps.ui.search.SearchUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import app.aaps.core.keys.R as KeysR
import app.aaps.core.ui.R as CoreUiR

/**
 * The redesign Menu screen: every entry does what the drawer entry does, and it behaves like a
 * screen (a pane for the screen reader, closed by Back).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h891dp")
class RedesignMenuScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val requests = mutableListOf<NavigationRequest>()
    private var closed = 0
    private var classic = 0

    @Before
    fun setUp() {
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
    }

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun show(
        showExit: Boolean = true,
        isTreatmentsEnabled: Boolean = true,
        attentionItems: List<RedesignAttentionItem> = emptyList(),
        fontScale: Float = 1f
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            MaterialTheme {
                Box(Modifier.requiredSize(412.dp, 891.dp)) {
                RedesignMenuScreen(
                    versionName = "4.0.0-test",
                    isSimpleMode = false,
                    isTreatmentsEnabled = isTreatmentsEnabled,
                    showExit = showExit,
                    searchUiState = SearchUiState(),
                    onSearchQueryChange = {},
                    onSearchClear = {},
                    onSearchActiveChange = {},
                    onSearchResultClick = {},
                    onSearchPluginToggle = {},
                    onNavigate = { requests += it },
                    onClose = { closed++ },
                    bottomPadding = 80.dp,
                    attentionItems = attentionItems,
                    onClassicInterface = { classic++ }
                )
                }
            }
            }
        }
    }

    private fun clickEntry(label: String) {
        compose.onNode(hasText(label) and hasClickAction(), useUnmergedTree = false).performScrollTo().performClick()
    }

    @Test
    fun theNewInterfaceRowAsksFirstThenGoesBackToTheClassicInterface() {
        show()
        val row = compose.onNode(hasTestTag(TAG_NEW_INTERFACE))
        row.performScrollTo()
        // The row is a switch that is on, with the title of the setting.
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        row.assert(hasText(string(KeysR.string.pref_title_redesign)))

        // Cancel: nothing changes.
        row.performClick()
        compose.onNodeWithText(string(R.string.redesign_classic_interface_message)).assertIsDisplayed()
        compose.onNodeWithText(string(CoreUiR.string.cancel)).performClick()
        assertThat(classic).isEqualTo(0)
        compose.onNodeWithText(string(R.string.redesign_classic_interface_message)).assertDoesNotExist()

        // OK: the classic interface is asked once.
        row.performClick()
        compose.onNodeWithText(string(CoreUiR.string.ok)).performClick()
        assertThat(classic).isEqualTo(1)
    }

    @Test
    fun everyEntrySendsTheSameRequestAsTheDrawer() {
        show()
        val entries = listOf(
            CoreUiR.string.settings to ElementType.SETTINGS,
            CoreUiR.string.nav_history_browser to ElementType.HISTORY_BROWSER,
            CoreUiR.string.statistics to ElementType.STATISTICS,
            CoreUiR.string.nav_about to ElementType.ABOUT,
            CoreUiR.string.nav_exit to ElementType.EXIT
        )
        for ((label, _) in entries) clickEntry(string(label))

        assertThat(requests).containsExactlyElementsIn(entries.map { NavigationRequest.Element(it.second) }).inOrder()
    }

    @Test
    fun theToolsListHasEveryOtherDrawerEntry() {
        show()
        compose.onNode(hasText(string(R.string.redesign_tools))).performScrollTo().assertIsDisplayed()
        // The drawer entries the shortcuts and the About card do not cover, in the drawer's order.
        val tools = listOf(
            CoreUiR.string.treatments_history to ElementType.TREATMENTS,
            CoreUiR.string.nav_profile_helper to ElementType.PROFILE_HELPER,
            CoreUiR.string.maintenance to ElementType.MAINTENANCE,
            CoreUiR.string.nav_setupwizard to ElementType.SETUP_WIZARD,
            CoreUiR.string.nav_configuration to ElementType.CONFIGURATION
        )
        for ((label, _) in tools) clickEntry(string(label))

        assertThat(requests).containsExactlyElementsIn(tools.map { NavigationRequest.Element(it.second) }).inOrder()
    }

    @Test
    fun itemsThatNeedTheUserAreShownFirstWithTheirBadgeAndItsMeaning() {
        var bgClicks = 0
        var loopClicks = 0
        val actionNeeded = string(R.string.a11y_badge_attention)
        val oneAvailable = RuntimeEnvironment.getApplication().getString(R.string.a11y_badge_available, 1)
        show(
            attentionItems = listOf(
                RedesignAttentionItem("xDrip+", Icons.Default.Settings, actionNeeded, { Badge { Text("!") } }) { bgClicks++ },
                RedesignAttentionItem("Accept", Icons.Default.Settings, oneAvailable, { Badge { Text("1") } }) { loopClicks++ }
            )
        )

        compose.onNode(hasText(string(R.string.redesign_needs_attention))).assertIsDisplayed()
        compose.onNode(hasText("xDrip+") and hasStateDescription(actionNeeded)).performClick()
        compose.onNode(hasText("Accept") and hasStateDescription(oneAvailable)).performClick()
        // The badge each item had in the bar is drawn in its row.
        compose.onNode(hasText("1")).assertIsDisplayed()
        assertThat(bgClicks).isEqualTo(1)
        assertThat(loopClicks).isEqualTo(1)
    }

    @Test
    fun theItemsLeavingTheBarAreObjectivesFirstThenTheMostUrgent() {
        var items = emptyList<RedesignAttentionItem>()
        compose.setContent {
            MaterialTheme {
                items = redesignAttentionItems(
                    objectivesSetupPlugin = plugin("Objectives"),
                    objectivesProgressText = "9/10",
                    objectivesProgressDescription = "9 of 10 objectives done",
                    pumpSetupPlugin = plugin("Omnipod DASH"),
                    permissionsMissing = true,
                    onNavigate = {},
                    onPermissionsClick = {}
                )
            }
        }
        compose.waitForIdle()
        // The BG source warning is beside the glucose circle and the loop suggestion is the Loop tile: neither is here.
        assertThat(items.map { it.label }).containsExactly("Objectives", "Omnipod DASH", string(R.string.permission_sheet_title)).inOrder()
        assertThat(items.first().stateDescription).isEqualTo("9 of 10 objectives done")
        // Objectives is a progress, so it does not light the dot on the Menu item; the other two do.
        assertThat(items.map { it.needsAction }).containsExactly(false, true, true).inOrder()
    }

    @Test
    fun withNothingToDoTheSectionIsEmpty() {
        var items = listOf<RedesignAttentionItem>()
        compose.setContent {
            MaterialTheme {
                items = redesignAttentionItems(null, null, null, null, false, {}, {})
            }
        }
        compose.waitForIdle()
        assertThat(items).isEmpty()
    }


    private fun plugin(name: String): PluginBase =
        mock<PluginBase>().also {
            whenever(it.pluginDescription).thenReturn(PluginDescription().icon(Icons.Default.Settings))
            whenever(it.name).thenReturn(name)
        }


    /** One line of text? A label cut inside a word would have more lines. */
    private fun lineCount(label: String): Int {
        val node = compose.onNode(hasText(label), useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().lineCount
    }

    private fun tileBounds(label: String) = compose.onNode(hasText(label) and hasClickAction(), useUnmergedTree = false).fetchSemanticsNode().boundsInRoot

    private val shortcutLabels get() = listOf(CoreUiR.string.settings, CoreUiR.string.nav_history_browser, CoreUiR.string.statistics).map { string(it) }

    private fun assertOneLineAndEqualTiles(fontScale: Float) {
        show(fontScale = fontScale)
        shortcutLabels.forEach { assertThat(lineCount(it)).isEqualTo(1) }
        assertThat(shortcutLabels.map { tileBounds(it).height }.toSet().size).isEqualTo(1)
    }

    @Test
    fun shortcutLabelsAreOneLineAndTilesEquallyHighAtNormalFont() = assertOneLineAndEqualTiles(1f)

    @Test
    fun shortcutLabelsAreOneLineAndTilesEquallyHighAtALargeFont() = assertOneLineAndEqualTiles(1.5f)

    @Test
    fun shortcutLabelsAreOneLineAndTilesEquallyHighAtTheLargestFont() = assertOneLineAndEqualTiles(2f)

    @Test
    fun shortcutsGoToTwoColumnsWhenThreeDoNotFit() {
        // At twice the font size the labels do not fit a third of the width, even at 11 sp.
        show(fontScale = 2f)
        val labels = listOf(CoreUiR.string.settings, CoreUiR.string.nav_history_browser, CoreUiR.string.statistics).map { string(it) }
        val tiles = labels.map { tileBounds(it) }
        assertThat(tiles[1].top).isEqualTo(tiles[0].top)
        assertThat(tiles[2].top).isGreaterThan(tiles[0].top)
    }

    @Test
    fun threeShortcutsShareOneRowAtNormalFont() {
        show()
        val tops = shortcutLabels.map { tileBounds(it).top }
        assertThat(tops.toSet().size).isEqualTo(1)
    }

    @Test
    fun editModeIsNotInTheMenu() {
        // The edit mode starts from the pencil button of the graphs page (phase 8a).
        show()
        assertThat(compose.onAllNodes(hasText(string(R.string.redesign_edit_mode))).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun withNothingToSetUpThereIsNoSection() {
        show()
        assertThat(compose.onAllNodes(hasText(string(R.string.redesign_needs_attention))).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun exitIsHiddenWhereTheAppMayNotCloseItself() {
        show(showExit = false)
        compose.onAllNodes(hasText(string(CoreUiR.string.nav_exit))).fetchSemanticsNodes().let { assertThat(it).isEmpty() }
    }

    @Test
    fun treatmentsNeedALoadedProfileLikeInTheDrawer() {
        show(isTreatmentsEnabled = false)
        compose.onNode(hasText(string(CoreUiR.string.treatments_history)) and hasClickAction()).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun theScreenIsAnnouncedAsAPane() {
        show()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, string(R.string.redesign_menu))).assertExists()
    }

    @Test
    fun backClosesTheMenu() {
        show()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertThat(closed).isEqualTo(1)
    }
}
