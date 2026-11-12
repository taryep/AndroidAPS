package app.aaps.ui.compose.redesign

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aaps.core.data.model.RM
import app.aaps.core.interfaces.alerts.EatReminderState
import app.aaps.core.interfaces.bgQualityCheck.BgQualityCheck
import app.aaps.core.interfaces.notifications.AapsNotification
import app.aaps.core.interfaces.notifications.NotificationId
import app.aaps.core.interfaces.notifications.NotificationLevel
import app.aaps.core.interfaces.overview.graph.BgDataPoint
import app.aaps.core.interfaces.overview.graph.GraphConfig
import app.aaps.core.interfaces.overview.graph.SecondaryGraph
import app.aaps.core.interfaces.overview.graph.SeriesType
import app.aaps.core.interfaces.overview.graph.BgRange
import app.aaps.core.interfaces.overview.graph.BgType
import app.aaps.core.interfaces.overview.graph.CobGraphData
import app.aaps.core.interfaces.overview.graph.GraphDataPoint
import app.aaps.core.interfaces.overview.graph.IobGraphData
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.core.interfaces.overview.graph.TimeRange
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.ui.R
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.main.MainNavigationBar
import app.aaps.ui.compose.main.TempTargetChipState
import app.aaps.ui.compose.overview.OverviewScreen
import app.aaps.ui.compose.testing.AapsScreenFixture
import app.aaps.ui.compose.testing.OverviewViewModelFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.sin
import app.aaps.core.ui.R as CoreUiR

