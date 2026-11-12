package app.aaps.ui.compose.redesign

/*
 * Durations of the redesign animations (step 11). The SPEC limit was 200 ms; it is raised to 300 ms for the
 * consumables drawer and the change of page, which move more of the screen. The animations run only when
 * something really changes (a new reading, a tap): never on a timer. Android's "Remove animations" setting
 * makes them instant.
 */

/** The consumables bar opening and closing like a drawer. */
const val REDESIGN_DRAWER_MS = 300

/** The page that appears (home, graphs) fades in. */
const val REDESIGN_PAGE_FADE_MS = 250

/** A new reading: the glucose value cross-fades, the trend arc turns, the new dot appears. */
const val REDESIGN_READING_MS = 200
