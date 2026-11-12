package app.aaps.ui.compose.redesign

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.data.model.durationInMinutes
import app.aaps.core.data.model.getPassedDurationToTimeInMinutes
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.aps.Loop
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.iob.IobCobCalculator
import app.aaps.core.interfaces.overview.graph.OverviewDataCache
import app.aaps.core.interfaces.overview.graph.ProfileDisplayData
import app.aaps.core.interfaces.overview.graph.TbrDisplayData
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.core.interfaces.overview.graph.TempTargetDisplayData
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.resources.TextResolver
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.DecimalFormatter
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.objects.extensions.convertedToAbsolute
import app.aaps.core.objects.extensions.convertedToPercent
import app.aaps.core.objects.extensions.round
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.extensions.displayText
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.overview.chips.CobUiState
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
    val tempBasalTimeLeft: String = "",
    /** The data of the stock "Temp basal" dialog on one line (SPEC 3.2.3), e.g. "0.00 U/h @15:58 5/60 min". */
    val tempBasalLine: String = ""
)

/**
 * The parts of the insulin on board, as the stock IOB dialog shows them (`ChipsViewModel.showIobInfo`):
 * same calls, same rounding.
 */
@Immutable
data class RedesignIobParts(val bolus: Double, val basal: Double) {

    /** The total, added exactly as the stock dialog adds it. */
    val total: Double get() = bolus + basal
}

/**
 * Builds [RedesignHomeData] and [RedesignIobParts] from the same sources `MainViewModel` reads for the chips, the overview
 * data cache, the profile and the active temporary basal. It only formats; it changes nothing.
 */
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
@ViewModelKey
@Inject
class RedesignHomeViewModel(
    private val overviewDataCache: OverviewDataCache,
    private val profileFunction: ProfileFunction,
    private val persistenceLayer: PersistenceLayer,
    private val iobCobCalculator: IobCobCalculator,
    private val dateUtil: DateUtil,
    private val rh: TextResolver,
    private val loop: Loop,
    private val config: Config,
    private val decimalFormatter: DecimalFormatter,
    private val preferences: Preferences
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

    /**
     * Bolus and basal insulin on board for the IOB line of the insulin screen (SPEC 3.2.2). Same trigger
     * as the IOB chip (a new IOB graph), plus the tick. Only computed while the line is shown. Null until
     * the first value is known.
     */
    val iobParts: StateFlow<RedesignIobParts?> = combine(overviewDataCache.iobGraphFlow, ticker) { _, _ ->
        RedesignIobParts(
            bolus = iobCobCalculator.calculateIobFromBolus().round().iob,
            basal = iobCobCalculator.calculateIobFromTempBasalsIncludingConvertedExtended().round().basaliob
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The carbs for the Glucides tile (SPEC 3.2): the carbs on board without the stock "required" part,
     * and the carbs required only when the stock chip would show them. The tile writes "13g required"
     * itself; the stock text already ends with "13 required", so it showed twice. Same trigger as the
     * COB chip (a new COB graph), plus the tick.
     */
    val cob: StateFlow<CobUiState> = combine(overviewDataCache.cobGraphFlow, ticker) { _, _ ->
        val cobInfo = iobCobCalculator.getCobInfo("RedesignHomeViewModel COB")
        val lastRun = loop.lastRun
        val carbsReq = if (config.APS) lastRun?.constraintsProcessed?.carbsReq ?: 0 else 0
        redesignCob(
            cobText = cobInfo.displayText(rh, decimalFormatter) ?: rh.gs(CoreUiStrings.value_unavailable_short),
            cobValue = cobInfo.displayCob ?: 0.0,
            carbsReq = carbsReq,
            lastApsRun = lastRun?.lastAPSRun,
            // Read only when it can matter, as the stock chip does.
            newestCarbsTime = if (carbsReq > 0) persistenceLayer.getNewestCarbs()?.timestamp ?: 0L else 0L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CobUiState())

    /**
     * Goes back to the classic AAPS interface (Menu of the new interface). `ComposeMainActivity` sees the
     * setting change and starts again; the way back is Settings › Appearance.
     */
    fun useClassicInterface() = preferences.put(BooleanKey.GeneralRedesign, false)

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
            tempBasalTimeLeft = running?.let { (tb, _) -> timeLeft(tb.timestamp, tb.duration, now) }.orEmpty(),
            tempBasalLine = running?.let { (tb, p) ->
                rh.gs(
                    UiStrings.redesign_tbr_line,
                    rh.gs(CoreUiStrings.pump_base_basal_rate, tb.convertedToAbsolute(now, p)),
                    dateUtil.timeString(tb.timestamp),
                    tb.getPassedDurationToTimeInMinutes(now),
                    tb.durationInMinutes.toInt()
                )
            }.orEmpty()
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

/**
 * The carbs of the Glucides tile, with the rule of the stock COB chip (`ChipsViewModel.cobUiState`): the
 * carbs required are shown only when no carbs were entered after the last loop run. Else
 * [CobUiState.carbsReq] is 0, so the small text, the orange colour and the border go away together.
 *
 * @param cobText the carbs on board, without any "required" part
 * @param lastApsRun time of the last loop run, null when the loop has not run
 * @param newestCarbsTime time of the newest carbs entry, 0 when there is none
 */
internal fun redesignCob(cobText: String, cobValue: Double, carbsReq: Int, lastApsRun: Long?, newestCarbsTime: Long): CobUiState =
    CobUiState(
        text = cobText,
        carbsReq = if (carbsReq > 0 && lastApsRun != null && newestCarbsTime < lastApsRun) carbsReq else 0,
        cobValue = cobValue
    )
