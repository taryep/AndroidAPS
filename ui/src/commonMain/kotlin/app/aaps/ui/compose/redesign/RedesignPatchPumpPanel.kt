package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.LocalClientRole
import app.aaps.core.ui.compose.StatusLevel
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.core.ui.compose.statusLevelToColor
import app.aaps.core.ui.compose.statusLevelToDescription
import app.aaps.core.ui.compose.stringResource
import app.aaps.core.ui.compose.stringResourceOrNull
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.overview.StatusLightsSettingsContent
import app.aaps.ui.compose.overview.statusLights.StatusItem

/**
 * Smallest reservoir size used for the level bar of a patch pump. No driver gives the real size
 * (`maxReservoirReading` is only a display limit: 50 on Omnipod and EOPatch2, the real size on Medtrum,
 * CareLevo and Equil), and every patch pump in the code holds at least 200 U.
 */
internal const val PATCH_RESERVOIR_MIN_CAPACITY_U = 200.0

/**
 * Part of the reservoir that is left, 0..1, or null when it is not known. A pump with a display limit
 * below the capacity (Omnipod: "50+") does not know the level once it reads the limit.
 */
internal fun reservoirFraction(units: Double, readingLimit: Double?): Float? {
    val limit = readingLimit ?: 0.0
    if (limit < PATCH_RESERVOIR_MIN_CAPACITY_U && units >= limit && limit > 0.0) return null
    val capacity = maxOf(limit, PATCH_RESERVOIR_MIN_CAPACITY_U)
    return (units / capacity).coerceIn(0.0, 1.0).toFloat()
}

/** The buttons of the open panel. Null means the button is not shown. */
internal data class PanelActions(
    val onSensorInsert: (() -> Unit)?,
    val onFill: (() -> Unit)?,
    val onInsulinChange: (() -> Unit)?,
    val onBatteryChange: (() -> Unit)?
)

/**
 * The open consumables panel for a patch pump (SPEC 3.5.1). Our own card, no title: the settings icon and the
 * collapse arrow are on the top right. Each row shows one thing: patch = time in use, insulin = reservoir left,
 * sensor = time in use ("time / longest time"). The pump battery keeps its row when the pump has one.
 * A tap on the patch row opens the pump's own screen, if it has one. The buttons follow the stock rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RedesignPatchPumpPanel(
    cannulaStatus: StatusItem?,
    insulinStatus: StatusItem?,
    sensorStatus: StatusItem?,
    batteryStatus: StatusItem?,
    showFill: Boolean,
    showPumpBatteryChange: Boolean,
    commandsAllowed: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    statusLightsDef: PreferenceSubScreenDef,
    onCollapse: () -> Unit,
    pumpHasMenu: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = redesignColors()
    // Each button also needs this phone's role to allow what it opens (the master would refuse it).
    val role = LocalClientRole.current
    val actions = PanelActions(
        onSensorInsert = if (commandsAllowed && role.allows(ElementType.SENSOR_INSERT.role)) {
            { onNavigate(NavigationRequest.Element(ElementType.SENSOR_INSERT)) }
        } else null,
        onFill = if (showFill && commandsAllowed && role.allows(ElementType.CANNULA_CHANGE.role)) {
            { onNavigate(NavigationRequest.Element(ElementType.CANNULA_CHANGE)) }
        } else null,
        onInsulinChange = if (showFill && commandsAllowed && role.allows(ElementType.FILL.role)) {
            { onNavigate(NavigationRequest.Element(ElementType.FILL)) }
        } else null,
        onBatteryChange = if (showPumpBatteryChange && commandsAllowed && role.allows(ElementType.BATTERY_CHANGE.role)) {
            { onNavigate(NavigationRequest.Element(ElementType.BATTERY_CHANGE)) }
        } else null
    )
    val onPumpClick: (() -> Unit)? = if (pumpHasMenu) {
        { onNavigate(NavigationRequest.Element(ElementType.PUMP)) }
    } else null
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(PANEL_RADIUS),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // The ∧ sits exactly where the ∨ of the closed bar is, so it does not jump when the panel opens.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PanelHeaderButton(
                    icon = Icons.Filled.Settings,
                    description = stringResource(CoreUiStrings.settings),
                    color = colors.textSecondary,
                    onClick = { showSettings = true }
                )
                PanelHeaderButton(
                    icon = Icons.Filled.ExpandLess,
                    description = stringResource(CoreUiStrings.collapse),
                    color = colors.textSecondary,
                    onClick = onCollapse,
                    modifier = Modifier.testTag(TAG_COLLAPSE)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)) {
                val rows = buildList<@Composable () -> Unit> {
                    cannulaStatus?.let {
                        add {
                            PanelRow(
                                item = it, value = ageText(it), valueColor = statusLevelToColor(it.ageStatus), progress = it.agePercent,
                                severity = it.ageStatus, colors = colors,
                                actionLabel = stringResource(CoreUiStrings.btn_status_cannula_change), onActionClick = actions.onFill,
                                onRowClick = onPumpClick,
                                modifier = Modifier.testTag(TAG_PATCH_ROW)
                            )
                        }
                    }
                    insulinStatus?.let {
                        add {
                            PanelRow(
                                item = it, value = it.level ?: "-", valueColor = statusLevelToColor(it.levelStatus),
                                progress = it.levelValue?.let { u -> reservoirFraction(u, it.reservoirReadingLimit) } ?: -1f,
                                severity = it.levelStatus, colors = colors,
                                actionLabel = stringResource(CoreUiStrings.btn_status_insulin_fill), onActionClick = actions.onInsulinChange,
                                modifier = Modifier.testTag(TAG_INSULIN_ROW)
                            )
                        }
                    }
                    sensorStatus?.let {
                        add {
                            PanelRow(
                                item = it, value = ageText(it), valueColor = statusLevelToColor(it.ageStatus), progress = it.agePercent,
                                severity = it.ageStatus, colors = colors,
                                onAddClick = actions.onSensorInsert,
                                modifier = Modifier.testTag(TAG_SENSOR_ROW)
                            )
                        }
                    }
                    batteryStatus?.let {
                        val showLevel = it.level != null && it.expandedLevel
                        add {
                            PanelRow(
                                item = it,
                                value = if (showLevel) it.level.orEmpty() else it.age,
                                valueColor = statusLevelToColor(if (showLevel) it.levelStatus else it.ageStatus),
                                progress = if (showLevel) it.levelPercent else it.agePercent,
                                severity = if (showLevel) it.levelStatus else it.ageStatus, colors = colors,
                                actionLabel = stringResource(CoreUiStrings.add), onActionClick = actions.onBatteryChange
                            )
                        }
                    }
                }
                rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(color = colors.border)
                    row()
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            StatusLightsSettingsContent(settingsDef = statusLightsDef)
        }
    }
}

/** "2d17h / 3d": the age and the longest age. Without a longest age, only the age. */
@Composable
private fun ageText(item: StatusItem): String =
    item.ageMax?.let { stringResource(UiStrings.redesign_value_of_max, item.age, it) } ?: item.age

