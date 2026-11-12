package app.aaps.ui.compose.redesign

import androidx.compose.runtime.staticCompositionLocalOf
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.interfaces.Preferences

/**
 * The switches of the phone overview redesign (see `docs/redesign/SPEC.md`).
 *
 * - [ENABLED] is the build switch. With it set to false the app must behave exactly like stock AAPS,
 *   whatever the setting says, so the fork can be turned off without removing code.
 * - The setting [BooleanKey.GeneralRedesign] (Settings › Appearance, and the Menu of the new interface)
 *   lets the user go back to the classic interface without a new build.
 *
 * Every hook in an upstream file asks [LocalRedesignEnabled] (in Compose) or [redesignEnabled].
 */
object RedesignFlags {

    const val ENABLED = true
}

/** True when the new interface is shown: the build switch is on and the user did not turn it off. */
fun redesignEnabled(preferences: Preferences): Boolean = RedesignFlags.ENABLED && preferences.get(BooleanKey.GeneralRedesign)

/**
 * The value of [redesignEnabled] for the screens. `ComposeMainActivity` provides it; the activity starts
 * again when the setting changes, so the value never changes while a screen is shown (hence static).
 * Without a provider (tests, previews) it is the build switch.
 */
val LocalRedesignEnabled = staticCompositionLocalOf { RedesignFlags.ENABLED }
