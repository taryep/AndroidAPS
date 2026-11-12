package app.aaps.ui.compose.redesign

import app.aaps.core.interfaces.overview.graph.GraphConfig
import app.aaps.core.interfaces.overview.graph.GraphConfigRepository
import app.aaps.core.keys.BooleanNonKey
import app.aaps.core.keys.interfaces.Preferences

/**
 * One-time change of the saved graph heights for the redesign (SPEC 4.2).
 *
 * The stock default is 100 for every graph, which the redesign reads as Small. The redesign default is
 * a Grand BG graph, so a BG height of exactly 100 (the stock default) becomes 250. Other graphs and other
 * BG heights are not touched. It runs once: a flag in the preferences records it, so a user who later
 * picks Small for BG keeps it.
 */
object RedesignGraphMigration {

    /** The config after the change, or null when nothing must change. */
    fun migrated(config: GraphConfig): GraphConfig? =
        if (config.bgHeight == GraphConfig.DEFAULT_GRAPH_HEIGHT_DP) config.copy(bgHeight = GraphSize.GRAND.code) else null

    /** Runs the change if it was not done yet, then sets the flag. */
    fun runOnce(preferences: Preferences, repository: GraphConfigRepository) {
        if (preferences.get(BooleanNonKey.RedesignGraphHeightsMigrated)) return
        migrated(repository.graphConfigFlow.value)?.let { repository.update(it) }
        preferences.put(BooleanNonKey.RedesignGraphHeightsMigrated, true)
    }
}
