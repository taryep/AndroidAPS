package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
 */
class RedesignAttentionItem(
    val label: String,
    val icon: ImageVector,
    val stateDescription: String,
    val badge: @Composable () -> Unit,
    val onClick: () -> Unit
)

/**
 * The items the redesign moves from the bottom bar to the Menu, so the bar keeps at most five items
 * (Treatments, Scenes, Manage, Objectives, Menu). They keep the label, icon, badge and action they
 * have in `MainNavigationBar`, and appear under the same conditions. Order: the most urgent first.
 *
 * @param bgSetupPlugin the BG source with a data quality problem (flat or doubled values), or null
 * @param loopActionAvailable true when an open loop suggestion waits to be accepted
 * @param pumpSetupPlugin the pump to set up, or null when there is nothing to do
 * @param permissionsMissing true when some permission is missing
 */
@Composable
fun redesignAttentionItems(
    bgSetupPlugin: PluginBase?,
    bgQualityBadgeIcon: ImageVector?,
    bgQualityBadgeTint: Color,
    bgQualityBadgeDescription: String?,
    loopActionAvailable: Boolean,
    pumpSetupPlugin: PluginBase?,
    permissionsMissing: Boolean,
    onNavigate: (NavigationRequest) -> Unit,
    onLoopActionClick: () -> Unit,
    onPermissionsClick: () -> Unit
): List<RedesignAttentionItem> {
    val actionNeeded = stringResource(UiStrings.a11y_badge_attention)
    val warningBadge: @Composable () -> Unit = { Badge { Text("!") } }
    return buildList {
        // The bar shows this item only when the plugin has an icon; keep the same rule.
        val bgIcon = bgSetupPlugin?.pluginDescription?.icon
        if (bgSetupPlugin != null && bgIcon != null) {
            add(
                RedesignAttentionItem(
                    label = bgSetupPlugin.pluginDescription.pluginName?.let { stringResource(it) } ?: bgSetupPlugin.name,
                    icon = bgIcon,
                    stateDescription = bgQualityBadgeDescription ?: actionNeeded,
                    badge = {
                        if (bgQualityBadgeIcon != null) {
                            Badge(containerColor = Color.Transparent) {
                                // Decorative: its meaning is the state description.
                                Icon(imageVector = bgQualityBadgeIcon, contentDescription = null, tint = bgQualityBadgeTint, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Badge { Text("!") }
                        }
                    },
                    onClick = { onNavigate(NavigationRequest.Plugin(bgSetupPlugin::class.simpleName.orEmpty())) }
                )
            )
        }
        if (loopActionAvailable) {
            add(
                RedesignAttentionItem(
                    // Not the bar label ("Loop"): in a list the row must say what waits for the user.
                    label = stringResource(UiStrings.redesign_loop_suggestion),
                    icon = Icons.Default.PlayCircle,
                    // One suggestion is waiting, as the constant "1" on the badge says.
                    stateDescription = stringResource(UiStrings.a11y_badge_available, 1),
                    badge = {
                        Badge(containerColor = AapsTheme.generalColors.statusNormal, contentColor = Color.Black) { Text("1") }
                    },
                    onClick = onLoopActionClick
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
