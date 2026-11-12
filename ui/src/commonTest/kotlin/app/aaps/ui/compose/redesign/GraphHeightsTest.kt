package app.aaps.ui.compose.redesign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Petit / Moyen / Grand heights of the redesign graphs page (SPEC 4.2). */
class GraphHeightsTest {

    private val gap = 12f
    private val pageHeight = 700f

    /** The page: the blocks and the gaps between them (the first block has no gap above it). */
    private fun pageTotal(sizes: List<GraphSize>, pageHeight: Float = this.pageHeight): Float {
        val small = GraphHeights.small(pageHeight, gap)
        return sizes.sumOf { GraphHeights.of(it, small, gap).toDouble() }.toFloat() + (sizes.size - 1) * gap
    }

    private val s = GraphSize.SMALL
    private val m = GraphSize.MEDIUM
    private val g = GraphSize.GRAND

    @Test
    fun `size codes are 100, 175 and 250`() {
        assertEquals(100, GraphSize.SMALL.code)
        assertEquals(175, GraphSize.MEDIUM.code)
        assertEquals(250, GraphSize.GRAND.code)
    }

    @Test
    fun `a saved code gives its own size`() {
        assertEquals(s, GraphSize.fromSavedHeight(100))
        assertEquals(m, GraphSize.fromSavedHeight(175))
        assertEquals(g, GraphSize.fromSavedHeight(250))
    }

    @Test
    fun `an old saved value is rounded to the nearest size`() {
        assertEquals(s, GraphSize.fromSavedHeight(0))
        assertEquals(s, GraphSize.fromSavedHeight(120))
        assertEquals(m, GraphSize.fromSavedHeight(150)) // the stock BG default
        assertEquals(m, GraphSize.fromSavedHeight(200))
        assertEquals(g, GraphSize.fromSavedHeight(230))
        assertEquals(g, GraphSize.fromSavedHeight(400))
    }

    @Test
    fun `small is a quarter of the page after the gaps`() {
        assertEquals((700f - 36f) / 4f, GraphHeights.small(700f, gap), 0.001f)
    }

    @Test
    fun `small is never lower than 100 dp`() {
        assertEquals(100f, GraphHeights.small(300f, gap), 0.001f)
        assertEquals(100f, GraphHeights.small(0f, gap), 0.001f)
    }

    @Test
    fun `medium is one and a half small and grand is two`() {
        val small = 166f
        assertEquals(small, GraphHeights.of(s, small, 0f), 0.001f)
        assertEquals(1.5f * small, GraphHeights.of(m, small, 0f), 0.001f)
        assertEquals(2f * small, GraphHeights.of(g, small, 0f), 0.001f)
    }

    @Test
    fun `layouts that fill one page fill it exactly`() {
        val layouts = listOf(
            listOf(g, g), listOf(g, s, s), listOf(s, g, s), listOf(s, s, g),
            listOf(m, m, s), listOf(m, s, m), listOf(s, m, m), listOf(s, s, s, s)
        )
        for (sizes in layouts) {
            assertEquals(pageHeight, pageTotal(sizes), 0.01f, "layout $sizes")
        }
    }

    @Test
    fun `the default Grand Small Small fills the page on any phone that is not at the minimum`() {
        for (h in listOf(560f, 640f, 700f, 800f, 900f)) {
            assertEquals(h, pageTotal(listOf(g, s, s), h), 0.01f, "page $h")
        }
    }

    @Test
    fun `smaller totals leave empty space at the bottom`() {
        assertTrue(pageTotal(listOf(s, s, s)) < pageHeight)
        assertTrue(pageTotal(listOf(m, s)) < pageHeight)
    }

    @Test
    fun `a taller total than the page scrolls`() {
        assertTrue(pageTotal(listOf(g, g, s)) > pageHeight)
    }

    @Test
    fun `on a short page the minimum applies and the page scrolls`() {
        val short = 300f
        assertEquals(100f, GraphHeights.small(short, gap), 0.001f)
        assertTrue(pageTotal(listOf(g, s, s), short) > short)
    }

    @Test
    fun `ofSaved rounds first`() {
        val small = 150f
        assertEquals(GraphHeights.of(m, small, gap), GraphHeights.ofSaved(150, small, gap), 0.001f)
    }
}
