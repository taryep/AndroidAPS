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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.aaps.core.data.model.RM
import app.aaps.core.data.model.TT
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.interfaces.overview.graph.TbrState
import app.aaps.core.ui.compose.LocalClientRole
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.navigation.icon
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.main.TempTargetChipState
import app.aaps.ui.compose.overview.chips.CobUiState
import app.aaps.ui.compose.overview.chips.IobUiState
import app.aaps.ui.compose.overview.chips.toIcon

/** What the tiles and the round slots show. Everything comes from the stock overview state and [RedesignHomeData]. */
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
 * The 2 × 2 tile grid of the home page (SPEC 3.2): Profile and Carbs, Active insulin and Basal. Each tap
 * does what the matching stock chip does, with the same limits on a client. Loop and Target are the
 * round slots of the circle row.
 */
@Composable
fun RedesignTiles(
    state: RedesignTilesState,
    commandsAllowed: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    onIobClick: () -> Unit,
    onBasalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = redesignColors()
    val role = LocalClientRole.current
    // The tiles are about 150 dp wide: their text grows with the system font only up to 1.2 times,
    // then gets smaller before it ends with "...".
    RedesignFontScaleCap(TILE_MAX_FONT_SCALE) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
        Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
            ProfileTile(state, colors, Modifier.weight(1f)) { onNavigate(NavigationRequest.Element(ElementType.PROFILE_MANAGEMENT)) }
            // Carbs entry, with the block of the Treatments item: nothing on a client that is not paired or has no role for it.
            CarbsTile(
                state, colors, Modifier.weight(1f),
                onClick = if (commandsAllowed && role.allows(ElementType.CARBS.role)) ({ onNavigate(NavigationRequest.Element(ElementType.CARBS)) }) else null
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
            InsulinTile(state, colors, Modifier.weight(1f), onIobClick)
            BasalTile(state, colors, Modifier.weight(1f), onBasalClick)
        }
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
        modifier = modifier,
        horizontalPadding = 14.dp
    )
}

@Composable
private fun InsulinTile(state: RedesignTilesState, colors: RedesignColors, modifier: Modifier, onClick: () -> Unit) {
    val tinted = state.iob.iobTotal >= REDESIGN_INSULIN_TINT_THRESHOLD_U
    RedesignTile(
        label = stringResource(UiStrings.redesign_tile_active_insulin),
        value = state.iob.text,
        icon = ElementType.INSULIN.icon(),
        tint = if (tinted) colors.insulin else colors.neutral,
        tinted = tinted,
        onClick = onClick,
        modifier = modifier,
        horizontalPadding = 14.dp
    )
}

@Composable
private fun CarbsTile(state: RedesignTilesState, colors: RedesignColors, modifier: Modifier, onClick: (() -> Unit)?) {
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
        onClick = onClick,
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
    enabled: Boolean = true,
    horizontalPadding: Dp = 10.dp
) {
    val colors = redesignColors()
    val shape = RoundedCornerShape(TILE_RADIUS)
    val border = BorderStroke(if (strongBorder) 1.5.dp else 1.dp, tint.border)
    val semantics = Modifier.semantics(mergeDescendants = true) {
        if (stateDescription != null) this.stateDescription = stateDescription
        if (onClick != null) role = Role.Button
    }
    val content: @Composable () -> Unit = {
        // The icon is as high as the two lines together, on their left. The two lines are centred in the space
        // right of the icon (step 2, decided 10 Nov).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint.accent, modifier = Modifier.size(TILE_ICON_SIZE))
            Spacer(Modifier.width(6.dp))
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
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
private val TILE_GAP = 16.dp
private val TILE_RADIUS = 22.dp
private val TILE_ICON_SIZE = 24.dp

private const val TILE_MAX_FONT_SCALE = 1.2f
