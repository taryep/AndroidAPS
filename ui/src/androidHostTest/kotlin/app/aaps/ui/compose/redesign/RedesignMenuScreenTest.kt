package app.aaps.ui.compose.redesign

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
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

    @Before
    fun setUp() {
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
    }

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun show(
        showExit: Boolean = true,
        isTreatmentsEnabled: Boolean = true,
        attentionItems: List<RedesignAttentionItem> = emptyList(),
        onEditScreenClick: (() -> Unit)? = null
    ) {
        compose.setContent {
            MaterialTheme {
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
                    onEditScreenClick = onEditScreenClick
                )
            }
        }
    }

    private fun clickEntry(label: String) {
        compose.onNode(hasText(label) and hasClickAction(), useUnmergedTree = false).performScrollTo().performClick()
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

    @Test
    fun editScreenIsAShortcutWhenItCanBeUsed() {
        var clicks = 0
        show(onEditScreenClick = { clicks++ })
        clickEntry(string(R.string.redesign_edit_mode))
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun editScreenIsHiddenInSimpleMode() {
        // MainScreen passes no callback in simple mode, where the graphs are preset.
        show(onEditScreenClick = null)
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
