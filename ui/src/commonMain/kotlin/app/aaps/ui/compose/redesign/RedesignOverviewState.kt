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

    /** The page shown, [HOME_PAGE] or [GRAPHS_PAGE]. Set it to change the page; the content follows. */
    var page by mutableIntStateOf(HOME_PAGE)

    /** True while the graphs page shows the buttons to add, change and remove graphs. */
    var editMode by mutableStateOf(false)

    /**
     * True while the Menu is the chosen item of the bottom bar. [page] stays the screen below it, so Back from the Menu
     * returns to the previous screen, and the edit mode and the graphs are kept.
     */
    var menuOpen by mutableStateOf(false)

    /**
     * Range of the home graph. Kept over a rotation and while other screens are open, not over a new
     * start of the app (SPEC 3.6: saved state, no preference).
     */
    var homeGraphRange by mutableStateOf(HomeGraphRange.DEFAULT)

    /** Home item of the bar, or a re-tap on Graphs or Menu: the home page, no Menu, no edit mode. */
    fun showHome() {
        menuOpen = false
        editMode = false
        page = HOME_PAGE
    }

    /** Graphs item: show the graphs page; a re-tap while it is shown goes home. */
    fun toggleGraphs() {
        if (page == GRAPHS_PAGE && !menuOpen) showHome() else {
            menuOpen = false
            page = GRAPHS_PAGE
        }
    }

    /** Menu item: open the Menu; a re-tap while it is open goes home. */
    fun toggleMenu() {
        if (menuOpen) showHome() else menuOpen = true
    }

    /** Menu > "Edit mode":the graphs page, in edit mode. */
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
    }
}
