package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.interfaces.InterfacesStrings
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.interfaces.ClientRole
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.LocalClientRole
import app.aaps.core.ui.compose.dialogs.ElementConfirmationDialog
import app.aaps.core.ui.compose.metroViewModel
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.navigation.color
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.manageSheet.ManageUiState
import app.aaps.ui.compose.manageSheet.ManageViewModel

/** What a tap on the Active insulin and Basal tiles does (SPEC 3.2.2 and 3.2.3). */
@Immutable
class RedesignTileTaps(val onIobClick: () -> Unit, val onBasalClick: () -> Unit)

/**
 * The tiles open the insulin screen and the temp basal screen, through [onNavigate] so the stock
 * protection (bolus PIN) is asked first. When the stock app would not offer that screen here (a client
 * that is not paired, a role that does not allow it, a pump that cannot set a temp basal now), the tap
 * shows the stock information dialog instead, as before.
 */
internal fun redesignTileTaps(
    insulinAllowed: Boolean,
    tempBasalAllowed: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    onIobInfo: () -> Unit,
    onTbrInfo: () -> Unit
) = RedesignTileTaps(
    onIobClick = { if (insulinAllowed) onNavigate(NavigationRequest.Element(ElementType.INSULIN)) else onIobInfo() },
    onBasalClick = { if (tempBasalAllowed) onNavigate(NavigationRequest.Element(ElementType.TEMP_BASAL)) else onTbrInfo() }
)

/**
 * The same rules as the stock sheets: Treatments â€º Insulin needs a master or a paired client and the
 * role for insulin. Manage â€º Temp basal (or its Cancel item while one runs) also needs a pump that can
 * set a temp basal now: [tempBasalOffered], from [ManageUiState].
 */
internal fun redesignTileRules(masterOrPairedClient: Boolean, role: ClientRole, tempBasalOffered: Boolean): Pair<Boolean, Boolean> =
    (masterOrPairedClient && role.allows(ElementType.INSULIN.role)) to
        (masterOrPairedClient && tempBasalOffered && role.allows(ElementType.TEMP_BASAL.role))

/** True when Manage shows "Temp basal" or its Cancel item. */
internal fun ManageUiState.tempBasalOffered(): Boolean = showMutatingActions && (showTempBasal || showCancelTempBasal)

@Composable
fun rememberRedesignTileTaps(
    manageViewModel: ManageViewModel,
    masterOrPairedClient: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    onIobInfo: () -> Unit,
    onTbrInfo: () -> Unit
): RedesignTileTaps {
    val manage by manageViewModel.uiState.collectAsStateWithLifecycle()
    val (insulinAllowed, tempBasalAllowed) = redesignTileRules(masterOrPairedClient, LocalClientRole.current, manage.tempBasalOffered())
    return remember(insulinAllowed, tempBasalAllowed, onNavigate, onIobInfo, onTbrInfo) {
        redesignTileTaps(insulinAllowed, tempBasalAllowed, onNavigate, onIobInfo, onTbrInfo)
    }
}

/**
 * The settings of the Treatments sheet (its gear) without the "show carbs button" switch: with the
 * redesign the sheet has no carbs entry whatever it says (SPEC 3.2.4). The switch stays in Settings.
 */
fun PreferenceSubScreenDef.withoutCarbsButton(): PreferenceSubScreenDef = copy(items = items - BooleanKey.OverviewShowCarbsButton)

/** One line on top of the insulin screen (SPEC 3.2.2): "Bolus 1.50 U + Basal -1.30 U = 0.20 U". */
@Composable
fun RedesignIobLine(viewModel: RedesignHomeViewModel = metroViewModel()) {
    val parts by viewModel.iobParts.collectAsStateWithLifecycle()
    parts?.let { RedesignIobLineContent(it) }
}

@Composable
internal fun RedesignIobLineContent(parts: RedesignIobParts) {
    val bolus = stringResource(InterfacesStrings.format_insulin_units, parts.bolus)
    val basal = stringResource(InterfacesStrings.format_insulin_units, parts.basal)
    val total = stringResource(InterfacesStrings.format_insulin_units, parts.total)
    // As in the mockup (sheet-insulin.png): the two parts in semi-bold, the total in the insulin colour.
    val insulin = ElementType.INSULIN.color()
    val strong = SpanStyle(fontWeight = FontWeight.SemiBold)
    RedesignInfoLine(
        badge = stringResource(CoreUiStrings.iob),
        accent = insulin,
        text = stringResource(UiStrings.redesign_iob_line, bolus, basal, total),
        values = listOf(bolus to strong, basal to strong, total to strong.copy(color = insulin))
    )
}

/**
 * One line on top of the temp basal screen (SPEC 3.2.3): the data of the stock "Temp basal" dialog
 * while one runs ("0.00 U/h @15:58 5/60 min"), or the profile basal. While a temp basal runs, a button cancels it with the stock
 * cancel of Manage (the master prepares it, the user confirms its lines, then it is applied) and then
 * closes the screen with [onCancelled]. A failed cancel goes to [onError], like in Manage.
 */
