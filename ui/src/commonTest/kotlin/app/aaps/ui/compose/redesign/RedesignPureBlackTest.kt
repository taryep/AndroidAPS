package app.aaps.ui.compose.redesign

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Step 13: "Pure black" keeps coloured text readable on the lighter surfaces. */
class RedesignPureBlackTest {

    private val colors = PureBlackRedesignColors

    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }

    @Test
    fun whatCannotBeTappedIsBlack() {
        assertEquals(Color.Black, colors.pageBackground)
        assertEquals(Color.Black, colors.graphCard)
    }

    @Test
    fun whatCanBeTappedIsLighterThanTheDarkTheme() {
        assertTrue(colors.surface.luminance() > DarkRedesignColors.surface.luminance())
        assertTrue(colors.insulin.background.luminance() > DarkRedesignColors.insulin.background.luminance())
    }

    @Test
    fun textStaysReadableOnEveryTint() {
        val tints = listOf(
            colors.neutral, colors.closedLoop, colors.openLoop, colors.lgs, colors.suspended, colors.disconnected,
            colors.disabled, colors.superBolus, colors.profileTemp, colors.targetTemp, colors.insulin,
            colors.carbsActive, colors.carbsRequired, colors.tempBasal
        )
        for (tint in tints) {
            assertTrue(contrast(tint.accent, tint.background) >= 4.5f, "accent $tint")
            assertTrue(contrast(colors.textPrimary, tint.background) >= 4.5f, "text $tint")
        }
        assertTrue(contrast(colors.textSecondary, colors.surface) >= 4.5f)
    }
}
