package app.aaps.ui.compose.redesign

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
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

/**
 * The redesign overview: the home page and the graphs page, one shown at a time, changed by buttons only (no swipe).
 * No page indicator; each page has a name for the screen reader.
 *
 * The graphs keep every sideways drag (it scrolls the history).
 *
 * @param homePage the content of the home page; it scrolls by itself
 * @param graphsPage the content of the graphs page; gets the edit mode, scrolls by itself
 * @param canAddGraph false greys the (+) of the edit pill (the stock maximum of graphs is reached)
 * @param onAddGraph the (+) of the edit pill: one more graph at the bottom, in Small
 */
@Composable
fun RedesignOverviewContent(
    state: RedesignOverviewState,
    homePage: @Composable () -> Unit,
    graphsPage: @Composable (editMode: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    canAddGraph: Boolean = true,
    onAddGraph: () -> Unit = {}
) {
    // Leaving the graphs page ends the edit mode.
    LaunchedEffect(state.page) {
        if (state.page == HOME_PAGE) state.editMode = false
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
    // A plain switch of content, no pager: the page changes in the same frame as the state, so there is never a
    // moment with the wrong page (for example under the Menu). Only the buttons change the page (the Graphs item of
    // the bottom bar, Back). Both pages stay composed, so the graphs keep their state and do not load again at each
    // change of page.
    // Step 11: the page that appears fades in. The other one is hidden at once, so there is never a moment with
    // both pages, and the switch itself still happens in one frame.
    val homeAlpha by animateFloatAsState(if (state.page == HOME_PAGE) 1f else 0f, tween(REDESIGN_PAGE_FADE_MS), label = "homePage")
    val graphsAlpha by animateFloatAsState(if (state.page == GRAPHS_PAGE) 1f else 0f, tween(REDESIGN_PAGE_FADE_MS), label = "graphsPage")
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hiddenWhen(state.page != HOME_PAGE).graphicsLayer { alpha = homeAlpha }.semantics { paneTitle = homeName }) { homePage() }
        Box(Modifier.fillMaxSize().hiddenWhen(state.page != GRAPHS_PAGE).graphicsLayer { alpha = graphsAlpha }.semantics { paneTitle = graphsName }) {
            graphsPage(state.editMode)
            // Step 9 (SPEC 4.3): the edit pill floats, centred over the title row (64 dp; the pill is 52 dp).
            if (state.editMode) RedesignEditPill(
                canAdd = canAddGraph,
                onCancel = { state.cancelEdit() },
                onAdd = onAddGraph,
                onDone = { state.editMode = false },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
            )
        }
    }
}

/**
 * Keeps the content composed but not placed: nothing is drawn, nothing is touched and the screen reader skips it.
 * It is measured as usual, so the content keeps its size and its state.
 */
internal fun Modifier.hiddenWhen(hidden: Boolean): Modifier =
    if (!hidden) this
    else this
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { }
        }
        .clearAndSetSemantics { }

