package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings

/**
 * The "Menu" item at the end of the bottom bar (SPEC section 2.3). Kept here so `MainNavigationBar`
 * only gains one call.
 *
 * It shows as selected while the Menu screen is open, and a second tap closes it. While it is open
 * a screen reader says what the tap will do ("close the menu..."), because "Menu, selected" alone
 * does not tell that the same button goes back.
 *
 * @param badge true when the Menu holds something to set up (pump, permissions). A dot is shown, and
 *   spoken as "action needed" like the other badges of the bar. While the Menu is open the state is
 *   "selected" instead: the items are then on the screen.
 */
@Composable
fun RowScope.RedesignMenuNavigationItem(
    selected: Boolean,
    badge: Boolean,
    onClick: () -> Unit,
    colors: NavigationBarItemColors
) {
    val label = stringResource(UiStrings.redesign_menu)
    val closeLabel = stringResource(UiStrings.redesign_close_menu)
    val actionNeeded = stringResource(UiStrings.a11y_badge_attention)
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = when {
            selected -> Modifier.semantics { onClick(label = closeLabel) { onClick(); true } }
            badge    -> Modifier.semantics { stateDescription = actionNeeded }
            else     -> Modifier
        },
        icon = {
            BadgedBox(badge = { if (badge) Badge() }) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = label)
            }
        },
        label = { Text(text = label, textAlign = TextAlign.Center) },
        colors = colors
    )
}
