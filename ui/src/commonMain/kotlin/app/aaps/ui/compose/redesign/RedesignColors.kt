package app.aaps.ui.compose.redesign

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Surface colours of the redesign screens (SPEC section 7). They are kept here and not in the
 * global theme, so the stock screens do not change.
 *
 * Only the colours used so far are here. The glucose palette and the tile tints come in later phases.
 */
@Immutable
data class RedesignColors(
    val pageBackground: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val chevron: Color,
    val shortcutIcon: Color,
    val danger: Color
)

val DarkRedesignColors = RedesignColors(
    pageBackground = Color(0xFF0E0F13),
    surface = Color(0xFF171920),
    border = Color(0xFF252932),
    textPrimary = Color(0xFFECEEF2),
    textSecondary = Color(0xFF9AA0AC),
    chevron = Color(0xFF6C7280),
    shortcutIcon = Color(0xFF8CCBF0),
    danger = Color(0xFFFF8A6B)
)

val LightRedesignColors = RedesignColors(
    pageBackground = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFE1E4E9),
    textPrimary = Color(0xFF14161B),
    textSecondary = Color(0xFF5B616E),
    chevron = Color(0xFF6B717D),
    shortcutIcon = Color(0xFF0A6388),
    danger = Color(0xFFB3261E)
)

/**
 * The redesign colours for the current theme. AAPS has its own dark mode setting, so the system
 * setting cannot be used; the theme's surface colour tells which scheme `AapsTheme` chose.
 */
@Composable
fun redesignColors(): RedesignColors =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) DarkRedesignColors else LightRedesignColors
