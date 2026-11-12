package app.aaps.ui.compose.redesign

import androidx.compose.runtime.Immutable
import app.aaps.core.interfaces.alerts.EatReminderState
import app.aaps.core.interfaces.bgQualityCheck.BgQualityCheck

/**
 * What the redesign home page needs from `MainScreen`, which owns it: the BG source warning, the
 * eat reminder, the open loop suggestion, and the tile values of [RedesignHomeData].
 *
 * The defaults show nothing, so the home page also works without them (tests, previews).
 */
@Immutable
class RedesignHomeInputs(
    val data: RedesignHomeData = RedesignHomeData(),
    /** Name of the BG source with a data problem, or null when its data is fine. */
    val bgSourceName: String? = null,
    val bgQualityState: BgQualityCheck.State? = null,
    /** What the bottom bar's BG source item did: opens the source plugin. */
    val onBgSourceClick: () -> Unit = {},
    val eatReminder: EatReminderState? = null,
    val onEatReminderStop: () -> Unit = {},
    val onEatReminderExtend: () -> Unit = {},
    /** False on a client while the master cannot be reached (same as the stock banner). */
    val eatReminderControlsEnabled: Boolean = true,
    /** True while an open loop suggestion waits to be accepted. */
    val loopSuggestion: Boolean = false,
    /** Opens the sheet to accept the suggestion. */
    val onLoopSuggestionClick: () -> Unit = {}
)
