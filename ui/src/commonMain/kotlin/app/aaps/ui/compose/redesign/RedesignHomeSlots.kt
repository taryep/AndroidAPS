package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aaps.core.interfaces.alerts.EatReminderState
import app.aaps.core.interfaces.bgQualityCheck.BgQualityCheck
import app.aaps.core.interfaces.pump.BolusProgressState
import app.aaps.core.objects.extensions.tickerFlow
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.LocalDateUtil
import app.aaps.core.ui.compose.icons.IcCarbs
import app.aaps.core.ui.compose.icons.Pump
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.overview.EatReminderBannerContent

/** What the left slot shows. Only one, by priority: BG source warning > eat reminder > pump activity. */
enum class RedesignLeftSlot { BG_SOURCE, EAT_REMINDER, PUMP_ACTIVITY, NONE }

/** The priority rule of the left slot (SPEC 3.3). */
fun redesignLeftSlot(bgSourceProblem: Boolean, eatReminder: Boolean, pumpActivity: Boolean): RedesignLeftSlot = when {
    bgSourceProblem -> RedesignLeftSlot.BG_SOURCE
    eatReminder     -> RedesignLeftSlot.EAT_REMINDER
    pumpActivity    -> RedesignLeftSlot.PUMP_ACTIVITY
    else            -> RedesignLeftSlot.NONE
}

/**
 * The button left of the glucose circle. It always keeps its place, so the circle does not move when
 * it shows or hides.
 */
@Composable
fun RedesignLeftSlotContent(
    inputs: RedesignHomeInputs,
    pumpActivity: Boolean,
    bolusState: BolusProgressState?,
    onPumpActivityClick: () -> Unit
) {
    val colors = redesignColors()
    val bgProblem = inputs.bgSourceName != null && (inputs.bgQualityState == BgQualityCheck.State.DOUBLED || inputs.bgQualityState == BgQualityCheck.State.FLAT)
    var showEatSheet by rememberSaveable { mutableStateOf(false) }
    when (redesignLeftSlot(bgProblem, inputs.eatReminder != null, pumpActivity)) {
        RedesignLeftSlot.BG_SOURCE     -> {
            val reason = stringResource(if (inputs.bgQualityState == BgQualityCheck.State.FLAT) UiStrings.redesign_bg_flat else UiStrings.redesign_bg_doubled)
            val source = inputs.bgSourceName.orEmpty()
            RedesignSlotButton(
                tint = colors.disabled,
                icon = Icons.Default.WaterDrop,
                title = source,
                caption = reason,
                description = stringResource(UiStrings.redesign_bg_problem_description, reason, source),
                onClick = inputs.onBgSourceClick
            )
        }

        RedesignLeftSlot.EAT_REMINDER  -> {
            val state = inputs.eatReminder ?: return
            val minutes = eatReminderMinutesText(state)
            RedesignSlotButton(
                tint = colors.carbsRequired,
                icon = IcCarbs,
                title = minutes,
                caption = stringResource(UiStrings.redesign_meal),
                description = stringResource(UiStrings.redesign_meal_description, minutes),
                onClick = { showEatSheet = true }
            )
        }

        RedesignLeftSlot.PUMP_ACTIVITY -> {
            val percent = bolusState?.takeIf { it.isSMB && it.percent in 1..99 }?.percent
            RedesignSlotButton(
                tint = colors.insulin,
                icon = Pump,
                title = percent?.let { stringResource(CoreUiStrings.formatPercent, it.toDouble()) } ?: stringResource(UiStrings.redesign_tile_pump),
                caption = if (percent != null) stringResource(UiStrings.redesign_tile_pump) else "",
                description = stringResource(UiStrings.redesign_pump_activity_description),
                onClick = onPumpActivityClick
            )
        }

        RedesignLeftSlot.NONE          -> Unit
    }
    val eatState = inputs.eatReminder
    if (showEatSheet && eatState != null) {
        RedesignEatReminderSheet(eatState, inputs, onDismiss = { showEatSheet = false })
    } else if (showEatSheet) {
        // The reminder ended (stopped elsewhere, or dismissed): nothing left to show.
        LaunchedEffect(Unit) { showEatSheet = false }
    }
}

/** Minutes left until it is time to eat, rounded up, or "Now" once it rang. */
@Composable
private fun eatReminderMinutesText(state: EatReminderState): String {
    val dateUtil = LocalDateUtil.current
    var now by remember { mutableLongStateOf(dateUtil.now()) }
    LaunchedEffect(Unit) { tickerFlow(EAT_TICK_MS).collect { now = dateUtil.now() } }
    if (state.hasRung(now)) return stringResource(UiStrings.redesign_meal_now)
    val minutes = ((state.remainingMs(now) + 59_999L) / 60_000L).toInt()
    return stringResource(CoreUiStrings.format_mins, minutes)
}

/** The stock eat reminder content (countdown, Stop / +5 min / Dismiss) in a small sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedesignEatReminderSheet(state: EatReminderState, inputs: RedesignHomeInputs, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        EatReminderBannerContent(
            state = state,
            onStop = {
                inputs.onEatReminderStop()
                onDismiss()
            },
            onExtend = inputs.onEatReminderExtend,
            controlsEnabled = inputs.eatReminderControlsEnabled,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        )
    }
}

/** A 72 × 72 dp button beside the glucose circle: icon, a short title and a caption. */
@Composable
fun RedesignSlotButton(
    tint: RedesignTint,
    icon: ImageVector,
    title: String,
    caption: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = tint.background,
        border = BorderStroke(1.dp, tint.border),
        modifier = Modifier
            .size(SLOT_SIZE)
            // One stop for the screen reader, with the whole sentence.
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint.accent, modifier = Modifier.size(20.dp))
            Text(
                title,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tint.accent, fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (caption.isNotEmpty()) {
                Text(
                    caption,
                    style = TextStyle(fontSize = 10.sp, color = tint.accent),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

val SLOT_SIZE = 72.dp
private const val EAT_TICK_MS = 15_000L
