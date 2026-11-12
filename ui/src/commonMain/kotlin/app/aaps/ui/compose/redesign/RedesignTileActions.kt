package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.interfaces.InterfacesStrings
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.interfaces.ClientRole
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
 * The same rules as the stock sheets: Treatments › Insulin needs a master or a paired client and the
 * role for insulin. Manage › Temp basal (or its Cancel item while one runs) also needs a pump that can
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
    // The insulin colour, as for "IOB" in the bar above.
    val insulin = ElementType.INSULIN.color()
    RedesignInfoLine(
        text = stringResource(UiStrings.redesign_iob_line, bolus, basal, total),
        values = listOf(bolus to insulin, basal to insulin, total to insulin)
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
    val basal = redesignColors().tempBasal.accent
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            data.tempBasalRunning && data.tempBasalLine.isNotEmpty() ->
                RedesignInfoLine(data.tempBasalLine, listOf(data.tempBasalRate to basal))

            data.basalRate.isNotEmpty()                              ->
                RedesignInfoLine(stringResource(UiStrings.redesign_profile_basal, data.basalRate), listOf(data.basalRate to basal))
        }
        if (showCancel) {
            val description = stringResource(UiStrings.redesign_cancel_temp_basal_description)
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = description }
            ) {
                Text(stringResource(UiStrings.redesign_cancel_temp_basal))
            }
        }
    }
}

/**
 * A single line in the look of the BG / IOB / COB bar above it (`DialogStatusBar`): light block, small
 * text, labels in grey and the [values] in their colour. It gets smaller down to 11 sp, then ends with
 * "…" (SPEC 3.2.2).
 *
 * [text] is the whole translated line. The values are only found in it to colour them; the text is
 * never cut or joined, so a translation may put them in any order.
 */
@Composable
private fun RedesignInfoLine(text: String, values: List<Pair<String, Color>>) {
    val style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicText(
            text = coloredValues(text, values),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = style.fontSize)
        )
    }
}

/** [text] with each of [values] in its colour, where it is found. Two equal values get one place each. */
internal fun coloredValues(text: String, values: List<Pair<String, Color>>): AnnotatedString {
    val used = mutableListOf<IntRange>()
    return buildAnnotatedString {
        append(text)
        for ((value, color) in values) {
            if (value.isEmpty()) continue
            var start = text.indexOf(value)
            while (start >= 0 && used.any { start in it || start + value.length - 1 in it }) start = text.indexOf(value, start + 1)
            if (start < 0) continue
            used += start until start + value.length
            addStyle(SpanStyle(color = color), start, start + value.length)
        }
    }
}