/**
 * Pictures of the phone overview with the redesign: no top bar, no QuickLaunch toolbar, the Menu item
 * in the bottom bar, and a notification. The bell and the pump activity button are beside the circle
 * on the home page; the graphs page has no floating buttons (SPEC correction 3.3).
 *
 * The overview and the bar are placed the way `MainScreen` places them: the content gets the bar's
 * height as bottom padding. It writes the pictures (`home_chrome.png`, `graphs_page.png`, ...) to
 * `ui/build/reports/redesign/`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-rFR-w412dp-h891dp-xxhdpi")
class RedesignHomeChromeScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var screen: AapsScreenFixture
    private lateinit var viewModels: OverviewViewModelFixture

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

    @Test
    fun homeWithANotification() = picture("home_chrome", RedesignOverviewState())

    @Test
    fun graphsPageWithANotification() = picture("graphs_page", RedesignOverviewState().apply { page = RedesignOverviewState.GRAPHS_PAGE })

    @Test
    fun graphsPageInEditMode() = picture("graphs_page_edit", RedesignOverviewState().apply { editScreen() })

    @Test
    fun homeWithABgSourceProblem() = picture(
        "home_bg_problem", RedesignOverviewState(),
        RedesignHomeInputs(bgSourceName = "xDrip+", bgQualityState = BgQualityCheck.State.DOUBLED, eatReminder = eatReminder())
    )

    @Test
    fun homeWithAnEatReminderAndPumpActivity() = picture("home_eat_reminder", RedesignOverviewState(), RedesignHomeInputs(eatReminder = eatReminder()), pumpActivity = true)

    @Test
    fun homeWithPumpActivity() = picture("home_pump_activity", RedesignOverviewState(), pumpActivity = true)

    @Test
    fun graphsPageHasNoFloatingButtons() {
        // SPEC correction 3.3: a notification and pump activity, yet nothing floats over the graphs.
        val state = RedesignOverviewState()
        picture("home_pump_bell", state, pumpActivity = true)
        // The matchers do find the two buttons: on the home page they are beside the circle (the bell is
        // the stock button, the pump activity is a slot button with its own name).
        assertThat(onScreen(notificationButton)).isNotEmpty()
        assertThat(onScreen(pumpSlotButton)).isNotEmpty()
        state.page = RedesignOverviewState.GRAPHS_PAGE
        compose.waitForIdle()
        saveScreen("graphs_page_pump")
        assertThat(onScreen(notificationButton)).isEmpty()
        assertThat(onScreen(pumpSlotButton)).isEmpty()
        // Nor the stock floating pump button.
        assertThat(onScreen(pumpFloatingButton)).isEmpty()
    }

    private val notificationButton get() = RuntimeEnvironment.getApplication().getString(R.string.a11y_notifications, 1)
    private val pumpSlotButton get() = RuntimeEnvironment.getApplication().getString(R.string.redesign_pump_activity_description)
    private val pumpFloatingButton get() = RuntimeEnvironment.getApplication().getString(CoreUiR.string.pump)

    /** Indexes of the nodes named [description] that are shown. The page not shown stays composed, but is not displayed. */
    private fun onScreen(description: String): List<Int> {
        val nodes = compose.onAllNodes(hasContentDescription(description), useUnmergedTree = true)
        return nodes.fetchSemanticsNodes().indices.filter { nodes[it].isDisplayed() }
    }

    private fun eatReminder() = EatReminderState(startedAt = screen.dateUtil.now(), endsAt = screen.dateUtil.now() + 12 * 60_000L, durationMs = 12 * 60_000L)

    /**
     * The graphs page with 24 h of readings, insulin and carbs, to check the gaps between the graphs.
     * The hour lines and the "now" line must stay at the same x in every graph. The long-press line is
     * placed per graph with the same x maths, but a hold cannot be shown here: Robolectric does not
     * deliver the long press to the Vico chart (tried with `down` + clock and with `longClick`).
     */
    @Test
    fun graphsPageWithData() {
        val now = OverviewViewModelFixture.NOW
        val times = (0 until 288).map { now - it * 300_000L }.reversed()
        viewModels.cache.timeRangeFlow.value = TimeRange(fromTime = times.first(), toTime = now, endTime = now)
        viewModels.cache.bgReadingsFlow.value = times.mapIndexed { i, t -> BgDataPoint(t, 130.0 + 40.0 * sin(i / 12.0), BgRange.IN_RANGE, BgType.REGULAR) }
        viewModels.cache.iobGraphFlow.value = IobGraphData(times.mapIndexed { i, t -> GraphDataPoint(t, 1.5 + sin(i / 9.0)) }, emptyList())
        viewModels.cache.cobGraphFlow.value = CobGraphData(times.mapIndexed { i, t -> GraphDataPoint(t, 20.0 + 15.0 * sin(i / 7.0)) }, emptyList())
        picture("graphs_page_data", RedesignOverviewState().apply { page = RedesignOverviewState.GRAPHS_PAGE })
    }

    @Test
    fun theGraphsFollowTheWindowHeight() {
        // Split screen: the window gets shorter, the graphs must too, without a restart.
        var height by mutableStateOf(891.dp)
        picture("graphs_page_full_window", RedesignOverviewState().apply { page = RedesignOverviewState.GRAPHS_PAGE }, screenHeight = { height })
        val full = iobGraphNameBottom()
        height = 520.dp
        compose.waitForIdle()
        val half = iobGraphNameBottom()
        assertThat(half).isLessThan(full)
        // And back: the graphs grow again.
        height = 891.dp
        compose.waitForIdle()
        assertThat(iobGraphNameBottom()).isEqualTo(full)
    }

    @Test
    fun grandSmallSmallFillsThePageWithoutScrolling() {
        val config = GraphConfig(bgHeight = 250, secondaryGraphs = listOf(SecondaryGraph(listOf(SeriesType.COB))))
        whenever(viewModels.graphConfigRepository.graphConfigFlow).thenReturn(MutableStateFlow(config))
        picture("graphs_page_gps", RedesignOverviewState().apply { page = RedesignOverviewState.GRAPHS_PAGE })
        // The page ends where the last graph ends: nothing to scroll to (a few px of rounding at most).
        assertThat(scrollRangePx()).isAtMost(compose.density.density * 2)
    }

    @Test
    fun fourSmallGraphsFillThePageWithoutScrolling() {
        val four = GraphConfig(secondaryGraphs = listOf(SecondaryGraph(listOf(SeriesType.COB)), SecondaryGraph(listOf(SeriesType.BGI))))
        whenever(viewModels.graphConfigRepository.graphConfigFlow).thenReturn(MutableStateFlow(four))
        picture("graphs_page_four", RedesignOverviewState().apply { page = RedesignOverviewState.GRAPHS_PAGE })
        assertThat(scrollRangePx()).isAtMost(compose.density.density * 2)
    }

    @Test
    fun moreThanFourUnitsScrollByTheExtra() {
        // Grand + three Small = 5 units: one Small graph and its gap are below the page.
        val config = GraphConfig(bgHeight = 250, secondaryGraphs = listOf(SecondaryGraph(listOf(SeriesType.COB)), SecondaryGraph(listOf(SeriesType.BGI))))
        whenever(viewModels.graphConfigRepository.graphConfigFlow).thenReturn(MutableStateFlow(config))
        picture("graphs_page_five_units", RedesignOverviewState().apply { page = RedesignOverviewState.GRAPHS_PAGE })
        assertThat(scrollRangePx()).isGreaterThan(compose.density.density * 100)
    }

    /** How far the graphs page can scroll, in px; 0 when everything fits. */
    private fun scrollRangePx(): Float {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange), useUnmergedTree = true).fetchSemanticsNodes()
        return nodes.maxOfOrNull { it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() } ?: 0f
    }


    /** Bottom of the "BAS" legend, at the top of the IOB graph: it moves when the graphs above it change height. */
    private fun iobGraphNameBottom(): Float =
        compose.onNode(hasText("BAS"), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.bottom

    private fun picture(
        name: String,
        state: RedesignOverviewState,
        inputs: RedesignHomeInputs = RedesignHomeInputs(),
        pumpActivity: Boolean = false,
        screenHeight: () -> Dp = { 891.dp }
    ) {
        val status = viewModels.awaitStatusItems()
        val notification = AapsNotification(
            id = NotificationId.PROFILE_SET_OK,
            instanceKey = 1,
            text = "Profile set",
            level = NotificationLevel.URGENT
        )
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                screen.Content {
                    var barHeightPx by remember { mutableIntStateOf(0) }
                    val barHeight = with(LocalDensity.current) { barHeightPx.toDp() }
                    Box(Modifier.requiredSize(412.dp, screenHeight()).testTag(SCREEN)) {
                        OverviewScreen(
                            profileName = "Normal", isProfileModified = false, profileProgress = 0f,
                            tempTargetText = "120–130", tempTargetState = TempTargetChipState.None,
                            tempTargetProgress = 0f, tempTargetReason = null,
                            runningMode = RM.Mode.CLOSED_LOOP, runningModeText = "Boucle fermée",
                            runningModeRemaining = "", runningModeProgress = 0f,
                            tbrState = TbrState.NONE, smbEnabled = true, isSimpleMode = false, calcProgress = 100,
                            graphViewModel = viewModels.graphViewModel, chipsViewModel = viewModels.chipsViewModel,
                            manageViewModel = viewModels.manageViewModel, statusViewModel = status,
                            statusLightsDef = PreferenceSubScreenDef(key = "statuslights", titleResId = CoreUiR.string.statuslights),
                            onNavigate = {}, onTbrChipClick = {}, onIobChipClick = {},
                            notifications = listOf(notification), onDismissNotification = {}, onNotificationActionClick = {},
                            autoShowNotificationSheet = false, onAutoShowConsumed = {},
                            paddingValues = PaddingValues(top = STATUS_BAR, bottom = GESTURE_BAR + barHeight),
                            redesign = state,
                            redesignHome = inputs,
                            isPumpCommunicating = pumpActivity
                        )
                        MainNavigationBar(
                            onManageClick = {},
                            onTreatmentClick = {},
                            onMenuClick = {},
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = GESTURE_BAR)
                                .onSizeChanged { barHeightPx = it.height }
                        )
                    }
                }
            }
        }
        compose.waitForIdle()

        saveScreen(name)
    }

    private fun saveScreen(name: String) {
        val dir = File("build/reports/redesign").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag(SCREEN).captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {

        const val SCREEN = "screen"

        /** Typical insets of the reference phone; the real app reads them from the window. */
        val STATUS_BAR = 32.dp
        val GESTURE_BAR = 24.dp
    }
}
