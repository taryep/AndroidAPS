package app.aaps.ui.compose.redesign

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.overview.graph.OverviewDataCache
import app.aaps.core.interfaces.overview.graph.ProfileDisplayData
import app.aaps.core.interfaces.overview.graph.TbrDisplayData
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.core.interfaces.overview.graph.TempTargetDisplayData
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.resources.TextResolver
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.objects.extensions.convertedToAbsolute
import app.aaps.core.objects.extensions.convertedToPercent
import app.aaps.core.ui.CoreUiStrings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Values of the home tiles that the stock overview state does not have as separate texts: the time
 * left of a temporary profile and of a temporary target, the basal rate, and the rate, percent and
 * time left of a temporary basal.
 *
 * Empty texts mean "nothing to show".
 */
@Immutable
data class RedesignHomeData(
    /** Profile name with its percentage and time shift, without the time left. Empty: use the stock chip text. */
    val profileName: String = "",
    /** Time left of a temporary profile switch, else empty. */
    val profileTimeLeft: String = "",
    /** Target range without the time left. Empty: use the stock chip text. */
    val targetText: String = "",
    /** Time left of a temporary target, else empty. */
    val targetTimeLeft: String = "",
    /** Basal rate of the profile now, e.g. "0.85 U/h". */
    val basalRate: String = "",
    /** True while a temporary basal rate runs. */
    val tempBasalRunning: Boolean = false,
    /** Rate of the temporary basal, e.g. "0.00 U/h". */
    val tempBasalRate: String = "",
    /** The temporary basal as a percent of the profile basal, e.g. "0%". */
    val tempBasalPercent: String = "",
    /** Time left of the temporary basal. */
    val tempBasalTimeLeft: String = ""
)

/**
 * Builds [RedesignHomeData] from the same sources `MainViewModel` reads for the chips, the overview
 * data cache, the profile and the active temporary basal. It only formats; it changes nothing.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class RedesignHomeViewModel(
    private val overviewDataCache: OverviewDataCache,
    private val profileFunction: ProfileFunction,
    private val persistenceLayer: PersistenceLayer,
    private val dateUtil: DateUtil,
    private val rh: TextResolver
) : ViewModel() {

    // The same 30 s tick as the chips, so the time left moves at the same moment.
    private val ticker = flow {
        while (true) {
            emit(dateUtil.now())
            delay(TICK_MS)
        }
    }

    val data: StateFlow<RedesignHomeData> = combine(
        overviewDataCache.profileFlow,
        overviewDataCache.tempTargetFlow,
        overviewDataCache.tbrFlow,
        ticker
    ) { profile, target, tbr, now -> build(profile, target, tbr, now) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RedesignHomeData())

    private suspend fun build(profileData: ProfileDisplayData?, targetData: TempTargetDisplayData?, tbrData: TbrDisplayData?, now: Long): RedesignHomeData {
        val profile = profileFunction.getProfile()
        val tempBasal = if (tbrData != null && tbrData.state != TbrState.NONE) persistenceLayer.getTemporaryBasalActiveAt(now) else null
        val running = if (tempBasal != null && profile != null) tempBasal to profile else null
        return RedesignHomeData(
            profileName = profileData?.profileName.orEmpty(),
            profileTimeLeft = profileData?.let { timeLeft(it.timestamp, it.duration, now) }.orEmpty(),
            targetText = targetData?.targetRangeText.orEmpty(),
            targetTimeLeft = targetData?.let { timeLeft(it.timestamp, it.duration, now) }.orEmpty(),
            basalRate = profile?.let { rh.gs(CoreUiStrings.pump_base_basal_rate, it.getBasal(now)) }.orEmpty(),
            tempBasalRunning = running != null,
            tempBasalRate = running?.let { (tb, p) -> rh.gs(CoreUiStrings.pump_base_basal_rate, tb.convertedToAbsolute(now, p)) }.orEmpty(),
            tempBasalPercent = running?.let { (tb, p) -> rh.gs(CoreUiStrings.formatPercent, tb.convertedToPercent(now, p).toDouble()) }.orEmpty(),
            tempBasalTimeLeft = running?.let { (tb, _) -> timeLeft(tb.timestamp, tb.duration, now) }.orEmpty()
        )
    }

    /** Time left in the same words as the stock chips, or empty when the record has no end or has ended. */
    private fun timeLeft(start: Long, duration: Long, now: Long): String {
        // 30 days or more is "permanent" for the chips too (see MainViewModel.buildChipState).
        if (duration <= 0 || duration >= T.days(30).msecs()) return ""
        val end = start + duration
        return if (now < end) dateUtil.untilString(end, rh, withParentheses = false) else ""
    }

    private companion object {

        const val TICK_MS = 30_000L
    }
}
