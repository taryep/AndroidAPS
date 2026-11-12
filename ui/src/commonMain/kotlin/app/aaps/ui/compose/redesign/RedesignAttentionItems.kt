package app.aaps.ui.compose.redesign

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.core.ui.compose.icons.Pump
import app.aaps.core.ui.compose.navigation.NavigationRequest
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings

/**
 * One item that needs the user, shown at the top of the Menu.
 *
 * @param stateDescription what the badge means, spoken by the screen reader
 * @param badge the badge the item had in the bottom bar
 * @param needsAction false for a progress that is not a problem (Objectives): it does not light the dot on the Menu item
 */
class RedesignAttentionItem(
    val label: String,
    val icon: ImageVector,
    val stateDescription: String,
    val badge: @Composable () -> Unit,
    val needsAction: Boolean = true,
    val onClick: () -> Unit
)

/**
 * The items the redesign moves from the bottom bar to the Menu, so the bar keeps at most five items
 * (Treatments, Scenes, Manage, Graphs, Menu). They keep the label, icon, badge and action they have
 * in `MainNavigationBar`, and appear under the same conditions. Order: Objectives (a progress, so
 * first and quiet), then the most urgent. The BG source warning is not here: it is beside the
 * glucose circle on the home page. An open loop suggestion is not here either: it is the Loop tile.
 *
 * @param objectivesSetupPlugin the Objectives plugin while some objective is not done, else null
 * @param objectivesProgressText the badge text, e.g. "9/10"
 * @param objectivesProgressDescription the progress in words, spoken by the screen reader
 * @param pumpSetupPlugin the pump to set up, or null when there is nothing to do
 * @param permissionsMissing true when some permission is missing
 */
@Composable
fun redesignAttentionItems(
    objectivesSetupPlugin: PluginBase?,
    objectivesProgressText: String?,
    objectivesProgressDescription: String?,
    pumpSetupPlugin: PluginBase?,
    permissionsMissing: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    onPermissionsClick: () -> Unit
): List<RedesignAttentionItem> {
    val actionNeeded = stringResource(UiStrings.a11y_badge_attention)
    val warningBadge: @Composable () -> Unit = { Badge { Text("!") } }
    val objectivesIcon = objectivesSetupPlugin?.pluginDescription?.icon
    return buildList {
        if (objectivesSetupPlugin != null && objectivesIcon != null) {
            add(
                RedesignAttentionItem(
                    label = objectivesSetupPlugin.pluginDescription.pluginName?.let { stringResource(it) } ?: objectivesSetupPlugin.name,
                    icon = objectivesIcon,
                    // "9 of 10 done", so a screen reader says why the row is there at all.
                    stateDescription = objectivesProgressDescription ?: actionNeeded,
                    badge = {
                        Badge(containerColor = AapsTheme.generalColors.statusWarning, contentColor = Color.Black) { Text(objectivesProgressText ?: "!") }
                    },
                    onClick = { onNavigate(NavigationRequest.Plugin(objectivesSetupPlugin::class.simpleName.orEmpty())) },
                    // A progress that lasts for months must not keep the Menu dot on.
                    needsAction = false
                )
            )
        }
        if (pumpSetupPlugin != null) {
            add(
                RedesignAttentionItem(
                    label = pumpSetupPlugin.pluginDescription.pluginName?.let { stringResource(it) } ?: pumpSetupPlugin.name,
                    icon = pumpSetupPlugin.pluginDescription.icon ?: Pump,
                    stateDescription = actionNeeded,
                    badge = warningBadge,
                    onClick = { onNavigate(NavigationRequest.Plugin(pumpSetupPlugin::class.simpleName.orEmpty())) }
                )
            )
        }
        if (permissionsMissing) {
            add(
                RedesignAttentionItem(
                    label = stringResource(UiStrings.permission_sheet_title),
                    icon = Icons.Default.GppMaybe,
                    stateDescription = actionNeeded,
                    badge = warningBadge,
                    onClick = onPermissionsClick
                )
            )
        }
    }
}
