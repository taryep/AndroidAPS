package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.StatusLevel
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.core.ui.compose.statusLevelToColor
import app.aaps.core.ui.compose.statusLevelToDescription
import app.aaps.core.ui.compose.stringResource
import app.aaps.core.ui.compose.stringResourceOrNull
import app.aaps.ui.compose.overview.OverviewStatusSection
import app.aaps.ui.compose.overview.statusLights.StatusItem

/**
 * The consumables bar of the home page (SPEC 3.5). Collapsed: a 26 dp bar with the ages and levels
 * that the stock bar shows (same data, same warning and critical colours). Expanded: the stock panel,
 * unchanged, with its settings icon and its buttons.
 */
@Composable
fun RedesignConsumablesBar(
    sensorStatus: StatusItem?,
    insulinStatus: StatusItem?,
    cannulaStatus: StatusItem?,
    batteryStatus: StatusItem?,
    showFill: Boolean,
    showPumpBatteryChange: Boolean,
    commandsAllowed: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    statusLightsDef: PreferenceSubScreenDef,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (expanded) {
        OverviewStatusSection(
            sensorStatus = sensorStatus,
            insulinStatus = insulinStatus,
            cannulaStatus = cannulaStatus,
            batteryStatus = batteryStatus,
            showFill = showFill,
            showPumpBatteryChange = showPumpBatteryChange,
            commandsAllowed = commandsAllowed,
            onNavigate = onNavigate,
            statusLightsDef = statusLightsDef,
            expanded = true,
            onExpandedChange = onExpandedChange,
            modifier = modifier
        )
        return
    }
    val items = listOfNotNull(cannulaStatus, insulinStatus, sensorStatus, batteryStatus)
        .filter { it.compactAge || (it.compactLevel && it.level != null) }
    // Same rule as the stock bar: with nothing to show (and nothing to expand), no bar.
    if (listOfNotNull(cannulaStatus, insulinStatus, sensorStatus, batteryStatus).isEmpty()) return
    val colors = redesignColors()
    Surface(
        shape = RoundedCornerShape(BAR_HEIGHT / 2),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .clickable { onExpandedChange(true) }
    ) {
        // The bar is 26 dp high: its text does not grow with the system font (the open panel does).
        RedesignFontScaleCap(1f) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                items.forEach { ConsumableItem(it, colors) }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(44.dp).height(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = stringResource(CoreUiStrings.expand),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        }
    }
}

/** One consumable: icon, age and / or level, the values in the colour of their warning level. */
@Composable
private fun ConsumableItem(item: StatusItem, colors: RedesignColors) {
    val showAge = item.compactAge
    val showLevel = item.compactLevel && item.level != null
    val ageColor = statusLevelToColor(item.ageStatus)
    val levelColor = if (item.level != null) statusLevelToColor(item.levelStatus) else ageColor
    // The worse of the two levels is spoken, as on the stock bar.
    val worst = when {
        item.ageStatus == StatusLevel.CRITICAL || (showLevel && item.levelStatus == StatusLevel.CRITICAL) -> StatusLevel.CRITICAL
        item.ageStatus == StatusLevel.WARNING || (showLevel && item.levelStatus == StatusLevel.WARNING)   -> StatusLevel.WARNING
        else                                                                                              -> StatusLevel.NORMAL
    }
    val severity = stringResourceOrNull(statusLevelToDescription(worst))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { if (severity != null) contentDescription = severity }
    ) {
        Icon(imageVector = item.icon, contentDescription = item.label, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            text = buildAnnotatedString {
                if (showAge) withStyle(SpanStyle(color = ageColor)) { append(item.age) }
                if (showLevel) withStyle(SpanStyle(color = levelColor)) {
                    if (showAge) append(" ")
                    append(item.level)
                }
            },
            style = TextStyle(fontSize = 12.sp, color = colors.textPrimary, fontFeatureSettings = "tnum"),
            maxLines = 1
        )
    }
}

private val BAR_HEIGHT = 26.dp
