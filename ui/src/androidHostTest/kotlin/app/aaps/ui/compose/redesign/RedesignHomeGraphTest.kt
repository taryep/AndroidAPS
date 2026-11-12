package app.aaps.ui.compose.redesign

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.aaps.core.data.model.GlucoseUnit
import app.aaps.core.data.model.RM
import app.aaps.core.interfaces.overview.graph.BgDataPoint
import app.aaps.core.interfaces.overview.graph.BgRange
import app.aaps.core.interfaces.overview.graph.BgType
import app.aaps.core.interfaces.overview.graph.BolusGraphPoint
import app.aaps.core.interfaces.overview.graph.BolusType
import app.aaps.core.interfaces.overview.graph.CarbsGraphPoint
import app.aaps.core.interfaces.overview.graph.GraphDataPoint
import app.aaps.core.interfaces.overview.graph.IobGraphData
import app.aaps.core.interfaces.overview.graph.RunningModeGraphData
import app.aaps.core.interfaces.overview.graph.RunningModeSegment
import app.aaps.core.interfaces.overview.graph.TargetLineData
import app.aaps.core.interfaces.overview.graph.TreatmentGraphData
import app.aaps.core.interfaces.resources.TextRefIdRegistry
import app.aaps.core.keys.StringKey
import app.aaps.core.keys.UnitDoubleKey
import app.aaps.ui.R
import app.aaps.ui.UiStringIds
import app.aaps.ui.compose.testing.AapsScreenFixture
import app.aaps.ui.compose.testing.OverviewViewModelFixture
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The home graph (SPEC 3.6): pictures of each range and theme, mmol/L and a reading above 280, the
 * range buttons, the spoken description, and a long press showing the stock pill. Pictures go to
 * `ui/build/reports/redesign/graph/`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-rFR-w412dp-h891dp-xxhdpi")
