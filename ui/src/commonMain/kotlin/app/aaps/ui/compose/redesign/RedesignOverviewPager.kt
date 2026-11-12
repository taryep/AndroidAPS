package app.aaps.ui.compose.redesign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.redesign.RedesignOverviewState.Companion.GRAPHS_PAGE
import app.aaps.ui.compose.redesign.RedesignOverviewState.Companion.HOME_PAGE
import app.aaps.ui.compose.redesign.RedesignOverviewState.Companion.PAGE_COUNT

/**
 * The redesign overview: two pages side by side, home and graphs, changed by buttons only (no swipe).
 * No page indicator; each page has a name for the screen reader.
 *
 * The graphs keep every sideways drag (it scrolls the history); the pager never takes one.
 *
 * @param homePage the content of the home page; it scrolls by itself
 * @param graphsPage the content of the graphs page; gets the edit mode, scrolls by itself
 */
@Composable
fun RedesignOverviewPager(
    state: RedesignOverviewState,
    homePage: @Composable () -> Unit,
    graphsPage: @Composable (editMode: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Not rememberPagerState: that one is saved state and would bring the graphs page back after
    // Android stopped the process. The page lives in `state`, which a new start resets.
    val pagerState = remember { PagerState(currentPage = state.page) { PAGE_COUNT } }

    // A page asked by code (tap on the glucose value, Menu > Edit screen, Back).
    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page) pagerState.animateScrollToPage(state.page)
    }
    // A page reached by a swipe. Leaving the graphs page ends the edit mode.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            state.page = page
            if (page == HOME_PAGE) state.editMode = false
        }
    }
    // The edit mode never stays on while the app is in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.editMode = false }

    // Back and the edge swipe on the graphs page: leave the edit mode, then go home.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.page == GRAPHS_PAGE && !state.menuOpen,
        onBackCompleted = { state.back() }
    )

    val homeName = stringResource(UiStrings.redesign_page_home)
    val graphsName = stringResource(UiStrings.redesign_page_graphs)
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        // Only the buttons change the page (the Graphs item of the bottom bar, a tap on the glucose
        // circle or the home graph, Menu > Edit mode, Back). No swipe, so the graphs keep every drag.
        userScrollEnabled = false,
        // Both pages stay composed, so the graphs do not load again at each change of page.
        beyondViewportPageCount = 1
    ) { page ->
        when (page) {
            HOME_PAGE -> Box(Modifier.fillMaxSize().semantics { paneTitle = homeName }) { homePage() }
            else      -> Column(Modifier.fillMaxSize().semantics { paneTitle = graphsName }) {
                if (state.editMode) RedesignEditPill(onDone = { state.editMode = false })
                Box(Modifier.weight(1f)) { graphsPage(state.editMode) }
            }
        }
    }
}

/** Top of the graphs page in edit mode: "Edit mode" and a "Done" button. */
@Composable
private fun RedesignEditPill(onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(UiStrings.redesign_edit_mode), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
                Button(onClick = onDone, modifier = Modifier.height(32.dp), contentPadding = PILL_BUTTON_PADDING) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(UiStrings.redesign_edit_done))
                }
            }
        }
    }
}

private val PILL_BUTTON_PADDING = PaddingValues(horizontal = 12.dp)
