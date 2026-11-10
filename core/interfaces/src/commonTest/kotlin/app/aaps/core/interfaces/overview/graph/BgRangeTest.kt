package app.aaps.core.interfaces.overview.graph

import kotlin.test.Test
import kotlin.test.assertEquals

class BgRangeTest {

    private val veryLowMark = 54.0
    private val lowMark = 70.0
    private val highMark = 180.0
    private val veryHighMark = 250.0

    private fun rangeOf(value: Double) = BgRange.of(value, veryLowMark, lowMark, highMark, veryHighMark)

    @Test
    fun valuesAreSortedIntoFiveRanges() {
        assertEquals(BgRange.VERY_LOW, rangeOf(40.0))
        assertEquals(BgRange.LOW, rangeOf(60.0))
        assertEquals(BgRange.IN_RANGE, rangeOf(120.0))
        assertEquals(BgRange.HIGH, rangeOf(200.0))
        assertEquals(BgRange.VERY_HIGH, rangeOf(300.0))
    }

    @Test
    fun valuesOnAMarkBelongToTheRangeInside() {
        assertEquals(BgRange.LOW, rangeOf(veryLowMark))
        assertEquals(BgRange.IN_RANGE, rangeOf(lowMark))
        assertEquals(BgRange.IN_RANGE, rangeOf(highMark))
        assertEquals(BgRange.HIGH, rangeOf(veryHighMark))
    }

    @Test
    fun marksAreUsedAsGiven() {
        // 3.5 mmol/l: above a very low mark of 3.0, below a low mark of 3.9
        assertEquals(BgRange.LOW, BgRange.of(3.5, 3.0, 3.9, 10.0, 13.9))
        assertEquals(BgRange.IN_RANGE, BgRange.of(6.0, 3.0, 3.9, 10.0, 13.9))
        // The user can move the very low mark up
        assertEquals(BgRange.VERY_LOW, BgRange.of(3.5, 3.6, 3.9, 10.0, 13.9))
    }

    @Test
    fun veryMarksWinOverMarksBeyondThem() {
        // A low mark below the very low mark must not hide a very low value.
        assertEquals(BgRange.VERY_LOW, BgRange.of(50.0, veryLowMark, 40.0, highMark, veryHighMark))
        // A high mark above the very high mark must not hide a very high value.
        assertEquals(BgRange.VERY_HIGH, BgRange.of(300.0, veryLowMark, lowMark, 350.0, veryHighMark))
    }
}
