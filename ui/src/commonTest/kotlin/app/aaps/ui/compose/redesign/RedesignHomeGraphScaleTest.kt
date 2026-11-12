package app.aaps.ui.compose.redesign

import app.aaps.core.data.configuration.Constants
import app.aaps.core.data.model.GlucoseUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toInstant
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedesignHomeGraphScaleTest {

    private val utc = TimeZone.UTC

    /** India: half an hour off UTC, so a local full hour is not a UTC full hour. */
    private val india = UtcOffset(hours = 5, minutes = 30).asTimeZone()

    private fun at(hour: Int, minute: Int, zone: TimeZone = utc) =
        LocalDateTime(2026, 11, 8, hour, minute).toInstant(zone).toEpochMilliseconds()

    @Test
    fun theWindowEndsAtTheNextFullHour() {
        assertEquals(at(1, 0), homeGraphWindowEnd(at(0, 55), utc))
        assertEquals(at(1, 0), homeGraphWindowEnd(at(0, 1), utc))
        // At a full hour exactly, that hour.
        assertEquals(at(1, 0), homeGraphWindowEnd(at(1, 0), utc))
    }

    @Test
    fun theFullHourIsALocalOne() {
        assertEquals(at(11, 0, india), homeGraphWindowEnd(at(10, 55, india), india))
    }

    @Test
    fun hourLabelsIncludeBothEdgesWithTheStepOfTheRange() {
        val end = at(1, 0)
        val six = homeGraphHourTicks(end, HomeGraphRange.H6)
        assertEquals(7, six.size)
        assertEquals(end - 6 * HOUR, six.first())
        assertEquals(end, six.last())
        // 1 h, 1 h, 2 h, 3 h, 4 h steps: always 4 to 7 labels.
        assertEquals(listOf(4, 7, 7, 7, 7), HomeGraphRange.entries.map { homeGraphHourTicks(end, it).size })
        val day = homeGraphHourTicks(end, HomeGraphRange.H24)
        assertTrue(day.zipWithNext().all { (a, b) -> b - a == 4 * HOUR })
    }

    @Test
    fun theAxisGrowsByFiftyAbove280Only() {
        assertEquals(280.0, homeGraphMaxMgdl(emptyList()))
        assertEquals(280.0, homeGraphMaxMgdl(listOf(120.0, 280.0)))
        assertEquals(300.0, homeGraphMaxMgdl(listOf(281.0)))
        assertEquals(300.0, homeGraphMaxMgdl(listOf(300.0)))
        assertEquals(350.0, homeGraphMaxMgdl(listOf(301.0)))
    }

    @Test
    fun theVeryLowAndVeryHighLinesAreTheTirLimits() {
        assertEquals(56, HOME_GRAPH_VERY_LOW_MGDL.roundToInt()) // 55.85
        assertEquals(250, HOME_GRAPH_VERY_HIGH_MGDL.roundToInt()) // 250.4
    }

    @Test
    fun theAxisGrowsBelow40ByTenToo() {
        assertEquals(40.0, homeGraphMinMgdl(emptyList()))
        assertEquals(40.0, homeGraphMinMgdl(listOf(40.0, 120.0)))
        assertEquals(30.0, homeGraphMinMgdl(listOf(39.0)))
        assertEquals(30.0, homeGraphMinMgdl(listOf(30.0)))
        assertEquals(20.0, homeGraphMinMgdl(listOf(29.0)))
        // Never below zero.
        assertEquals(0.0, homeGraphMinMgdl(listOf(3.0)))
    }

    @Test
    fun greyStepsLeaveOutTheColouredLines() {
        val lines = listOf(HOME_GRAPH_VERY_HIGH_MGDL, 170.0, 70.0, HOME_GRAPH_VERY_LOW_MGDL)
        // 250 is on the very high line (250.4), so it is not drawn twice.
        assertEquals(listOf(50.0, 100.0, 150.0, 200.0), homeGraphGraduations(40.0, 280.0, 50.0, lines))
        assertEquals(listOf(50.0, 100.0, 150.0, 200.0, 300.0, 350.0), homeGraphGraduations(40.0, 350.0, 50.0, lines))
        // An axis that goes down gets the steps under 40 too.
        assertEquals(listOf(30.0, 40.0, 50.0, 60.0), homeGraphGraduations(20.0, 60.0, 10.0, emptyList()))
    }

    @Test
    fun theNormalAxisKeepsAStepOf50AndTwoMmol() {
        // A plot of 270 dp on the axis 40 to 280, at least 32 dp between two graduations.
        assertEquals(50.0, homeGraphStepMgdl(GlucoseUnit.MGDL, 40.0, 280.0, 270f, 32f))
        val mmol = homeGraphStepMgdl(GlucoseUnit.MMOL, 40.0, 280.0, 270f, 32f) / Constants.MMOLL_TO_MGDL
        assertEquals(2.0, mmol, 0.001)
    }

    @Test
    fun theStepGetsCoarserOnATallerAxisOrAShorterPlot() {
        // Same plot, axis 40 to 600: 50 mg/dL would be 22 dp apart.
        assertEquals(100.0, homeGraphStepMgdl(GlucoseUnit.MGDL, 40.0, 600.0, 270f, 32f))
        // A short plot (120 dp) on the normal axis; at 160 dp it is still 50.
        assertEquals(100.0, homeGraphStepMgdl(GlucoseUnit.MGDL, 40.0, 280.0, 120f, 32f))
        assertEquals(50.0, homeGraphStepMgdl(GlucoseUnit.MGDL, 40.0, 280.0, 160f, 32f))
        // A very short axis can have a finer step.
        assertEquals(10.0, homeGraphStepMgdl(GlucoseUnit.MGDL, 40.0, 90.0, 270f, 32f))
    }

    @Test
    fun valuesOutsideTheAxisStayOnItsEdge() {
        assertEquals(100f, homeGraphY(HOME_GRAPH_MIN_MGDL, 40.0, 280.0, top = 0f, bottom = 100f))
        assertEquals(0f, homeGraphY(280.0, 40.0, 280.0, top = 0f, bottom = 100f))
        assertEquals(100f, homeGraphY(20.0, 40.0, 280.0, top = 0f, bottom = 100f))
        assertEquals(0f, homeGraphY(400.0, 40.0, 280.0, top = 0f, bottom = 100f))
        assertEquals(50f, homeGraphY(160.0, 40.0, 280.0, top = 0f, bottom = 100f))
        // A lower axis gives the low readings room.
        assertEquals(50f, homeGraphY(20.0, 0.0, 40.0, top = 0f, bottom = 100f))
    }

    @Test
    fun longRangesDrawFewerDotsAndTheNewestIsAlwaysOne() {
        assertEquals(listOf(1, 1, 3, 6, 6), HomeGraphRange.entries.map { homeGraphDotStep(it) })
        // Counted back from the newest: of 10 readings, 12 h keeps indexes 9, 6, 3, 0.
        val kept = (0..9).filter { (9 - it) % homeGraphDotStep(HomeGraphRange.H12) == 0 }
        assertEquals(listOf(0, 3, 6, 9), kept)
    }

    @Test
    fun aGapOfMoreThanSixteenMinutesBreaksTheLine() {
        val min = 60_000L
        assertEquals(emptyList(), homeGraphRuns(emptyList()))
        assertEquals(listOf(0..3), homeGraphRuns(listOf(0, 5 * min, 10 * min, 15 * min)))
        // 15 minutes is a missed reading, 20 is a gap: the line breaks there.
        assertEquals(listOf(0..1, 2..3), homeGraphRuns(listOf(0, 5 * min, 25 * min, 30 * min)))
        assertEquals(listOf(0..0, 1..1), homeGraphRuns(listOf(0, 60 * min)))
    }

    @Test
    fun theSmoothLineStaysFlatAtAPeakAndADip() {
        val xs = floatArrayOf(0f, 10f, 20f, 30f, 40f)
        // Up, peak, down, dip, up.
        val ys = floatArrayOf(0f, 10f, 5f, 8f, 0f)
        val slopes = homeGraphTangents(xs, ys)
        assertEquals(0f, slopes[0])
        assertEquals(0f, slopes[4])
        // 10 and 5 and 8 are a peak, a dip and a peak.
        assertEquals(0f, slopes[1])
        assertEquals(0f, slopes[2])
        assertEquals(0f, slopes[3])
        // On a steady climb the slope is the slope of the points.
        val climb = homeGraphTangents(floatArrayOf(0f, 10f, 20f), floatArrayOf(0f, 5f, 10f))
        assertEquals(0.5f, climb[1], 0.0001f)
    }

    @Test
    fun labelsTooCloseAreMovedApartInOrder() {
        // Given unsorted, answered in the same order.
        assertEquals(listOf(15f, 10f), spreadLabels(listOf(12f, 10f), minGap = 5f, top = 0f, bottom = 100f))
        // At the bottom edge they move up instead.
        assertEquals(listOf(95f, 100f), spreadLabels(listOf(98f, 99f), minGap = 5f, top = 0f, bottom = 100f))
        // Far enough: not moved.
        assertEquals(listOf(10f, 50f), spreadLabels(listOf(10f, 50f), minGap = 5f, top = 0f, bottom = 100f))
    }

    @Test
    fun dotsGetSmallerWhenTheyAreClose() {
        // 332 px wide: 6 h is 72 readings, 4.6 px apart, so 42 % of it; 3 h has room for the full size.
        assertTrue(abs(homeGraphDotRadius(332f, HomeGraphRange.H6, maxRadius = 2.8f) - 0.42f * 332f / 72f) < 0.001f)
        assertEquals(2.8f, homeGraphDotRadius(332f, HomeGraphRange.H3, maxRadius = 2.8f))
    }

    @Test
    fun sixHoursByDefault() {
        assertEquals(HomeGraphRange.H6, HomeGraphRange.DEFAULT)
    }

    private companion object {

        const val HOUR = 3_600_000L
    }
}
