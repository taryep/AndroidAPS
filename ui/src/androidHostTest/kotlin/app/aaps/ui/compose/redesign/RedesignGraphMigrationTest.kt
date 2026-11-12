package app.aaps.ui.compose.redesign

import app.aaps.core.interfaces.overview.graph.GraphConfig
import app.aaps.core.interfaces.overview.graph.GraphConfigRepository
import app.aaps.core.interfaces.overview.graph.SecondaryGraph
import app.aaps.core.interfaces.overview.graph.SeriesType
import app.aaps.core.keys.BooleanNonKey
import app.aaps.core.keys.interfaces.Preferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** The one-time change of the BG graph height from the stock default 100 to Grand (SPEC 4.2). */
class RedesignGraphMigrationTest {

    private val preferences = mock<Preferences>()
    private val repository = mock<GraphConfigRepository>()

    private fun start(config: GraphConfig, done: Boolean) {
        whenever(preferences.get(BooleanNonKey.RedesignGraphHeightsMigrated)).thenReturn(done)
        whenever(repository.graphConfigFlow).thenReturn(MutableStateFlow(config))
    }

    @Test
    fun `a BG height of 100 becomes 250 and nothing else changes`() {
        val config = GraphConfig(iobHeight = 100, secondaryGraphs = listOf(SecondaryGraph(listOf(SeriesType.COB), 100)))
        start(config, done = false)
        RedesignGraphMigration.runOnce(preferences, repository)
        verify(repository).update(config.copy(bgHeight = 250))
        verify(preferences).put(BooleanNonKey.RedesignGraphHeightsMigrated, true)
    }

    @Test
    fun `other graph heights are not touched`() {
        val config = GraphConfig(bgHeight = 100, iobHeight = 175, secondaryGraphs = listOf(SecondaryGraph(listOf(SeriesType.COB), 250)))
        val migrated = RedesignGraphMigration.migrated(config)!!
        assertThat(migrated.iobHeight).isEqualTo(175)
        assertThat(migrated.secondaryGraphs).isEqualTo(config.secondaryGraphs)
    }

    @Test
    fun `a BG height other than 100 is kept, and the flag is still set`() {
        start(GraphConfig(bgHeight = 175), done = false)
        RedesignGraphMigration.runOnce(preferences, repository)
        verify(repository, never()).update(any())
        verify(preferences).put(BooleanNonKey.RedesignGraphHeightsMigrated, true)
    }

    @Test
    fun `it is never done twice`() {
        // The user chose Small for BG after the first run: the flag keeps it.
        start(GraphConfig(bgHeight = 100), done = true)
        RedesignGraphMigration.runOnce(preferences, repository)
        verify(repository, never()).update(any())
        verify(preferences, never()).put(eq(BooleanNonKey.RedesignGraphHeightsMigrated), any<Boolean>())
    }
}
