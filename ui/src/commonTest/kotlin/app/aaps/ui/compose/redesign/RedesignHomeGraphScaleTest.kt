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
        val six = homeGraphScrolledTicks(end, end, HomeGraphRange.H6)
        assertEquals(7, six.size)
        assertEquals(end - 6 * HOUR, six.first())
        assertEquals(end, six.last())
        // 1 h, 1 h, 2 h, 3 h, 4 h steps: always 4 to 7 labels.
        assertEquals(listOf(4, 7, 7, 7, 7), HomeGraphRange.entries.map { homeGraphScrolledTicks(end, end, it).size })
        val day = homeGraphScrolledTicks(end, end, HomeGraphRange.H24)
        assertTrue(day.zipWithNext().all { (a, b) -> b - a == 4 * HOUR })
    }

    @Test
    fun theWindowScrollsInsideTheLast24HoursOnly() {
        val nowEnd = at(12, 0)
        // 6 h: back at most 18 h. Further back stops there; later than now follows now again.
        assertEquals(nowEnd - 18 * HOUR, homeGraphScrolledEnd(nowEnd, nowEnd - 30 * HOUR, HomeGraphRange.H6))
        assertEquals(nowEnd - 2 * HOUR, homeGraphScrolledEnd(nowEnd, nowEnd - 2 * HOUR, HomeGraphRange.H6))
        assertEquals(nowEnd, homeGraphScrolledEnd(nowEnd, nowEnd + HOUR, HomeGraphRange.H6))
        // 24 h never moves.
        assertEquals(nowEnd, homeGraphScrolledEnd(nowEnd, nowEnd - 5 * HOUR, HomeGraphRange.H24))
    }

    @Test
    fun aDragToTheRightShowsOlderDataAndBackToNowFollowsNowAgain() {
        val nowEnd = at(12, 0)
        // Half the plot width on 6 h is 3 hours.
        val back = homeGraphDraggedEnd(nowEnd, null, HomeGraphRange.H6, dragPx = 200f, plotWidthPx = 400f)
        assertEquals(nowEnd - 3 * HOUR, back)
        // Dragging to the left past now gives null: the window follows now, so new readings move it again.
        assertEquals(null, homeGraphDraggedEnd(nowEnd, back, HomeGraphRange.H6, dragPx = -400f, plotWidthPx = 400f))
        // The window of a scrolled graph is a time: when now moves on, it stays where it is.
        assertEquals(back, homeGraphScrolledEnd(nowEnd + HOUR, back, HomeGraphRange.H6))
    }

    @Test
    fun scrolledHourLabelsKeepTheGridOfNow() {
        val nowEnd = at(12, 0)
        // 12 h, labels every 2 h counted back from 12:00; the window ends at 10:30.
        val ticks = homeGraphScrolledTicks(nowEnd, at(10, 30), HomeGraphRange.H12)
        assertEquals(listOf(at(0, 0), at(2, 0), at(4, 0), at(6, 0), at(8, 0), at(10, 0)), ticks)
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

    @Test
    fun aTargetChangeIsJoinedByAVerticalSegmentInTheColourOfTheTargetInForce() {
        // Profile target 100 until 10:00, temporary target 140 since then; buildTargetLine repeats the
        // last value at the end of the graph (11:00).
        val points = listOf(at(6, 0) to 100.0, at(10, 0) to 140.0, at(11, 0) to 140.0)
        val segments = targetLineSegments(points, start = at(7, 0), end = at(11, 0), now = at(10, 30))

        assertEquals(
            listOf(
                TargetLineSegment(at(7, 0), 100.0, at(10, 0), 100.0, current = false),
                TargetLineSegment(at(10, 0), 100.0, at(10, 0), 140.0, current = true),
                TargetLineSegment(at(10, 0), 140.0, at(11, 0), 140.0, current = true)
            ),
            segments
        )
    }

    @Test
    fun theTargetInForceIsNotTheRepeatedLastValue() {
        // The repeated last value can lie before now (end of the graph not moved yet): the step that
        // started at 10:00 is still the one in force, not the repeated point.
        val points = listOf(at(6, 0) to 100.0, at(10, 0) to 140.0, at(10, 20) to 140.0)
        val segments = targetLineSegments(points, start = at(7, 0), end = at(11, 0), now = at(10, 30))

        assertEquals(
            listOf(
                TargetLineSegment(at(7, 0), 100.0, at(10, 0), 100.0, current = false),
                TargetLineSegment(at(10, 0), 100.0, at(10, 0), 140.0, current = true),
                TargetLineSegment(at(10, 0), 140.0, at(10, 20), 140.0, current = true),
                TargetLineSegment(at(10, 20), 140.0, at(10, 30), 140.0, current = true)
            ),
            segments
        )
    }

    @Test
    fun aChangeBeforeTheWindowHasNoVerticalSegmentAndPastChangesAreNotCurrent() {
        // 120 until 8:00 (before the window), 100 until 10:00, back to 120 since then.
        val points = listOf(at(6, 0) to 120.0, at(8, 0) to 100.0, at(10, 0) to 120.0, at(12, 0) to 120.0)
        val segments = targetLineSegments(points, start = at(9, 0), end = at(12, 0), now = at(11, 0))

        assertEquals(
            listOf(
                TargetLineSegment(at(9, 0), 100.0, at(10, 0), 100.0, current = false),
                TargetLineSegment(at(10, 0), 100.0, at(10, 0), 120.0, current = true),
                TargetLineSegment(at(10, 0), 120.0, at(12, 0), 120.0, current = true)
            ),
            segments
        )
    }

    @Test
    fun carbsRequiredFollowTheRuleOfTheStockChip() {
        // Carbs entered before the last loop run: still required.
        assertEquals(13, redesignCob("0 g", 0.0, carbsReq = 13, lastApsRun = 2_000L, newestCarbsTime = 1_000L).carbsReq)
        // Carbs entered after the last loop run: no more required, as on the stock chip.
        assertEquals(0, redesignCob("0 g", 0.0, carbsReq = 13, lastApsRun = 2_000L, newestCarbsTime = 3_000L).carbsReq)
        // The loop has not run: nothing required.
        assertEquals(0, redesignCob("0 g", 0.0, carbsReq = 13, lastApsRun = null, newestCarbsTime = 0L).carbsReq)
        // The text is the carbs on board only: the tile adds "13g requis" itself.
        assertEquals("0 g", redesignCob("0 g", 0.0, carbsReq = 13, lastApsRun = 2_000L, newestCarbsTime = 1_000L).text)
    }

    private companion object {

        const val HOUR = 3_600_000L
    }
}
