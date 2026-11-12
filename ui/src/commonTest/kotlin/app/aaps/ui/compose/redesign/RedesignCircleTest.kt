package app.aaps.ui.compose.redesign

import app.aaps.core.data.model.TrendArrow
import app.aaps.core.interfaces.overview.graph.BgInfoData
import app.aaps.core.interfaces.overview.graph.BgRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Step 2: the glucose circle follows each reading of the source, not only the 5 minute buckets. */
class RedesignCircleTest {

    private val bucket = BgInfoData(
        bgValue = 120.0, bgText = "120", bgRange = BgRange.IN_RANGE, isOutdated = false, timestamp = 10 * MIN,
        trendArrow = TrendArrow.FLAT, trendDescription = "stable", delta = 1.0, deltaText = "+1",
        shortAvgDelta = null, shortAvgDeltaText = null, longAvgDelta = null, longAvgDeltaText = null
    )

    @Test
    fun aNewerReadingGivesItsValueColourAndTimeButKeepsTheStockDeltaAndTrend() {
        val newest = CircleReading(timestamp = 13 * MIN, valueInUnits = 190.0, text = "190", range = BgRange.HIGH)
        val shown = circleBgInfo(bucket, newest, now = 14 * MIN)

        assertEquals("190", shown.bgText)
        assertEquals(BgRange.HIGH, shown.bgRange)
        assertEquals(13 * MIN, shown.timestamp)
        assertEquals("+1", shown.deltaText)
        assertEquals(TrendArrow.FLAT, shown.trendArrow)
        assertFalse(shown.isOutdated)
    }

    @Test
    fun anOlderOrEqualReadingChangesNothing() {
        val same = CircleReading(timestamp = 10 * MIN, valueInUnits = 118.0, text = "118", range = BgRange.IN_RANGE)
        assertEquals(bucket, circleBgInfo(bucket, same, now = 11 * MIN))
        assertEquals(bucket, circleBgInfo(bucket, null, now = 11 * MIN))
    }

    @Test
    fun theValueIsCrossedOutWhenTooOld() {
        // The stock limit is 9 minutes (Constants.OLD_BG_MINUTES).
        assertTrue(circleBgInfo(bucket, null, now = 30 * MIN).isOutdated)
    }

    private companion object {

        const val MIN = 60_000L
    }
}
