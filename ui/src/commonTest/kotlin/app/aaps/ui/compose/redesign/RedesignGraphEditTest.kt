package app.aaps.ui.compose.redesign

import app.aaps.core.interfaces.overview.graph.GraphConfig
import app.aaps.core.interfaces.overview.graph.SecondaryGraph
import app.aaps.core.interfaces.overview.graph.SeriesType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Step 9: the edit mode of the graphs page changes only the stock graph settings, as plain list changes. */
class RedesignGraphEditTest {

    private val cob = SecondaryGraph(listOf(SeriesType.COB), GraphSize.SMALL.code)
    private val dev = SecondaryGraph(listOf(SeriesType.DEVIATIONS), GraphSize.MEDIUM.code)
    private val config = GraphConfig(bgHeight = GraphSize.GRAND.code, iobHeight = GraphSize.SMALL.code, secondaryGraphs = listOf(cob, dev))

    @Test
    fun theSizeGoesOneStepAndStopsAtTheEnds() {
        assertEquals(GraphSize.MEDIUM.code, RedesignGraphEdit.resize(config, GraphBlock.Kind.IOB, 0, bigger = true).iobHeight)
        assertSame(config, RedesignGraphEdit.resize(config, GraphBlock.Kind.BG, 0, bigger = true))
        assertSame(config, RedesignGraphEdit.resize(config, GraphBlock.Kind.OTHER, 0, bigger = false))
        val bigger = RedesignGraphEdit.resize(config, GraphBlock.Kind.OTHER, 1, bigger = true)
        assertEquals(listOf(GraphSize.SMALL.code, GraphSize.GRAND.code), bigger.secondaryGraphs.map { it.height })
    }

    @Test
    fun aGraphMovesOnePlaceAndNeverAboveIobOrBelowTheLast() {
        assertEquals(listOf(dev, cob), RedesignGraphEdit.move(config, 1, up = true).secondaryGraphs)
        assertEquals(listOf(dev, cob), RedesignGraphEdit.move(config, 0, up = false).secondaryGraphs)
        assertSame(config, RedesignGraphEdit.move(config, 0, up = true))
        assertSame(config, RedesignGraphEdit.move(config, 1, up = false))
    }

    @Test
    fun deleteRemovesOnlyThatGraph() {
        assertEquals(listOf(dev), RedesignGraphEdit.remove(config, 0).secondaryGraphs)
        assertSame(config, RedesignGraphEdit.remove(config, 5))
    }

    @Test
    fun aNewGraphGoesAtTheBottomInSmallWithASeriesNoOtherGraphShows() {
        val added = RedesignGraphEdit.add(config, available = listOf(SeriesType.COB, SeriesType.DEVIATIONS, SeriesType.SENSITIVITY))
        assertEquals(3, added.secondaryGraphs.size)
        assertEquals(SecondaryGraph(listOf(SeriesType.SENSITIVITY), GraphSize.SMALL.code), added.secondaryGraphs.last())
    }

    @Test
    fun noGraphIsAddedAboveTheStockMaximum() {
        val full = config.copy(secondaryGraphs = List(GraphConfig.MAX_SECONDARY_GRAPHS) { cob })
        assertFalse(RedesignGraphEdit.canAdd(full))
        assertSame(full, RedesignGraphEdit.add(full))
        assertTrue(RedesignGraphEdit.canAdd(config))
    }

    @Test
    fun cancelAndBackPutTheGraphsBackAndDoneKeepsThem() {
        val state = RedesignOverviewState()
        state.editScreen()
        state.editSnapshot = config
        state.cancelEdit()
        assertFalse(state.editMode)
        assertEquals(config, state.restoreGraphs)

        state.restoreGraphs = null
        state.editScreen()
        state.back()
        assertEquals(config, state.restoreGraphs)

        // (✓) only leaves the edit mode.
        state.restoreGraphs = null
        state.editScreen()
        state.editMode = false
        assertNull(state.restoreGraphs)
    }
}
