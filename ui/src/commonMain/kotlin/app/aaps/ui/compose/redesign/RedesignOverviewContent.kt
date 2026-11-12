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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 */
@Composable
fun RedesignOverviewContent(
    state: RedesignOverviewState,
    homePage: @Composable () -> Unit,
    graphsPage: @Composable (editMode: Boolean) -> Unit,
    modifier: Modifier = Modifier
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
    // the bottom bar, a tap on the glucose circle or the home graph, Back). Both pages stay composed, so the graphs
    // keep their state and do not load again at each change of page.
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hiddenWhen(state.page != HOME_PAGE).semantics { paneTitle = homeName }) { homePage() }
        Column(Modifier.fillMaxSize().hiddenWhen(state.page != GRAPHS_PAGE).semantics { paneTitle = graphsName }) {
            if (state.editMode) RedesignEditPill(onDone = { state.editMode = false })
            Box(Modifier.weight(1f)) { graphsPage(state.editMode) }
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
                Button(
                    onClick = onDone,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PILL_BUTTON_PADDING,
                    colors = ButtonDefaults.buttonColors(containerColor = DONE_BUTTON_COLOR, contentColor = DONE_BUTTON_TEXT_COLOR)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(UiStrings.redesign_edit_done))
                }
            }
        }
    }
}

private val PILL_BUTTON_PADDING = PaddingValues(horizontal = 12.dp)

/** The "Done" button of the edit pill: sky blue with dark text (SPEC 7). */
private val DONE_BUTTON_COLOR = Color(0xFF56B4E9)
private val DONE_BUTTON_TEXT_COLOR = Color(0xFF06263A)
