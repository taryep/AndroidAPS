package app.aaps.ui.compose.overview.statusLights

import androidx.compose.ui.graphics.vector.ImageVector
import app.aaps.core.ui.compose.StatusLevel

/**
 * Status item for sensor/insulin/cannula/battery display in Overview.
 *
 * Compact flags control what is shown in the collapsed (Overview-like) row.
 * Expanded flags control what is shown in the expanded (Actions-like) detail rows.
 */
data class StatusItem(
    val label: String,
    /** "3d4h", or "4h" while there is no full day yet. Always short, so every screen size reads the same. */
    val age: String,
    val ageStatus: StatusLevel = StatusLevel.UNSPECIFIED,
    val agePercent: Float = -1f, // 0-1 progress toward critical threshold
    val level: String? = null,
    val levelStatus: StatusLevel = StatusLevel.UNSPECIFIED,
    val levelPercent: Float = -1f, // -1 means no level, 0-1 for progress (inverted: 100% = empty/critical)
    val icon: ImageVector,
    val compactAge: Boolean = true,     // Show age in collapsed view
    val compactLevel: Boolean = true,   // Show level in collapsed view
    val expandedLevel: Boolean = true,  // Show level in expanded view
    /** The level as a number (reservoir units), for a layout that draws its own bar. Not used by the stock panel. */
    val levelValue: Double? = null,
    /** Longest age before the item is critical, short text ("3d", "8h"). For a layout that shows "age / maximum". */
    val ageMax: String? = null,
    /** The pump driver's largest reservoir reading [U]. A display limit, not always the real size. */
    val reservoirReadingLimit: Double? = null
)
