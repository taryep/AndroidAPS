package app.aaps.ui.compose.redesign

import app.aaps.core.data.model.RM
import kotlin.math.PI
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Step 12: the ring of the glucose circle shows the loop mode. */
class RedesignLoopRingTest {

    @Test
    fun eachModeHasThePatternOfItsLoopIcon() {
        assertEquals(RedesignLoopRing.SOLID, RM.Mode.CLOSED_LOOP.redesignLoopRing())
        assertEquals(RedesignLoopRing.DASHES, RM.Mode.CLOSED_LOOP_LGS.redesignLoopRing())
        assertEquals(RedesignLoopRing.DASHES, RM.Mode.OPEN_LOOP.redesignLoopRing())
        assertEquals(RedesignLoopRing.LONG_DASHES, RM.Mode.SUSPENDED_BY_USER.redesignLoopRing())
        assertEquals(RedesignLoopRing.DOTS, RM.Mode.DISABLED_LOOP.redesignLoopRing())
        assertEquals(RedesignLoopRing.DOTS, RM.Mode.DISCONNECTED_PUMP.redesignLoopRing())
    }

    @Test
    fun theTintIsTheOneOfTheLoopSlot() {
        assertEquals(DarkRedesignColors.lgs, RM.Mode.CLOSED_LOOP_LGS.redesignLoopTint(DarkRedesignColors))
        assertEquals(DarkRedesignColors.neutral, null.redesignLoopTint(DarkRedesignColors))
    }

    @Test
    fun aWholeNumberOfDashesFitsTheCircle() {
        assertNull(RedesignLoopRing.SOLID.intervals(100f, 8f))
        for (ring in listOf(RedesignLoopRing.DASHES, RedesignLoopRing.LONG_DASHES, RedesignLoopRing.DOTS)) {
            val (on, off) = ring.intervals(100f, 8f)!!.toList()
            val count = 2 * PI * 100 / (on + off)
            assertEquals(round(count), count, 0.001)
            assertTrue(off > 0f)
        }
    }
}
