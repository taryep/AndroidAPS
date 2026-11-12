package app.aaps.ui.compose.redesign

import android.app.Activity
import android.content.pm.ActivityInfo
import kotlin.math.min

/**
 * Portrait only on a phone (SPEC 0.4): the whole activity, so every screen in it (overview,
 * settings, ...) stays upright. Called from `ComposeMainActivity.onCreate`, before any content, so
 * the app does not first draw in landscape and then turn.
 *
 * - Flag off: nothing, the app is stock.
 * - Tablet: not locked; its landscape layouts are stock and stay. "Tablet" is the device, not the
 *   window: the largest window the screen can give (`maximumWindowMetrics`), so a phone in split
 *   screen stays a phone and a tablet in split screen stays a tablet. On a foldable it follows the
 *   screen in use; Android starts the activity again when that changes, so this runs again.
 * - In split screen Android does not apply the lock; the graphs page fill height follows the window.
 *
 * A pump wizard (`KeepScreenOnEffect`) and `LockPortraitOrientation` set their own lock and put this
 * one back when they end.
 */
object RedesignOrientation {

    fun lock(activity: Activity) {
        if (!RedesignFlags.ENABLED) return
        if (isPhone(activity)) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        // A foldable opened to its tablet screen: take back the lock set while it was folded.
        else if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun isPhone(activity: Activity): Boolean {
        val bounds = activity.windowManager.maximumWindowMetrics.bounds
        val smallestSideDp = min(bounds.width(), bounds.height()) / activity.resources.displayMetrics.density
        return smallestSideDp < TABLET_SMALLEST_WIDTH_DP
    }

    /** Android's own line between phones and tablets (the `sw600dp` resource qualifier). */
    private const val TABLET_SMALLEST_WIDTH_DP = 600
}
