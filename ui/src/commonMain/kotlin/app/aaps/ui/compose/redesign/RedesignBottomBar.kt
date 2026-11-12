package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aaps.core.interfaces.navigation.ElementType
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.core.ui.compose.LocalClientRole
import app.aaps.core.ui.compose.icons.IcAutomation
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings

/**
 * The bottom bar of the redesign: Treatments, Scenes, Manage, Graphs and Menu. The stock `MainNavigationBar` is
 * not used with the redesign, so it stays as in 4.0.
 *
 * Pump set-up, objectives, permissions, the BG source warning and the loop suggestion are not here: they are in
 * the Menu (see [redesignAttentionItems]) or beside the glucose circle.
 *
 * @param masterOrPairedClient false hides Treatments and Scenes (an unpaired client cannot send them)
 * @param automationTotal scenes and automation items defined; the Scenes item shows when it is above 0
 * @param automationCount the items the user can start now; shown as the badge of the Scenes item
 * @param graphsSelected true while the graphs page is the chosen item
 * @param menuSelected true while the Menu is the chosen item
 * @param menuBadge true when the Menu holds something to set up
 */
@Composable
fun RedesignBottomBar(
    onTreatmentClick: () -> Unit,
    onAutomationClick: () -> Unit,
    onManageClick: () -> Unit,
    onGraphsClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    masterOrPairedClient: Boolean = true,
    quickWizardCount: Int = 0,
    automationTotal: Int = 0,
    automationCount: Int = 0,
    graphsSelected: Boolean = false,
    menuSelected: Boolean = false,
    menuBadge: Boolean = false
) {
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    // A client whose role cannot use what a button opens does not get the button: the master would refuse it.
    val role = LocalClientRole.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = WindowInsets(0),
        modifier = modifier
    ) {
        if (masterOrPairedClient && role.allows(ElementType.TREATMENT.role)) {
            NavigationBarItem(
                selected = false,
                modifier = Modifier.badgeState(
                    if (quickWizardCount > 0) stringResource(UiStrings.a11y_badge_available, quickWizardCount) else null
                ),
                onClick = onTreatmentClick,
                icon = {
                    BadgedBox(
                        badge = {
                            if (quickWizardCount > 0) {
                                Badge(containerColor = AapsTheme.generalColors.statusNormal, contentColor = Color.Black) {
                                    Text(text = quickWizardCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fastfood,
                            contentDescription = stringResource(CoreUiStrings.treatments),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text(text = stringResource(CoreUiStrings.treatments), textAlign = TextAlign.Center) },
                colors = navColors
            )
        }

        // Visible whenever scenes or automation items exist. The badge counts only what can be started now.
        if (masterOrPairedClient && role.allows(ElementType.SCENE.role) && automationTotal > 0) {
            NavigationBarItem(
                selected = false,
                modifier = Modifier.badgeState(
                    if (automationCount > 0) stringResource(UiStrings.a11y_badge_available, automationCount) else null
                ),
                onClick = onAutomationClick,
                icon = {
                    BadgedBox(
                        badge = {
                            if (automationCount > 0) {
                                Badge(containerColor = AapsTheme.generalColors.statusNormal, contentColor = Color.Black) {
                                    Text(text = automationCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = IcAutomation,
                            contentDescription = stringResource(CoreUiStrings.scenes),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text(text = stringResource(CoreUiStrings.scenes), textAlign = TextAlign.Center) },
                colors = navColors
            )
        }

        NavigationBarItem(
            selected = false,
            onClick = onManageClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.ManageAccounts,
                    contentDescription = stringResource(CoreUiStrings.manage)
                )
            },
            label = { Text(text = stringResource(CoreUiStrings.manage), textAlign = TextAlign.Center) },
            colors = navColors
        )

        RedesignGraphsNavigationItem(selected = graphsSelected, onClick = onGraphsClick, colors = navColors)
        RedesignMenuNavigationItem(selected = menuSelected, badge = menuBadge, onClick = onMenuClick, colors = navColors)
    }
}

/**
 * Speaks what a badge shows (the badge is inside a slot that Material 3 removes from the semantics), or changes
 * nothing when there is no badge.
 */
private fun Modifier.badgeState(state: String?): Modifier =
    if (state != null) semantics { stateDescription = state } else this

/**
 * Places the bottom bar at the bottom of the screen, over the content, with its colours (RedesignBottomBarStyle).
 * The place is set here, outside the style wrapper (which has its own Box), so `align` works on the screen's Box.
 *
 * @param bottomInset space under the bar (the system navigation bar)
 * @param onHeightChange the measured height of the bar, for the padding of the content above it
 */
@Composable
fun BoxScope.RedesignBottomBarArea(
    bottomInset: Dp,
    onHeightChange: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = bottomInset)
            .onSizeChanged { if (it.height > 0) onHeightChange(it.height) }
    ) {
        RedesignBottomBarStyle(enabled = true, content = content)
    }
}
