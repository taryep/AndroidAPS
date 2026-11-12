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
 * The glucose palette itself is the stock one (`GeneralColors`, phase 0).
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
    val danger: Color,
    /** Tile tints of the running modes (SPEC 3.2). */
    val closedLoop: RedesignTint,
    val openLoop: RedesignTint,
    val lgs: RedesignTint,
    val suspended: RedesignTint,
    val disconnected: RedesignTint,
    val disabled: RedesignTint,
    val superBolus: RedesignTint,
    /** Temporary profile switch (yellow) and temporary target (orange). */
    val profileTemp: RedesignTint,
    val targetTemp: RedesignTint,
    val insulin: RedesignTint,
    val carbsActive: RedesignTint,
    val carbsRequired: RedesignTint,
    val tempBasal: RedesignTint,
    /** Home graph card (SPEC 3.6) and its parts. */
    val graphCard: Color,
    val graphCardBorder: Color,
    /** The faint grey axis labels. */
    val graphLabel: Color,
    val selectedRange: Color,
    /** The round edit button of the graphs page (SPEC 4.1): background and border. Its icon is [textSecondary], grey on purpose. */
    val editButton: Color,
    val editButtonBorder: Color,
    /** Text of the 250 / 170 / 70 / 56 labels: darker than the lines on light, so 11 sp stays readable. */
    val thresholdText: RedesignThresholdText
) {

    /** A tile with nothing special to show. */
    val neutral: RedesignTint get() = RedesignTint(background = surface, border = border, accent = textPrimary)
}

/** Text colours of the very high, high, low and very low line labels of the home graph. */
@Immutable
data class RedesignThresholdText(val veryHigh: Color, val high: Color, val low: Color, val veryLow: Color)

/** Background, border and accent (icon, label) of a tinted tile or slot. */
@Immutable
data class RedesignTint(val background: Color, val border: Color, val accent: Color)

/** The insulin tile is tinted from this much insulin on board, in units. Below it, and when negative, it stays neutral. */
const val REDESIGN_INSULIN_TINT_THRESHOLD_U = 1.0

val DarkRedesignColors = RedesignColors(
    pageBackground = Color(0xFF0E0F13),
    surface = Color(0xFF171920),
    border = Color(0xFF252932),
    textPrimary = Color(0xFFECEEF2),
    textSecondary = Color(0xFF9AA0AC),
    chevron = Color(0xFF6C7280),
    shortcutIcon = Color(0xFF8CCBF0),
    danger = Color(0xFFFF8A6B),
    closedLoop = RedesignTint(Color(0xFF132219), Color(0xFF1F3A28), Color(0xFF6FD98B)),
    openLoop = RedesignTint(Color(0xFF14203A), Color(0xFF26406B), Color(0xFF8CB8F2)),
    lgs = RedesignTint(Color(0xFF1F1A33), Color(0xFF3A2F60), Color(0xFFB9A6F2)),
    suspended = RedesignTint(Color(0xFF2A2512), Color(0xFF524716), Color(0xFFF0D86A)),
    disconnected = RedesignTint(Color(0xFF1E2026), Color(0xFF3A3E48), Color(0xFFB4BAC5)),
    disabled = RedesignTint(Color(0xFF2C1616), Color(0xFF5A2626), Color(0xFFFF8A80)),
    superBolus = RedesignTint(Color(0xFF2A1E10), Color(0xFF553A16), Color(0xFFF7A541)),
    profileTemp = RedesignTint(Color(0xFF2A2512), Color(0xFF524716), Color(0xFFF0D86A)),
    targetTemp = RedesignTint(Color(0xFF2A1E10), Color(0xFF553A16), Color(0xFFF7A541)),
    insulin = RedesignTint(Color(0xFF0F2530), Color(0xFF1C4352), Color(0xFF7FD6E6)),
    carbsActive = RedesignTint(Color(0xFF251B10), Color(0xFF4A3518), Color(0xFFF7B267)),
    carbsRequired = RedesignTint(Color(0xFF33200E), Color(0xFF8A5A1E), Color(0xFFF7B267)),
    tempBasal = RedesignTint(Color(0xFF10292C), Color(0xFF1C4247), Color(0xFF8FD3DA)),
    graphCard = Color(0xFF14161B),
    graphCardBorder = Color(0xFF22252D),
    graphLabel = Color(0xFF858B98),
    selectedRange = Color(0xFF262A33),
    editButton = Color(0xFF1E2128),
    editButtonBorder = Color(0xFF3A4660),
    // The dark glucose colours, except 56: its line colour is too dark for text on the dark card.
    thresholdText = RedesignThresholdText(Color(0xFFE69F00), Color(0xFFF0E442), Color(0xFFFF6A3D), Color(0xFFE2602C))
)

