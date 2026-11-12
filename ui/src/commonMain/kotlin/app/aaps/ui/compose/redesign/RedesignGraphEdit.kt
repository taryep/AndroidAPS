package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aaps.core.interfaces.overview.graph.GraphConfig
import app.aaps.core.interfaces.overview.graph.SecondaryGraph
import app.aaps.core.interfaces.overview.graph.SeriesType
import app.aaps.core.keys.interfaces.TextRef
import app.aaps.core.ui.CoreUiStrings
import app.aaps.core.ui.compose.stringResource
import app.aaps.ui.UiStrings
import app.aaps.ui.compose.overview.graphs.CONFIGURABLE_SERIES

/**
 * The changes of the edit mode of the graphs page (step 9, SPEC 4.3), on the stock graph settings. They are
 * plain list changes of [GraphConfig]: nothing new is saved, and the stock screens read the same settings.
 * BG and IOB / basal are fixed (first and second); the other graphs are [GraphConfig.secondaryGraphs].
 */
object RedesignGraphEdit {

    /** The size of a graph: BG, IOB / basal, or the other graph at [index]. */
    fun size(config: GraphConfig, kind: GraphBlock.Kind, index: Int): GraphSize = GraphSize.fromSavedHeight(
        when (kind) {
            GraphBlock.Kind.BG    -> config.bgHeight
            GraphBlock.Kind.IOB   -> config.iobHeight
            GraphBlock.Kind.OTHER -> config.secondaryGraphs.getOrNull(index)?.height ?: GraphSize.SMALL.code
        }
    )

    /** One size bigger ([bigger]) or smaller. Grand and Small are the ends: then nothing changes. */
    fun resize(config: GraphConfig, kind: GraphBlock.Kind, index: Int, bigger: Boolean): GraphConfig {
        val sizes = GraphSize.entries
        val now = sizes.indexOf(size(config, kind, index))
        val next = sizes.getOrNull(if (bigger) now + 1 else now - 1) ?: return config
        return when (kind) {
            GraphBlock.Kind.BG    -> config.copy(bgHeight = next.code)
            GraphBlock.Kind.IOB   -> config.copy(iobHeight = next.code)
            GraphBlock.Kind.OTHER -> config.copy(
                secondaryGraphs = config.secondaryGraphs.mapIndexed { i, graph -> if (i == index) graph.copy(height = next.code) else graph }
            )
        }
    }

    /** The other graph at [index] one place up ([up]) or down. It never goes above IOB / basal. */
    fun move(config: GraphConfig, index: Int, up: Boolean): GraphConfig {
        val target = if (up) index - 1 else index + 1
        val graphs = config.secondaryGraphs
        if (index !in graphs.indices || target !in graphs.indices) return config
        return config.copy(secondaryGraphs = graphs.toMutableList().also { it[index] = graphs[target]; it[target] = graphs[index] })
    }

    /** Without the other graph at [index]. */
    fun remove(config: GraphConfig, index: Int): GraphConfig =
        if (index !in config.secondaryGraphs.indices) config
        else config.copy(secondaryGraphs = config.secondaryGraphs.filterIndexed { i, _ -> i != index })

    /** True while one more graph can be added (the stock maximum). */
    fun canAdd(config: GraphConfig): Boolean = config.secondaryGraphs.size < GraphConfig.MAX_SECONDARY_GRAPHS

    /**
     * One more graph at the bottom, in Small (SPEC 4.3). It shows the first series that no other graph shows
     * (its pencil changes it), so a new graph is never empty: the stock code removes a graph with no series.
     */
    fun add(config: GraphConfig, available: List<SeriesType> = CONFIGURABLE_SERIES): GraphConfig {
        if (!canAdd(config) || available.isEmpty()) return config
        val used = config.secondaryGraphs.flatMap { it.series }.toSet()
        val series = available.firstOrNull { it !in used } ?: available.first()
        return config.copy(secondaryGraphs = config.secondaryGraphs + SecondaryGraph(listOf(series), GraphSize.SMALL.code))
    }
}

/** The name of a size, as the size control shows it. */
fun GraphSize.label(): TextRef = when (this) {
    GraphSize.SMALL  -> UiStrings.redesign_size_small
    GraphSize.MEDIUM -> UiStrings.redesign_size_medium
    GraphSize.GRAND  -> UiStrings.redesign_size_grand
}

/**
 * Drawn over a graph in edit mode (SPEC 4.3): the graph is dimmed and takes no touch (the page still scrolls,
 * the touches are not consumed), then its controls: the size `− Petit +` and the pencil (the stock menu of the
 * graph) on the right; for the graphs that can move, `↑ ↓` and delete at the bottom left.
 *
 * @param count how many other graphs there are, so `↓` is greyed on the last one
 */
