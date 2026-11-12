package app.aaps.ui.compose.redesign

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.interfaces.Preferences
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Step 1b: the switch between the new and the classic interface. The setting decides, the main screen
 * follows it, and the setting is on by default.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h891dp")
class RedesignSwitchTest {

    @get:Rule
    val compose = createComposeRule()

    private fun preferences(redesign: Boolean): Preferences = mock<Preferences>().also {
        whenever(it.get(BooleanKey.GeneralRedesign)).thenReturn(redesign)
    }

    @Test
    fun theNewInterfaceIsOnByDefault() {
        assertThat(BooleanKey.GeneralRedesign.defaultValue).isTrue()
    }

    @Test
    fun theSettingDecidesWhichInterfaceIsShown() {
        assertThat(redesignEnabled(preferences(redesign = true))).isEqualTo(RedesignFlags.ENABLED)
        assertThat(redesignEnabled(preferences(redesign = false))).isFalse()
    }

    @Test
    fun withTheSettingOffAPhoneGetsTheClassicMainScreen() {
        val active = mutableListOf<Boolean>()
        compose.setContent {
            CompositionLocalProvider(LocalRedesignEnabled provides false) { active += redesignChromeActive() }
            CompositionLocalProvider(LocalRedesignEnabled provides true) { active += redesignChromeActive() }
        }
        compose.waitForIdle()

        assertThat(active.take(2)).containsExactly(false, RedesignFlags.ENABLED).inOrder()
    }
}
