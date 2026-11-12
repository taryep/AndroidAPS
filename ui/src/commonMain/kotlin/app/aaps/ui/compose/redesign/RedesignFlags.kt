package app.aaps.ui.compose.redesign

/**
 * One switch for the whole phone overview redesign (see `docs/redesign/SPEC.md`).
 *
 * With [ENABLED] set to false the app must behave exactly like stock AAPS. Every hook in an
 * upstream file checks this flag, so the fork can be turned off without removing code.
 */
object RedesignFlags {

    const val ENABLED = true
}