@Composable
fun BoxScope.RedesignGraphEditOverlay(block: GraphBlock, config: GraphConfig, count: Int, onChange: (GraphConfig) -> Unit) {
    val colors = redesignColors()
    // The graph at 40 %: a veil of the page colour. A pointer input that does not consume: the graph under it
    // gets nothing (only the top one of overlapping siblings gets a touch), the page above still scrolls.
    Box(
        Modifier
            .matchParentSize()
            .background(colors.pageBackground.copy(alpha = VEIL_ALPHA))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
    )
    val size = RedesignGraphEdit.size(config, block.kind, block.index)
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.align(Alignment.TopEnd).padding(top = CONTROLS_TOP, end = 4.dp)
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = EDIT_SURFACE, border = BorderStroke(1.dp, EDIT_BORDER)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EditIconButton(Icons.Filled.Remove, stringResource(UiStrings.redesign_size_smaller), enabled = size != GraphSize.SMALL, framed = false) {
                    onChange(RedesignGraphEdit.resize(config, block.kind, block.index, bigger = false))
                }
                Text(
                    text = stringResource(size.label()),
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.width(52.dp)
                )
                EditIconButton(Icons.Filled.Add, stringResource(UiStrings.redesign_size_bigger), enabled = size != GraphSize.GRAND, framed = false) {
                    onChange(RedesignGraphEdit.resize(config, block.kind, block.index, bigger = true))
                }
            }
        }
        EditIconButton(Icons.Filled.Edit, stringResource(UiStrings.redesign_graph_options), tint = DONE_COLOR, onClick = block.openMenu)
    }
    if (block.kind == GraphBlock.Kind.OTHER) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = EDIT_SURFACE,
            border = BorderStroke(1.dp, EDIT_BORDER),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ↑ is greyed on the first other graph: it cannot go above IOB / basal.
                EditIconButton(Icons.Filled.ArrowUpward, stringResource(UiStrings.redesign_graph_up), enabled = block.index > 0, framed = false) {
                    onChange(RedesignGraphEdit.move(config, block.index, up = true))
                }
                EditIconButton(Icons.Filled.ArrowDownward, stringResource(UiStrings.redesign_graph_down), enabled = block.index < count - 1, framed = false) {
                    onChange(RedesignGraphEdit.move(config, block.index, up = false))
                }
                // No confirmation: (✕) undoes it.
                EditIconButton(Icons.Filled.Delete, stringResource(UiStrings.redesign_graph_delete), tint = DELETE_COLOR, framed = false) {
                    onChange(RedesignGraphEdit.remove(config, block.index))
                }
            }
        }
    }
}

/**
 * The edit pill over the title row (SPEC 4.3): `(✕) Mode édition (+) (✓)`. Close on the left, confirm on the
 * right. No red: red is for delete.
 */
@Composable
fun RedesignEditPill(canAdd: Boolean, onCancel: () -> Unit, onAdd: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = EDIT_SURFACE,
        border = BorderStroke(1.dp, EDIT_BORDER),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp).size(width = PILL_WIDTH, height = 52.dp)
        ) {
            EditIconButton(Icons.Filled.Close, stringResource(CoreUiStrings.cancel), background = PILL_BUTTON, onClick = onCancel)
            Text(
                text = stringResource(UiStrings.redesign_edit_mode),
                fontSize = 14.sp,
                color = PILL_TEXT,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            EditIconButton(Icons.Filled.Add, stringResource(CoreUiStrings.graph_add), background = PILL_BUTTON, enabled = canAdd, onClick = onAdd)
            EditIconButton(Icons.Filled.Check, stringResource(UiStrings.redesign_edit_done), background = DONE_COLOR, tint = DONE_ICON, onClick = onDone)
        }
    }
}

/** A round 44 dp button (48 dp touch target) of the edit mode. */
@Composable
private fun EditIconButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    framed: Boolean = true,
    background: Color = EDIT_SURFACE,
    tint: Color = PILL_TEXT,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (framed) background else Color.Transparent,
            contentColor = tint,
            disabledContainerColor = if (framed) background else Color.Transparent,
            disabledContentColor = tint.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .size(48.dp)
            .padding(2.dp)
            .semantics { contentDescription = description }
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

// Colours of the edit mode, from SPEC 4.3. The edit pill and the controls are dark in both themes, as in the mockup.
private val EDIT_SURFACE = Color(0xFF1E2128)
private val EDIT_BORDER = Color(0xFF3A4660)
private val PILL_BUTTON = Color(0xFF2A2E38)
private val PILL_TEXT = Color(0xFFC9CED8)
private val DONE_COLOR = Color(0xFF56B4E9)
private val DONE_ICON = Color(0xFF06121B)
private val DELETE_COLOR = Color(0xFFE0603A)

private val PILL_WIDTH = 300.dp

/** The controls start under the legend line of the graph. */
private val CONTROLS_TOP = 26.dp

/** The graphs at 40 %: a veil of the page colour at 60 %. */
private const val VEIL_ALPHA = 0.6f
