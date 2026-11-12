package app.aaps.ui.compose.redesign

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.aaps.core.ui.compose.PHONE_COLUMN_MIN_WIDTH_DP
import app.aaps.core.ui.compose.isLandscape

/**
 * True when the overview uses the stacked phone layout, the only layout the redesign changes.
 *
 * This is the same test `OverviewScreen` makes to choose between the stacked, split and tablet
 * layouts: two columns are used only in landscape and only when two phone columns fit. It is copied
 * here, not shared, so the upstream file does not need to change. If upstream changes its rule, a
 * rotated phone may show the stock chrome with the stacked layout, or the other way round; nothing
 * breaks, but keep the two in step.
 *
 * @param maxWidth the width of the window content, as `BoxWithConstraints` gives it
 */
@Composable
fun isRedesignPhoneLayout(maxWidth: Dp): Boolean =
    !(isLandscape() && maxWidth >= (2 * PHONE_COLUMN_MIN_WIDTH_DP).dp)
