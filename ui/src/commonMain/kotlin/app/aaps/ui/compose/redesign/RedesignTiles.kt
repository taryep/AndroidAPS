package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TT
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.interfaces.overview.graph.TbrState
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
import app.aaps.ui.compose.overview.chips.CobUiState
import app.aaps.ui.compose.overview.chips.IobUiState
import app.aaps.ui.compose.overview.chips.toIcon

/** What the six tiles show. Everything comes from the stock overview state and [RedesignHomeData]. */
class RedesignTilesState(
    val profileName: String,
    val isProfileModified: Boolean,
    val tempTargetText: String,
    val tempTargetState: TempTargetChipState,
    val tempTargetReason: TT.Reason?,
    val runningMode: RM.Mode?,
    val runningModeText: String,
    val runningModeRemaining: String,
    val tbrState: TbrState,
    val iob: IobUiState,
    val cob: CobUiState,
    val data: RedesignHomeData,
    val loopSuggestion: Boolean
)

/**
 * The 2 × 3 tile grid of the home page (SPEC 3.2). Each tap does what the matching stock chip does,
 * with the same limits on a client.
 */
@Composable
fun RedesignTiles(
    state: RedesignTilesState,
    commandsAllowed: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    onIobClick: () -> Unit,
    onBasalClick: () -> Unit,
    onLoopSuggestionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = redesignColors()
    val role = LocalClientRole.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
        Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
            ProfileTile(state, colors, Modifier.weight(1f)) { onNavigate(NavigationRequest.Element(ElementType.PROFILE_MANAGEMENT)) }
            TargetTile(
                state, colors, Modifier.weight(1f),
                enabled = commandsAllowed && role.allows(ElementType.TEMP_TARGET_MANAGEMENT.role)
            ) { onNavigate(NavigationRequest.Element(ElementType.TEMP_TARGET_MANAGEMENT)) }
            LoopTile(
                state, colors, Modifier.weight(1f),
                enabled = state.loopSuggestion || (commandsAllowed && role.allows(ElementType.RUNNING_MODE.role)),
                onClick = if (state.loopSuggestion) onLoopSuggestionClick else ({ onNavigate(NavigationRequest.Element(ElementType.RUNNING_MODE)) })
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
            InsulinTile(state, colors, Modifier.weight(1f), onIobClick)
            CarbsTile(state, colors, Modifier.weight(1f))
            BasalTile(state, colors, Modifier.weight(1f), onBasalClick)
        }
    }
}

