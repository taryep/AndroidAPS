package app.aaps.ui.compose.redesign

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * State of the redesign overview: which page is shown and whether the graphs can be edited.
 *
 * A view model, not saved state: it is kept over a rotation, but a new start of the app (also after
 * Android stopped the process) begins on the home page, without edit mode.
 */
class RedesignOverviewState : ViewModel() {

    /** The page shown, [HOME_PAGE] or [GRAPHS_PAGE]. Set it to change the page; the pager follows. */
    var page by mutableIntStateOf(HOME_PAGE)

    /** True while the graphs page shows the buttons to add, change and remove graphs. */
    var editMode by mutableStateOf(false)

    /** True while the Menu screen is open over the overview. The Menu handles Back then. */
    var menuOpen by mutableStateOf(false)

    /** Menu > "Edit screen": the graphs page, in edit mode. */
    fun editScreen() {
        editMode = true
        page = GRAPHS_PAGE
    }

    /** Back on the graphs page: first leave the edit mode, then go back to the home page. */
    fun back() {
        if (editMode) editMode = false else page = HOME_PAGE
    }

    companion object {

        const val HOME_PAGE = 0
        const val GRAPHS_PAGE = 1
        const val PAGE_COUNT = 2
    }
}
