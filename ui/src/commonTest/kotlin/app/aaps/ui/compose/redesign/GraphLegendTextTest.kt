package app.aaps.ui.compose.redesign

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A legend line can break only between two entries (SPEC 4.1). */
class GraphLegendTextTest {

    private val items = listOf(
        GraphLegendItem("Glucides actifs", Color.Red),
        GraphLegendItem("Déviations", Color.Blue)
    )

    @Test
    fun `entries are joined by a normal space and nothing else breaks`() {
        val text = legendText(items).text
        // The only normal spaces are between the two entries.
        assertEquals(2, text.count { it == ' ' })
        assertTrue(text.contains("● Glucides actifs"))
        assertTrue(text.contains("● Déviations"))
    }

    @Test
    fun `an entry without a dot has no dot`() {
        assertEquals("Glycémie", legendText(listOf(GraphLegendItem("Glycémie"))).text)
    }
}
