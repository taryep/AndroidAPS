package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TT
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.core.ui.compose.LocalClientRole
import app.aaps.core.ui.compose.icons.IcTarget
import app.aaps.core.ui.compose.icons.IcTtActivity
import app.aaps.core.ui.compose.icons.IcTtEatingSoon
import app.aaps.core.ui.compose.icons.IcTtHypo
import app.aaps.core.ui.compose.icons.IcTtManual
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.navigation.icon
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.main.TempTargetChipState
import app.aaps.ui.compose.overview.chips.toIcon

/**
 * One round slot (84 dp) of the circle row (SPEC 3.3): an icon, a bold caption and, under it, a small
 * second line. With no caption and no second line, only the icon is shown (the Loop slot, step 7).
 * [badge] is a small count at the top right (an open loop suggestion waits).
 *
 * The text never overflows: it gets smaller, down to 10 sp, before it ends with "...".
 *
 * @param onClick null for a slot with no action
 * @param onLongClick a second action on a long press, with [onLongClickLabel] for the screen reader
 * @param stateDescription the full stock text for the screen reader
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RedesignRoundSlot(
    tint: RedesignTint,
    icon: ImageVector,
    iconSize: Dp,
    caption: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    secondLine: String = "",
    badge: String? = null,
    description: String? = null,
    stateDescription: String? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null
) {
    val semantics = Modifier.semantics(mergeDescendants = true) {
        if (description != null) contentDescription = description
        if (stateDescription != null) this.stateDescription = stateDescription
        if (onClick != null) role = Role.Button
    }
    val content: @Composable () -> Unit = {
        // The slot is a fixed 84 dp circle, so its text does not grow with the system font (as the BG circle).
        RedesignFontScaleCap(1f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = tint.accent, modifier = Modifier.size(iconSize))
                if (caption.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    SlotText(caption, tint, fontSize = 12, bold = true)
                }
                if (secondLine.isNotEmpty()) SlotText(secondLine, tint, fontSize = 11, bold = false)
            }
        }
    }
    Box(modifier.size(ROUND_SLOT_SIZE)) {
        if (onClick != null && onLongClick != null) {
            // A tap and a long press: a plain Surface with combinedClickable, clipped to the circle.
            Surface(
                shape = CircleShape,
                color = tint.background,
                border = BorderStroke(1.dp, tint.border),
                modifier = Modifier
                    .size(ROUND_SLOT_SIZE)
                    .clip(CircleShape)
                    .combinedClickable(
                        enabled = enabled,
                        role = Role.Button,
                        onLongClickLabel = onLongClickLabel,
                        onLongClick = onLongClick,
                        onClick = onClick
                    )
                    .then(semantics),
                content = content
            )
        } else if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                shape = CircleShape,
                color = tint.background,
                border = BorderStroke(1.dp, tint.border),
                modifier = Modifier.size(ROUND_SLOT_SIZE).then(semantics),
                content = content
            )
        } else {
            Surface(
                shape = CircleShape,
                color = tint.background,
                border = BorderStroke(1.dp, tint.border),
                modifier = Modifier.size(ROUND_SLOT_SIZE).then(semantics),
                content = content
            )
        }
        if (badge != null) {
            // Count badge at the top right, with a ring in the page colour. The number is part of the name below.
            val ring = redesignColors().pageBackground
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(BADGE_SIZE + BADGE_RING * 2)
                    .background(ring, CircleShape)
                    .clearAndSetSemantics { }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(BADGE_SIZE).clip(CircleShape).background(tint.accent)) {
                    Text(badge, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint.background))
                }
            }
        }
    }
}

@Composable
private fun SlotText(text: String, tint: RedesignTint, fontSize: Int, bold: Boolean) {
    BasicText(
        text = text,
        style = TextStyle(
            fontSize = fontSize.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = tint.accent,
            textAlign = TextAlign.Center,
            fontFeatureSettings = "tnum"
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = fontSize.sp)
    )
}

/**
 * The Loop slot: the running mode with its colours (SPEC 3.2.1), the icon only (step 7); the words are for
 * the screen reader. A tap does what the Loop tile did: it opens the running mode dialog, or, while an
 * open loop suggestion waits, the sheet to accept it; then a long press opens the running mode dialog.
 */
