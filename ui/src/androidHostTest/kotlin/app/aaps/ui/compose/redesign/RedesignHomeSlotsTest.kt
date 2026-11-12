package app.aaps.ui.compose.redesign

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The priority of the button left of the glucose circle (SPEC 3.3). */
class RedesignHomeSlotsTest {

    @Test
    fun theBgSourceWarningWinsOverEverything() {
        assertThat(redesignLeftSlot(bgSourceProblem = true, eatReminder = true, pumpActivity = true)).isEqualTo(RedesignLeftSlot.BG_SOURCE)
    }

    @Test
    fun theEatReminderWinsOverThePumpActivity() {
        assertThat(redesignLeftSlot(bgSourceProblem = false, eatReminder = true, pumpActivity = true)).isEqualTo(RedesignLeftSlot.EAT_REMINDER)
    }

    @Test
    fun thePumpActivityShowsAlone() {
        assertThat(redesignLeftSlot(bgSourceProblem = false, eatReminder = false, pumpActivity = true)).isEqualTo(RedesignLeftSlot.PUMP_ACTIVITY)
    }

    @Test
    fun withNothingTheSlotIsEmpty() {
        assertThat(redesignLeftSlot(bgSourceProblem = false, eatReminder = false, pumpActivity = false)).isEqualTo(RedesignLeftSlot.NONE)
    }
}
