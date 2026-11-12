package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings

/**
 * The "Graphs" item of the bottom bar (SPEC 4b), just before the Menu item. Kept here so
 * `MainNavigationBar` only gains one call.
 *
 * A tap shows the graphs page; while it is shown the item is selected and the same tap goes back to
 * the home page. A screen reader is told what the tap will do then, as for the Menu item.
 */
@Composable
fun RowScope.RedesignGraphsNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    colors: NavigationBarItemColors
) {
    val label = stringResource(UiStrings.redesign_graphs)
    val backLabel = stringResource(UiStrings.redesign_close_graphs)
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = if (selected) Modifier.semantics { onClick(label = backLabel) { onClick(); true } } else Modifier,
        icon = { Icon(imageVector = Icons.AutoMirrored.Filled.ShowChart, contentDescription = label) },
        label = { Text(text = label, textAlign = TextAlign.Center) },
        colors = colors
    )
}