@Composable
fun RedesignTbrLine(
    onCancelled: () -> Unit,
    onError: (String) -> Unit,
    homeViewModel: RedesignHomeViewModel = metroViewModel(),
    manageViewModel: ManageViewModel = metroViewModel()
) {
    val data by homeViewModel.data.collectAsStateWithLifecycle()
    val manage by manageViewModel.uiState.collectAsStateWithLifecycle()
    var confirmation by remember { mutableStateOf<ManageViewModel.SideEffect.ShowConfirmation?>(null) }

    LaunchedEffect(Unit) {
        manageViewModel.sideEffect.collect { effect ->
            when (effect) {
                is ManageViewModel.SideEffect.ShowConfirmation -> confirmation = effect
                is ManageViewModel.SideEffect.ShowError        -> onError(effect.comment)
            }
        }
    }

    confirmation?.let { c ->
        ElementConfirmationDialog(
            elementType = c.elementType,
            lines = c.lines,
            onConfirm = {
                manageViewModel.commitCancel(c.bolusId, c.elementType, c.label)
                confirmation = null
                onCancelled()
            },
            onDismiss = { confirmation = null }
        )
    }

    RedesignTbrLineContent(
        data = data,
        showCancel = data.tempBasalRunning && manage.showCancelTempBasal,
        onCancelClick = { manageViewModel.cancelTempBasal() }
    )
}

@Composable
internal fun RedesignTbrLineContent(data: RedesignHomeData, showCancel: Boolean, onCancelClick: () -> Unit) {
    // Our teal of the Basal tile: the stock temp basal colour is a light cyan that cannot be read on a
    // light background (to fix in phase 9).
    val colors = redesignColors()
    val basal = colors.tempBasal.accent
    val rate = SpanStyle(color = basal, fontWeight = FontWeight.SemiBold)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            // As in the mockup (sheet-basal.png): badge "TBR", the rate in the basal colour, the rest plain.
            data.tempBasalRunning && data.tempBasalLine.isNotEmpty() ->
                RedesignInfoLine(stringResource(UiStrings.redesign_badge_tbr), basal, data.tempBasalLine, listOf(data.tempBasalRate to rate))

            data.basalRate.isNotEmpty()                              ->
                RedesignInfoLine(stringResource(CoreUiStrings.basal), basal, stringResource(UiStrings.redesign_profile_basal, data.basalRate), listOf(data.basalRate to rate))
        }
        if (showCancel) {
            // Outlined in the danger colour, with a cross, as in the mockup.
            val description = stringResource(UiStrings.redesign_cancel_temp_basal_description)
            OutlinedButton(
                onClick = onCancelClick,
                border = BorderStroke(1.dp, colors.danger),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = description }
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(UiStrings.redesign_cancel_temp_basal), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * A single line in a rounded card with a border, as in the mockups (sheet-insulin.png, sheet-basal.png):
 * a small [badge] ("IOB", "TBR") in the [accent] colour, then the [text], left-aligned, with the [values]
 * in their style. It gets smaller down to 11 sp, then ends with "â€¦" (SPEC 3.2.2).
 *
 * [text] is the whole translated line. The values are only found in it to style them; the text is
 * never cut or joined, so a translation may put them in any order.
 */
@Composable
private fun RedesignInfoLine(badge: String, accent: Color, text: String, values: List<Pair<String, SpanStyle>>) {
    val colors = redesignColors()
    val style = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary, fontFeatureSettings = "tnum")
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(INFO_LINE_RADIUS),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = badge,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(BADGE_RADIUS))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(BADGE_RADIUS))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            BasicText(
                text = styledValues(text, values),
                modifier = Modifier.weight(1f),
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = style.fontSize)
            )
        }
    }
}

private val INFO_LINE_RADIUS = 16.dp
private val BADGE_RADIUS = 8.dp

/** [text] with each of [values] in its colour, where it is found. Two equal values get one place each. */
internal fun coloredValues(text: String, values: List<Pair<String, Color>>): AnnotatedString =
    styledValues(text, values.map { (value, color) -> value to SpanStyle(color = color) })

/** [text] with each of [values] in its style, where it is found. Two equal values get one place each. */
internal fun styledValues(text: String, values: List<Pair<String, SpanStyle>>): AnnotatedString {
    val used = mutableListOf<IntRange>()
    return buildAnnotatedString {
        append(text)
        for ((value, spanStyle) in values) {
            if (value.isEmpty()) continue
            var start = text.indexOf(value)
            while (start >= 0 && used.any { start in it || start + value.length - 1 in it }) start = text.indexOf(value, start + 1)
            if (start < 0) continue
            used += start until start + value.length
            addStyle(spanStyle, start, start + value.length)
        }
    }
}