@Composable
fun RedesignLoopSlot(
    state: RedesignTilesState,
    commandsAllowed: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    onLoopSuggestionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = redesignColors()
    val role = LocalClientRole.current
    val mode = state.runningMode
    val remaining = state.runningModeRemaining
    val caption: String
    var secondLine = ""
    val tint: RedesignTint
    when {
        state.loopSuggestion                      -> {
            caption = stringResource(UiStrings.redesign_mode_open); tint = colors.openLoop
        }

        mode == RM.Mode.CLOSED_LOOP || mode == RM.Mode.RESUME -> {
            caption = stringResource(UiStrings.redesign_mode_closed); tint = colors.closedLoop
        }

        mode == RM.Mode.OPEN_LOOP                 -> {
            caption = stringResource(UiStrings.redesign_mode_open); tint = colors.openLoop
        }

        mode == RM.Mode.CLOSED_LOOP_LGS           -> {
            caption = stringResource(UiStrings.redesign_mode_lgs); tint = colors.lgs
        }

        mode == RM.Mode.SUSPENDED_BY_USER || mode == RM.Mode.SUSPENDED_BY_PUMP || mode == RM.Mode.SUSPENDED_BY_DST -> {
            caption = stringResource(UiStrings.redesign_mode_paused_no_time); secondLine = remaining; tint = colors.suspended
        }

        mode == RM.Mode.DISCONNECTED_PUMP         -> {
            caption = stringResource(UiStrings.redesign_mode_disconnected); secondLine = remaining; tint = colors.disconnected
        }

        mode == RM.Mode.DISABLED_LOOP             -> {
            caption = stringResource(UiStrings.redesign_mode_disabled); tint = colors.disabled
        }

        // Superbolus, or no mode known yet: the stock text.
        else                                      -> {
            caption = state.runningModeText; tint = if (mode == RM.Mode.SUPER_BOLUS) colors.superBolus else colors.neutral
        }
    }
    // Step 7 (decided 9 Nov): the icon only, the colour tells the mode. The words stay for the screen reader:
    // "Loop: Paused, 25 min", and "a suggestion waits" with the badge.
    val modeText = if (secondLine.isNotEmpty()) stringResource(UiStrings.redesign_loop_mode_time, caption, secondLine) else caption
    val description =
        if (state.loopSuggestion) stringResource(UiStrings.redesign_loop_slot_suggestion, modeText)
        else stringResource(UiStrings.redesign_loop_slot, modeText)
    val canChangeMode = commandsAllowed && role.allows(ElementType.RUNNING_MODE.role)
    val openModeDialog = { onNavigate(NavigationRequest.Element(ElementType.RUNNING_MODE)) }
    RedesignRoundSlot(
        tint = tint,
        icon = mode?.toIcon() ?: ElementType.RUNNING_MODE.icon(),
        iconSize = LOOP_ICON_SIZE,
        caption = "",
        badge = if (state.loopSuggestion) "1" else null,
        description = description,
        // The full stock text (mode and time left), as on the chip.
        stateDescription = state.runningModeText.takeIf { !state.loopSuggestion && it.isNotEmpty() },
        enabled = state.loopSuggestion || canChangeMode,
        // Step 7 (decided 12 Nov): with a suggestion waiting, a tap opens the stock sheet to accept it (it asks
        // before anything is sent) and a long press changes the mode. Otherwise a tap changes the mode.
        onClick = if (state.loopSuggestion) onLoopSuggestionClick else openModeDialog,
        onLongClick = if (state.loopSuggestion && canChangeMode) openModeDialog else null,
        onLongClickLabel = stringResource(UiStrings.redesign_loop_change_mode),
        modifier = modifier
    )
}

/**
 * The Target slot: the target range, neutral by default. A temporary target shows its icon, the value
 * and the time left in orange; a target the loop adjusted keeps the colour of the stock chip. A tap
 * does what the Target tile did.
 */
@Composable
fun RedesignTargetSlot(
    state: RedesignTilesState,
    commandsAllowed: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = redesignColors()
    val role = LocalClientRole.current
    val data = state.data
    val active = state.tempTargetState == TempTargetChipState.Active
    val tint = when (state.tempTargetState) {
        TempTargetChipState.Active   -> colors.targetTemp
        // Same colour as the stock chip uses for a target the loop has adjusted.
        TempTargetChipState.Adjusted -> AapsTheme.generalColors.adjusted.let { RedesignTint(it.copy(alpha = 0.2f).compositeOver(colors.surface), it.copy(alpha = 0.5f), it) }
        TempTargetChipState.None     -> colors.neutral
    }
    val value = data.targetText.ifEmpty { state.tempTargetText }.ifEmpty { "Ã¢â‚¬â€" }
    val timeLeft = if (active) data.targetTimeLeft else ""
    RedesignRoundSlot(
        tint = tint,
        icon = if (active) state.tempTargetReason.toIcon() else IcTarget,
        iconSize = TARGET_ICON_SIZE,
        caption = value,
        secondLine = timeLeft,
        description = if (timeLeft.isNotEmpty()) stringResource(UiStrings.redesign_target_description_left, value, timeLeft)
        else stringResource(UiStrings.redesign_target_description, value),
        stateDescription = if (state.tempTargetState == TempTargetChipState.Adjusted) stringResource(UiStrings.a11y_tt_adjusted) else null,
        enabled = commandsAllowed && role.allows(ElementType.TEMP_TARGET_MANAGEMENT.role),
        onClick = { onNavigate(NavigationRequest.Element(ElementType.TEMP_TARGET_MANAGEMENT)) },
        modifier = modifier
    )
}

private fun TT.Reason?.toIcon(): ImageVector = when (this) {
    TT.Reason.EATING_SOON  -> IcTtEatingSoon
    TT.Reason.ACTIVITY     -> IcTtActivity
    TT.Reason.HYPOGLYCEMIA -> IcTtHypo
    else                   -> IcTtManual
}

/** Size of the round Loop and Target slots. */
val ROUND_SLOT_SIZE = 84.dp
private val LOOP_ICON_SIZE = 36.dp
private val TARGET_ICON_SIZE = 32.dp
private val BADGE_SIZE = 18.dp
private val BADGE_RING = 2.dp
