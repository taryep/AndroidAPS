package app.aaps.ui.compose.redesign

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import app.aaps.core.data.model.RM
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.ui.R
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.main.TempTargetChipState
import app.aaps.ui.compose.overview.OverviewScreen
import app.aaps.ui.compose.redesign.RedesignOverviewState.Companion.GRAPHS_PAGE
import app.aaps.ui.compose.redesign.RedesignOverviewState.Companion.HOME_PAGE
import app.aaps.ui.compose.testing.AapsScreenFixture
import app.aaps.ui.compose.testing.OverviewViewModelFixture
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import app.aaps.core.ui.R as CoreUiR

/**
 * Phase 2: the phone overview on two pages (home, graphs) and the edit mode of the graphs page,
 * drawn through the real `OverviewScreen` with real view models.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h891dp-xxhdpi")
class RedesignOverviewContentTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var screen: AapsScreenFixture
    private lateinit var viewModels: OverviewViewModelFixture
    private val state = RedesignOverviewState()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        TextRefIdRegistry.register("ui") { name -> UiStringIds.idOf(name) }
        screen = AapsScreenFixture()
        viewModels = OverviewViewModelFixture(screen)
        viewModels.withBg()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)
    private val addGraph get() = string(CoreUiR.string.graph_add)
    private val editModeLabel get() = string(R.string.redesign_edit_mode)

    private fun show(isSimpleMode: Boolean = false) {
        val status = viewModels.awaitStatusItems()
        compose.setContent {
            MaterialTheme {
                screen.Content {
                    Box(Modifier.requiredSize(412.dp, 891.dp).testTag(SCREEN)) {
                        OverviewScreen(
                            profileName = "Normal", isProfileModified = false, profileProgress = 0f,
                            tempTargetText = "120â€“130", tempTargetState = TempTargetChipState.None,
                            tempTargetProgress = 0f, tempTargetReason = null,
                            runningMode = RM.Mode.CLOSED_LOOP, runningModeText = "Closed loop",
                            runningModeRemaining = "", runningModeProgress = 0f,
                            tbrState = TbrState.NONE, smbEnabled = true, isSimpleMode = isSimpleMode, calcProgress = 100,
                            graphViewModel = viewModels.graphViewModel, chipsViewModel = viewModels.chipsViewModel,
                            manageViewModel = viewModels.manageViewModel, statusViewModel = status,
                            statusLightsDef = PreferenceSubScreenDef(key = "statuslights", titleResId = CoreUiR.string.statuslights),
                            onNavigate = {}, onTbrChipClick = {}, onIobChipClick = {},
                            notifications = emptyList(), onDismissNotification = {}, onNotificationActionClick = {},
                            autoShowNotificationSheet = false, onAutoShowConsumed = {},
                            paddingValues = PaddingValues(top = 32.dp, bottom = 104.dp),
                            redesign = state
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun pane(name: Int) = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, string(name)))

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun theAppStartsOnTheHomePageWithoutGraphs() {
        show()
        pane(R.string.redesign_page_home).assertIsDisplayed()
        pane(R.string.redesign_page_graphs).assertIsNotDisplayed()
    }

    @Test
    fun aTapOnTheGlucoseValueStaysOnHome() {
        // Step 2 (decided 10 Nov): nothing on Home opens the graphs page; only the bottom bar does.
        show()
        compose.onAllNodesWithTag(REDESIGN_BG_CIRCLE_TAG).onFirst().performClick()
        compose.waitForIdle()

        assertThat(state.page).isNotEqualTo(GRAPHS_PAGE)
        pane(R.string.redesign_page_home).assertIsDisplayed()
    }

    @Test
    fun aSwipeDoesNotChangeThePage() {
        show()
        // Only buttons change the page (SPEC 4b): the swipe that used to open the graphs does nothing.
        compose.onNodeWithTag(SCREEN).performTouchInput { swipeLeft(startX = right - 10f, endX = left + 10f) }
        compose.waitForIdle()
        assertThat(state.page).isEqualTo(HOME_PAGE)
    }

    @Test
    fun aSidewaysDragOnTheGlucoseGraphScrollsTheGraphNotThePage() {
        state.page = GRAPHS_PAGE
        show()
        // The glucose graph is the first block of the graphs page, under the 32 dp top padding.
        compose.onNodeWithTag(SCREEN).performTouchInput {
            val y = 150.dp.toPx()
            swipe(start = Offset(centerX - 100f, y), end = Offset(centerX + 300f, y), durationMillis = 300)
        }
        compose.waitForIdle()
        assertThat(state.page).isEqualTo(GRAPHS_PAGE)
    }

    @Test
    fun theEditButtonsShowOnlyInEditMode() {
        state.page = GRAPHS_PAGE
        show()
        // Step 9: "add a graph" is the (+) icon of the pill, so it is found by its screen reader name.
        assertThat(compose.onAllNodes(hasContentDescription(addGraph)).fetchSemanticsNodes()).isEmpty()
        assertThat(compose.onAllNodes(hasText(editModeLabel)).fetchSemanticsNodes()).isEmpty()

        compose.runOnUiThread { state.editScreen() }
        compose.waitForIdle()
        compose.onNode(hasText(editModeLabel)).assertIsDisplayed()
        assertThat(compose.onAllNodes(hasContentDescription(addGraph)).fetchSemanticsNodes()).isNotEmpty()
    }

    @Test
    fun editScreenFromTheHomePageOpensTheGraphsInEditMode() {
        show()
        compose.runOnUiThread { state.editScreen() }
        compose.waitForIdle()
        assertThat(state.page).isEqualTo(GRAPHS_PAGE)
        assertThat(state.editMode).isTrue()
        compose.onNode(hasText(editModeLabel)).assertIsDisplayed()
    }

    @Test
    fun doneLeavesTheEditMode() {
        show()
        compose.runOnUiThread { state.editScreen() }
        compose.waitForIdle()
        compose.onNode(hasContentDescription(string(R.string.redesign_edit_done))).performClick()
        compose.waitForIdle()
        assertThat(state.editMode).isFalse()
        assertThat(compose.onAllNodes(hasContentDescription(addGraph)).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun goingBackToTheHomePageLeavesTheEditMode() {
        show()
        compose.runOnUiThread { state.editScreen() }
        compose.waitForIdle()
        compose.runOnUiThread { state.page = HOME_PAGE }
        compose.waitForIdle()
        assertThat(state.editMode).isFalse()
    }

    @Test
    fun backLeavesTheEditModeThenGoesHome() {
        show()
        compose.runOnUiThread { state.editScreen() }
        compose.waitForIdle()

        pressBack()
        assertThat(state.editMode).isFalse()
        assertThat(state.page).isEqualTo(GRAPHS_PAGE)

        pressBack()
        assertThat(state.page).isEqualTo(HOME_PAGE)
    }

    @Test
    fun backIsLeftToTheMenuWhileItIsOpen() {
        state.page = GRAPHS_PAGE
        state.menuOpen = true
        show()
        pressBack()
        assertThat(state.page).isEqualTo(GRAPHS_PAGE)
    }

    @Test
    fun theEditModeEndsWhenTheAppGoesToTheBackground() {
        show()
        compose.runOnUiThread { state.editScreen() }
        compose.waitForIdle()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.waitForIdle()
        assertThat(state.editMode).isFalse()
    }

    @Test
    fun theTitleRowHasAnEditButtonThatStartsTheEditMode() {
        state.page = GRAPHS_PAGE
        show()
        compose.onNode(hasText(string(R.string.redesign_graphs_title))).assertIsDisplayed()
        compose.onNode(hasContentDescription(string(R.string.redesign_edit_graphs))).performClick()
        compose.waitForIdle()
        assertThat(state.editMode).isTrue()
        compose.onNode(hasText(editModeLabel)).assertIsDisplayed()
    }

    @Test
    fun aRetapOnMenuOrGraphsGoesHomeAndKeepsNoEditMode() {
        state.editScreen()
        state.toggleMenu()
        assertThat(state.menuOpen).isTrue()
        assertThat(state.page).isEqualTo(GRAPHS_PAGE)
        assertThat(state.editMode).isTrue()

        state.toggleMenu()
        assertThat(state.menuOpen).isFalse()
        assertThat(state.page).isEqualTo(HOME_PAGE)
        assertThat(state.editMode).isFalse()

        state.toggleGraphs()
        assertThat(state.page).isEqualTo(GRAPHS_PAGE)
        state.toggleGraphs()
        assertThat(state.page).isEqualTo(HOME_PAGE)
    }

    @Test
    fun graphsKeepTheirPlaceWhileTheMenuIsOpen() {
        state.page = GRAPHS_PAGE
        state.toggleMenu()
        state.menuOpen = false // Back from the Menu: the previous screen
        assertThat(state.page).isEqualTo(GRAPHS_PAGE)
    }

    @Test
    fun simpleModeHasNoTitleEditButton() {
        state.page = GRAPHS_PAGE
        show(isSimpleMode = true)
        assertThat(compose.onAllNodes(hasContentDescription(string(R.string.redesign_edit_graphs))).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun simpleModeHasNoEditButtonsEvenInEditMode() {
        state.editScreen()
        show(isSimpleMode = true)
        assertThat(compose.onAllNodes(hasContentDescription(addGraph)).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    fun aNewStateStartsOnTheHomePage() {
        // A new start of the app (also after Android stopped the process) makes a new view model.
        val fresh = RedesignOverviewState()
        assertThat(fresh.page).isEqualTo(HOME_PAGE)
        assertThat(fresh.editMode).isFalse()
    }

    private companion object {

        const val SCREEN = "screen"
    }
}
