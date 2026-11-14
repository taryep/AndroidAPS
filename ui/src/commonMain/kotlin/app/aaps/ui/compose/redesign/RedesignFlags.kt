package app.aaps.ui.compose.redesign

import androidx.compose.runtime.staticCompositionLocalOf
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.interfaces.Preferences

/**
 * True when the new interface is shown (see `docs/redesign/SPEC.md`). The setting [BooleanKey.GeneralRedesign]
 * (Settings › Appearance, and the Menu of the new interface) is the only switch.
 *
 * Every hook in an upstream file asks [LocalRedesignEnabled] (in Compose) or this function.
 */
fun redesignEnabled(preferences: Preferences): Boolean = preferences.get(BooleanKey.GeneralRedesign)

/**
 * The value of [redesignEnabled] for the screens. `ComposeMainActivity` provides it; the activity starts
 * again when the setting changes, so the value never changes while a screen is shown (hence static).
 *
 * Without a provider it is false: the iOS and desktop shells render the same screens but do not follow
 * the setting (no restart, no portrait lock), so there the classic interface stays.
 */
val LocalRedesignEnabled = staticCompositionLocalOf { false }