/**
 * One row: icon and name, then the (+) or the ">" right after the name, then the optional stock button, then
 * one value with its bar. The value column has a fixed width, so every bar ends on the same right edge.
 */
@Composable
private fun PanelRow(
    item: StatusItem,
    value: String,
    valueColor: Color,
    progress: Float,
    severity: StatusLevel,
    colors: RedesignColors,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
    onRowClick: (() -> Unit)? = null
) {
    val severityText = stringResourceOrNull(statusLevelToDescription(if (severity == StatusLevel.UNSPECIFIED) StatusLevel.NORMAL else severity))
    val openMenu = stringResource(UiStrings.redesign_open_pump_menu)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .then(
                if (onRowClick != null) Modifier.clickable(onClickLabel = openMenu, onClick = onRowClick) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // What is only read is one node, as on the stock rows; the buttons stay separate focus stops.
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { if (severityText != null) contentDescription = severityText },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = item.icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = colors.textSecondary)
            // Name and (+) / ">" share ALL the free space, so the value column always ends on the same right edge.
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (onAddClick != null) AddButton(onAddClick)
                if (onRowClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = openMenu,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (actionLabel != null && onActionClick != null) {
                FilledTonalButton(
                    onClick = onActionClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .semantics { contentDescription = item.label }
                ) {
                    Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
            ValueWithBar(value = value, color = valueColor, progress = progress)
        }
    }
}

/** The small round (+) beside the "Sensor" name: the stock "Add" action, same confirmation. */
@Composable
private fun AddButton(onClick: () -> Unit) {
    val description = stringResource(UiStrings.redesign_add_sensor)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ADD_BUTTON_SIZE)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(TAG_ADD_SENSOR)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ValueWithBar(value: String, color: Color, progress: Float) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(VALUE_WIDTH)
    ) {
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color, maxLines = 1)
        if (progress >= 0) {
            LinearProgressIndicator(
                progress = { progress },
                // No semantics: the row already says what the bar measures (same reason as the stock rows).
                modifier = Modifier.width(VALUE_WIDTH).height(6.dp).clearAndSetSemantics { },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                // A plain bar: no gap between the filled part and the track, and no dot at the end.
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

internal const val TAG_PATCH_ROW = "redesign_patch_row"
internal const val TAG_INSULIN_ROW = "redesign_insulin_row"
internal const val TAG_SENSOR_ROW = "redesign_sensor_row"
internal const val TAG_ADD_SENSOR = "redesign_add_sensor"
internal const val TAG_COLLAPSE = "redesign_collapse"

private val PANEL_RADIUS = 16.dp
/** The touch area of the header buttons: 48 dp, the minimum size for a touch target. */
private val HEADER_TOUCH_SIZE = 48.dp
private val ROW_MIN_HEIGHT = 48.dp
private val VALUE_WIDTH = 88.dp
private val ADD_BUTTON_SIZE = 28.dp

/**
 * A header button of the open panel: a 48 dp touch area in the top right corner, with the icon drawn as on the
 * closed bar (a [CHEVRON_BOX_WIDTH] × [CHEVRON_BOX_HEIGHT] box at the right edge, centred on the height of
 * the bar). So the ∧ of the open panel is exactly where the ∨ of the closed bar is.
 */
@Composable
private fun PanelHeaderButton(icon: ImageVector, description: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier
            .size(HEADER_TOUCH_SIZE)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = (BAR_HEIGHT - CHEVRON_BOX_HEIGHT) / 2)
                .width(CHEVRON_BOX_WIDTH)
                .height(CHEVRON_BOX_HEIGHT)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(CHEVRON_ICON_SIZE))
        }
    }
}