// Light theme: the same hues as the dark tints, a very light background, a mid border and a dark accent.
val LightRedesignColors = RedesignColors(
    pageBackground = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFE1E4E9),
    textPrimary = Color(0xFF14161B),
    textSecondary = Color(0xFF5B616E),
    chevron = Color(0xFF6B717D),
    shortcutIcon = Color(0xFF0A6388),
    danger = Color(0xFFB3261E),
    closedLoop = RedesignTint(Color(0xFFE9F6EC), Color(0xFFBFE3CA), Color(0xFF1D7238)),
    openLoop = RedesignTint(Color(0xFFEAF1FC), Color(0xFFBCD3F2), Color(0xFF1F4F99)),
    lgs = RedesignTint(Color(0xFFF0ECFB), Color(0xFFD3C8F2), Color(0xFF5B3FB0)),
    suspended = RedesignTint(Color(0xFFFBF6DD), Color(0xFFE8D98A), Color(0xFF6E5A00)),
    disconnected = RedesignTint(Color(0xFFEEF0F3), Color(0xFFCDD1D8), Color(0xFF4A505C)),
    disabled = RedesignTint(Color(0xFFFBEAEA), Color(0xFFEDBDBD), Color(0xFFA12A2A)),
    superBolus = RedesignTint(Color(0xFFFDF0E0), Color(0xFFEFC58C), Color(0xFF8A4A00)),
    profileTemp = RedesignTint(Color(0xFFFBF6DD), Color(0xFFE8D98A), Color(0xFF6E5A00)),
    targetTemp = RedesignTint(Color(0xFFFDF0E0), Color(0xFFEFC58C), Color(0xFF8A4A00)),
    insulin = RedesignTint(Color(0xFFE6F1F8), Color(0xFFB8D7EA), Color(0xFF0A6388)),
    carbsActive = RedesignTint(Color(0xFFFDF4EA), Color(0xFFF0D3AE), Color(0xFF9A5300)),
    carbsRequired = RedesignTint(Color(0xFFFDEFD8), Color(0xFFE8B160), Color(0xFF8A4A00)),
    tempBasal = RedesignTint(Color(0xFFE3F4F4), Color(0xFFADDCDD), Color(0xFF0B6A70)),
    graphCard = Color(0xFFFFFFFF),
    graphCardBorder = Color(0xFFE1E4E9),
    graphLabel = Color(0xFF6B717D),
    selectedRange = Color(0xFFEEF0F3),
    editButton = Color(0xFFEEF0F4),
    editButtonBorder = Color(0xFFC9CED8),
    thresholdText = RedesignThresholdText(Color(0xFF8A6508), Color(0xFF9A6A00), Color(0xFFB34C00), Color(0xFF8B2500))
)

/**
 * The redesign colours for the current theme. AAPS has its own dark mode setting, so the system
 * setting cannot be used; the theme's surface colour tells which scheme `AapsTheme` chose.
 */
@Composable
fun redesignColors(): RedesignColors =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) DarkRedesignColors else LightRedesignColors