class RedesignHomeGraphTest {

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
        // The pill shows the time of the reading.
        whenever(screen.dateUtil.timeString(any<Long>())).thenReturn(PILL_TIME)
        // The real theme in dark or light, glucose colours included: AapsTheme follows this setting.
        whenever(screen.preferences.observe(StringKey.GeneralDarkMode)).thenReturn(darkMode)
    }

    private val darkMode = MutableStateFlow("dark")

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun picturesOfEveryRange() {
        withDay()
        for (range in HomeGraphRange.entries) {
            show(range)
            picture("range_${range.hours}h_dark")
        }
        show(HomeGraphRange.H6, dark = false)
        picture("range_6h_light")
    }

    @Test
    fun aReadingAbove280MakesTheAxisTaller() {
        withDay(peak = 320.0)
        show(HomeGraphRange.H6)
        picture("above_280")
    }

    @Test
    fun mmolUsersGetTheSameRangeInTheirUnit() {
        whenever(screen.profileUtil.units).thenReturn(GlucoseUnit.MMOL)
        whenever(screen.preferences.get(UnitDoubleKey.OverviewHighMark)).thenReturn(10.0)
        whenever(screen.preferences.get(UnitDoubleKey.OverviewLowMark)).thenReturn(3.9)
        whenever(screen.preferences.observe(UnitDoubleKey.OverviewHighMark)).thenReturn(MutableStateFlow(10.0))
        whenever(screen.preferences.observe(UnitDoubleKey.OverviewLowMark)).thenReturn(MutableStateFlow(3.9))
        withDay(mmol = true)
        show(HomeGraphRange.H6)
        // The labels are drawn, not text nodes: checked on the picture (3.1 / 3.9 / 10.0 / 13.9, steps every 2).
        picture("mmol")
        // The spoken value is in mmol/L.
        compose.onNode(hasContentDescription("mmol/L", substring = true)).assertExists()
    }

    @Test
    fun theRangeButtonsShowTheChoiceAndChangeIt() {
        withDay()
        show(HomeGraphRange.DEFAULT)
        compose.onNodeWithContentDescription(rangeName(6)).assertIsSelected()
        compose.onNodeWithContentDescription(rangeName(12)).assertIsNotSelected().performClick()
        compose.waitForIdle()
        assertThat(shownRange).isEqualTo(HomeGraphRange.H12)
        compose.onNodeWithContentDescription(rangeName(12)).assertIsSelected()
        compose.onNodeWithContentDescription(rangeName(6)).assertIsNotSelected()
    }

    @Test
    fun theGraphSaysWhatItShows() {
        withDay()
        show(HomeGraphRange.H6)
        compose.onNode(hasContentDescription(rangeName(6), substring = true) and hasContentDescription("mg/dL", substring = true)).assertExists()
    }

    @Test
    fun withoutReadingsTheGraphSaysSo() {
        show(HomeGraphRange.H6)
        val expected = string(R.string.redesign_graph_description_no_value, rangeName(6))
        compose.onNodeWithContentDescription(expected).assertExists()
        picture("no_reading")
    }

    @Test
    fun aLongPressShowsTheStockPill() {
        withDay()
        show(HomeGraphRange.H6)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(GRAPH).performTouchInput {
            down(Offset(width * 0.6f, height * 0.6f))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(4f, 0f))
        }
        compose.mainClock.advanceTimeBy(100)
        // The stock pill: it starts with the time of the reading.
        compose.onNode(hasText(PILL_TIME), useUnmergedTree = true).assertExists()
        picture("hold")
    }

    /** 24 h of readings every 5 min, IOB, a target and an hour of open loop 2 h ago. */
    private fun withDay(peak: Double = 220.0, mmol: Boolean = false, dip: Double? = null) {
        val now = OverviewViewModelFixture.NOW
        val times = (0 until 288).map { now - it * 300_000L }.reversed()
        val factor = if (mmol) 1 / 18.01559 else 1.0
        viewModels.cache.bgReadingsFlow.value = times.mapIndexed { i, t ->
            val mgdl = if (dip != null && i in 140..160) dip else 130.0 + (peak - 130.0) * sin(i / 10.0).coerceAtLeast(-0.8)
            val range = when {
                mgdl > 250 -> BgRange.VERY_HIGH
                mgdl > 180 -> BgRange.HIGH
                mgdl < 56  -> BgRange.VERY_LOW
                mgdl < 72  -> BgRange.LOW
                else       -> BgRange.IN_RANGE
            }
            BgDataPoint(t, mgdl * factor, range, BgType.REGULAR)
        }
        viewModels.cache.iobGraphFlow.value = IobGraphData(times.mapIndexed { i, t -> GraphDataPoint(t, 1.5 + sin(i / 9.0)) }, emptyList())
        viewModels.cache.targetLineFlow.value = TargetLineData(listOf(GraphDataPoint(times.first(), 110.0 * factor)))
        viewModels.cache.runningModeGraphFlow.value = RunningModeGraphData(listOf(RunningModeSegment(RM.Mode.OPEN_LOOP, now - 3 * HOUR, now - 2 * HOUR)))
        // A meal with its bolus, two SMBs after it, and a correction earlier.
        viewModels.cache.treatmentGraphFlow.value = TreatmentGraphData(
            boluses = listOf(
                BolusGraphPoint(now - 2 * HOUR, 4.0, BolusType.NORMAL, true, "4.00 U"),
                BolusGraphPoint(now - 80 * MINUTE, 0.3, BolusType.SMB, true, "0.30 U"),
                BolusGraphPoint(now - 50 * MINUTE, 0.4, BolusType.SMB, true, "0.40 U"),
                BolusGraphPoint(now - 8 * HOUR, 1.5, BolusType.NORMAL, true, "1.50 U")
            ),
            carbs = listOf(CarbsGraphPoint(now - 2 * HOUR - 10 * MINUTE, 45.0, true, "45 g")),
            extendedBoluses = emptyList(), therapyEvents = emptyList(), calibrations = emptyList()
        )
    }

    @Test
    fun picturesWithTreatmentsOnEveryRange() {
        withDay()
        for (range in listOf(HomeGraphRange.H6, HomeGraphRange.H12, HomeGraphRange.H24)) {
            show(range)
            picture("treatments_${range.hours}h")
        }
    }

    @Test
    fun aReadingBelow40MakesTheAxisLowerAndTheStepFollows() {
        withDay(dip = 28.0)
        show(HomeGraphRange.H24)
        picture("below_40")
    }

    @Test
    fun aTemporaryTargetColoursTheLabelAndTheCurrentStep() {
        withDay()
        // The profile target until half an hour ago, then a temporary one.
        val now = OverviewViewModelFixture.NOW
        viewModels.cache.targetLineFlow.value = TargetLineData(listOf(GraphDataPoint(now - 20 * HOUR, 110.0), GraphDataPoint(now - 30 * MINUTE, 150.0)))
        accent = Color(0xFFF7A541)
        show(HomeGraphRange.H6)
        picture("target_temp")
        accent = null
        compose.waitForIdle()
        picture("target_profile")
    }

    @Test
    fun onlyTheRangeButtonsCanBeTapped() {
        // Step 2 (decided 10 Nov): the plot has no tap action any more; the graphs page opens from the bottom bar.
        withDay()
        show(HomeGraphRange.H6)
        compose.onNodeWithTag(GRAPH).performTouchInput { click(Offset(width * 0.5f, height * 0.6f)) }
        compose.waitForIdle()
        val tappable = compose.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(GRAPH))).fetchSemanticsNodes()
        assertThat(tappable).hasSize(HomeGraphRange.entries.size)
    }

    // A test may show several pictures, but content can be set only once: the range is state, the theme a flow.
    private var shownRange by mutableStateOf(HomeGraphRange.DEFAULT)
    private var contentSet = false
    private var accent by mutableStateOf<Color?>(null)

    private fun show(range: HomeGraphRange, dark: Boolean = true) {
        shownRange = range
        darkMode.value = if (dark) "dark" else "light"
        if (!contentSet) {
            contentSet = true
            compose.setContent {
                screen.Content {
                    Box(Modifier.requiredSize(380.dp, 356.dp).testTag(GRAPH)) {
                        RedesignHomeGraph(viewModels.graphViewModel, shownRange, { shownRange = it }, targetAccent = accent)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun picture(name: String) {
        val dir = File("build/reports/redesign/graph").apply { mkdirs() }
        val bitmap = compose.onNodeWithTag(GRAPH).captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun string(id: Int, vararg args: Any): String = RuntimeEnvironment.getApplication().getString(id, *args)
    private fun rangeName(hours: Int) = string(R.string.redesign_graph_range_description, hours)

    private companion object {

        const val GRAPH = "graph"
        const val PILL_TIME = "21:33"
        const val HOUR = 3_600_000L
        const val MINUTE = 60_000L
    }
}