@Composable
private fun ProfileTile(state: RedesignTilesState, colors: RedesignColors, modifier: Modifier, onClick: () -> Unit) {
    val label = stringResource(UiStrings.redesign_tile_profile)
    val data = state.data
    // The view model has the name without the time left; the stock text is the fallback before it loads.
    // The name already has the percentage in it, e.g. "Normal (90%)", so it is not shown a second time.
    val name = data.profileName.ifEmpty { state.profileName }
    RedesignTile(
        label = label,
        labelTime = data.profileTimeLeft,
        value = name,
        icon = ElementType.PROFILE_MANAGEMENT.icon(),
        tint = if (state.isProfileModified) colors.profileTemp else colors.neutral,
        tinted = state.isProfileModified,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TargetTile(state: RedesignTilesState, colors: RedesignColors, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(UiStrings.redesign_tile_target)
    val data = state.data
    val tint = when (state.tempTargetState) {
        TempTargetChipState.Active   -> colors.targetTemp
        // Same colour as the stock chip uses for a target the loop has adjusted.
        TempTargetChipState.Adjusted -> AapsTheme.generalColors.adjusted.let { RedesignTint(it.copy(alpha = 0.2f).compositeOver(colors.surface), it.copy(alpha = 0.5f), it) }
        TempTargetChipState.None     -> colors.neutral
    }
    RedesignTile(
        label = label,
        labelTime = if (state.tempTargetState == TempTargetChipState.Active) data.targetTimeLeft else "",
        value = data.targetText.ifEmpty { state.tempTargetText }.ifEmpty { "—" },
        icon = if (state.tempTargetState == TempTargetChipState.Active) state.tempTargetReason.toIcon() else IcTarget,
        tint = tint,
        tinted = state.tempTargetState != TempTargetChipState.None,
        stateDescription = if (state.tempTargetState == TempTargetChipState.Adjusted) stringResource(UiStrings.a11y_tt_adjusted) else null,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun LoopTile(state: RedesignTilesState, colors: RedesignColors, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val mode = state.runningMode
    val loop = stringResource(UiStrings.redesign_tile_loop)
    val remaining = state.runningModeRemaining
    val label: String
    var labelTime = ""
    val value: String
    val tint: RedesignTint
    when {
        // An open loop suggestion waits: the tile says so, and a tap opens the sheet to accept it.
        state.loopSuggestion                      -> {
            label = stringResource(UiStrings.redesign_tile_open_loop); value = stringResource(UiStrings.redesign_loop_suggestion_value); tint = colors.openLoop
        }

        mode == RM.Mode.CLOSED_LOOP || mode == RM.Mode.RESUME -> {
            label = loop; value = stringResource(UiStrings.redesign_mode_closed); tint = colors.closedLoop
        }

        mode == RM.Mode.OPEN_LOOP                 -> {
            label = loop; value = stringResource(UiStrings.redesign_mode_open); tint = colors.openLoop
        }

        mode == RM.Mode.CLOSED_LOOP_LGS           -> {
            label = loop; value = stringResource(UiStrings.redesign_mode_lgs); tint = colors.lgs
        }

        mode == RM.Mode.SUSPENDED_BY_USER || mode == RM.Mode.SUSPENDED_BY_PUMP || mode == RM.Mode.SUSPENDED_BY_DST -> {
            label = loop
            value = if (remaining.isNotEmpty()) stringResource(UiStrings.redesign_mode_paused, remaining) else stringResource(UiStrings.redesign_mode_paused_no_time)
            tint = colors.suspended
        }

        mode == RM.Mode.DISCONNECTED_PUMP         -> {
            label = stringResource(UiStrings.redesign_tile_pump)
            labelTime = remaining
            value = stringResource(UiStrings.redesign_mode_disconnected)
            tint = colors.disconnected
        }

        mode == RM.Mode.DISABLED_LOOP             -> {
            label = loop; value = stringResource(UiStrings.redesign_mode_disabled); tint = colors.disabled
        }

        // Superbolus, or no mode known yet: the stock text.
        else                                      -> {
            label = loop; value = state.runningModeText; tint = if (mode == RM.Mode.SUPER_BOLUS) colors.superBolus else colors.neutral
        }
    }
    RedesignTile(
        label = label,
        labelTime = labelTime,
        value = value,
        icon = mode?.toIcon() ?: ElementType.RUNNING_MODE.icon(),
        tint = tint,
        tinted = tint != colors.neutral,
        // The full stock text (mode and time left) for the screen reader, as on the chip.
        stateDescription = state.runningModeText.takeIf { !state.loopSuggestion && it.isNotEmpty() },
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun InsulinTile(state: RedesignTilesState, colors: RedesignColors, modifier: Modifier, onClick: () -> Unit) {
    val tinted = state.iob.iobTotal >= REDESIGN_INSULIN_TINT_THRESHOLD_U
    RedesignTile(
        label = stringResource(UiStrings.redesign_tile_insulin),
        value = state.iob.text,
        icon = ElementType.INSULIN.icon(),
        tint = if (tinted) colors.insulin else colors.neutral,
        tinted = tinted,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun CarbsTile(state: RedesignTilesState, colors: RedesignColors, modifier: Modifier) {
    val cob = state.cob
    val tint = when {
        cob.carbsReq > 0   -> colors.carbsRequired
        cob.cobValue > 0.0 -> colors.carbsActive
        else               -> colors.neutral
    }
    RedesignTile(
        label = stringResource(UiStrings.redesign_tile_carbs),
        value = cob.text,
        small = if (cob.carbsReq > 0) stringResource(UiStrings.redesign_carbs_required, cob.carbsReq) else "",
        icon = ElementType.COB.icon(),
        tint = tint,
        tinted = tint != colors.neutral,
        // A stronger border when carbs are required, as the SPEC asks.
        strongBorder = cob.carbsReq > 0,
        // The stock carbs chip has no action either.
        onClick = null,
        modifier = modifier
    )
}

@Composable
private fun BasalTile(state: RedesignTilesState, colors: RedesignColors, modifier: Modifier, onClick: () -> Unit) {
    val data = state.data
    val running = data.tempBasalRunning
    RedesignTile(
        label = if (running) stringResource(UiStrings.redesign_tile_temp_basal) else stringResource(UiStrings.redesign_tile_basal),
        labelTime = if (running) data.tempBasalTimeLeft else "",
        value = (if (running) data.tempBasalRate else data.basalRate).ifEmpty { "—" },
        small = if (running) data.tempBasalPercent else "",
        icon = state.tbrState.toIcon(),
        tint = if (running) colors.tempBasal else colors.neutral,
        tinted = running,
        onClick = onClick,
        modifier = modifier
    )
}

private fun TT.Reason?.toIcon(): ImageVector = when (this) {
    TT.Reason.EATING_SOON  -> IcTtEatingSoon
    TT.Reason.ACTIVITY     -> IcTtActivity
    TT.Reason.HYPOGLYCEMIA -> IcTtHypo
    else                   -> IcTtManual
}

/**
 * One tile: 48 dp, line 1 = icon and label, line 2 = value (and a small part after it). The value
 * never overflows: it gets smaller down to 11 sp first, and only then ends with "…".
 *
 * @param tinted the tile shows a state: its label takes the accent colour
 * @param onClick null for a tile with no action
 */
@Composable
fun RedesignTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: RedesignTint,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    labelTime: String = "",
    small: String = "",
    tinted: Boolean = false,
    strongBorder: Boolean = false,
    stateDescription: String? = null,
    enabled: Boolean = true
) {
    val colors = redesignColors()
    val shape = RoundedCornerShape(12.dp)
    val border = BorderStroke(if (strongBorder) 1.5.dp else 1.dp, tint.border)
    val semantics = Modifier.semantics(mergeDescendants = true) {
        if (stateDescription != null) this.stateDescription = stateDescription
        if (onClick != null) role = Role.Button
    }
    val content: @Composable () -> Unit = {
        // The icon is as high as the two lines together, on their left.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint.accent, modifier = Modifier.size(TILE_ICON_SIZE))
            Spacer(Modifier.width(6.dp))
            Column(verticalArrangement = Arrangement.Center) {
                // The time left follows the name after a space, in the accent colour and semi-bold: no separator sign.
                FittingText(
                    text = buildAnnotatedString {
                        append(label)
                        if (labelTime.isNotEmpty()) {
                            append(" ")
                            withStyle(SpanStyle(color = tint.accent, fontWeight = FontWeight.SemiBold)) { append(labelTime) }
                        }
                    },
                    style = TextStyle(fontSize = 11.sp, color = if (tinted) tint.accent else colors.textSecondary),
                    minSize = 9
                )
                val valueText = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(value) }
                    if (small.isNotEmpty()) {
                        append(" ")
                        withStyle(SpanStyle(fontSize = 0.8.em, color = colors.textSecondary)) { append(small) }
                    }
                }
                BasicText(
                    text = valueText,
                    style = TextStyle(fontSize = 14.sp, color = colors.textPrimary, fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp)
                )
            }
        }
    }
    val tileModifier = modifier
        .fillMaxWidth()
        .heightIn(min = TILE_HEIGHT)
        .then(semantics)
    if (onClick != null) {
        Surface(onClick = onClick, enabled = enabled, shape = shape, color = tint.background, border = border, modifier = tileModifier, content = content)
    } else {
        Surface(shape = shape, color = tint.background, border = border, modifier = tileModifier, content = content)
    }
}

/** One line that gets smaller (down to [minSize] sp) before it ends with "…". */
@Composable
private fun FittingText(text: AnnotatedString, style: TextStyle, minSize: Int) {
    BasicText(
        text = text,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = minSize.sp, maxFontSize = style.fontSize)
    )
}

private val TILE_HEIGHT = 48.dp
private val TILE_GAP = 6.dp
private val TILE_ICON_SIZE = 24.dp

