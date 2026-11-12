package app.aaps.ui.compose.redesign

import kotlin.math.abs
import kotlin.math.max

/**
 * The three graph sizes of the redesign graphs page (SPEC 4.2). The saved height of a graph is a size code:
 * [SMALL] = 100, [MEDIUM] = 175, [GRAND] = 250. The stock saved format is not changed.
 *
 * @param code the value saved in the graph config
 * @param units how many "units" the size is worth: a page holds 4 units
 */
enum class GraphSize(val code: Int, val units: Float) {

    SMALL(100, 1f),
    MEDIUM(175, 1.5f),
    GRAND(250, 2f);

    companion object {

        /** The size of a saved height: the nearest code. A value in the middle of two codes gets the smaller one. */
        fun fromSavedHeight(saved: Int): GraphSize = entries.minBy { abs(it.code - saved) }
    }
}

/** Heights of the graphs on the redesign graphs page. All values are dp, as plain floats so they are easy to test. */
object GraphHeights {

    /** Smallest height of a Small graph. */
    const val MIN_SMALL_DP = 100f

    /** A page holds this many units (Small = 1, Medium = 1.5, Grand = 2). */
    const val PAGE_UNITS = 4

    /**
     * Height of a Small graph: a quarter of the page, after the gaps between four graphs.
     * @param pageHeight the height between the title row and the bottom bar
     * @param gap the gap between two graphs
     */
    fun small(pageHeight: Float, gap: Float): Float = max(MIN_SMALL_DP, (pageHeight - (PAGE_UNITS - 1) * gap) / PAGE_UNITS)

    /**
     * Height of a graph block of [size], with [small] from [small].
     * A graph that is worth more than one unit also takes the gaps it spans: a Grand graph is as high as two
     * Small graphs and the gap between them. So Grand + Small + Small and four Small graphs both fill the page
     * exactly, although the first one has one gap less.
     */
    fun of(size: GraphSize, small: Float, gap: Float): Float = size.units * small + (size.units - 1f) * gap

    /** Same, from a saved height (any value; it is rounded to the nearest size). */
    fun ofSaved(savedHeight: Int, small: Float, gap: Float): Float = of(GraphSize.fromSavedHeight(savedHeight), small, gap)
}
