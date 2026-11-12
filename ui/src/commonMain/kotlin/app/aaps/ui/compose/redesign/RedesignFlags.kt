package app.aaps.ui.compose.redesign

/**
 * One switch for the whole phone overview redesign (see `docs/redesign/SPEC.md`).
 *
 * With [ENABLED] set to false the app must behave exactly like stock AAPS. Every hook in an
 * upstream file checks this flag, so the fork can be turned off without removing code.
 */
object RedesignFlags {

    const val ENABLED = true

    /**
     * Two small dots (● ○) at the bottom of the overview pages that show which page is open.
     * Off for now: to be decided once the graphs page fills its height (SPEC 2).
     */
    const val PAGE_DOTS = false
}
