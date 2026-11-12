package app.aaps.ui.compose.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aaps.core.interfaces.notifications.AapsNotification
import app.aaps.core.interfaces.notifications.NotificationLevel
import kotlinx.coroutines.delay

/**
 * The three types of notification the home page shows (step 8, decided 12 Nov): info (blue), warning
 * (yellow) and alert (red). Ordered from the most serious.
 */
enum class RedesignNotificationType { ALERT, WARNING, INFO }

/**
 * The type of a stock level. URGENT is the alarm level (sound, full screen), so it is an alert;
 * IMPORTANT and NORMAL are warnings; LOW, INFO and ANNOUNCEMENT are information.
 */
fun NotificationLevel.redesignType(): RedesignNotificationType = when (this) {
    NotificationLevel.URGENT    -> RedesignNotificationType.ALERT
    NotificationLevel.IMPORTANT,
    NotificationLevel.NORMAL    -> RedesignNotificationType.WARNING

    NotificationLevel.LOW,
    NotificationLevel.INFO,
    NotificationLevel.ANNOUNCEMENT -> RedesignNotificationType.INFO
}

/** The type of the most serious notification in [notifications], or null when there is none. */
fun redesignMostSeriousType(notifications: List<AapsNotification>): RedesignNotificationType? =
    notifications.minByOrNull { it.level.priority }?.level?.redesignType()

/** The colours of a type: the tints of the running modes, so they work in both themes. */
fun RedesignColors.notificationTint(type: RedesignNotificationType): RedesignTint = when (type) {
    RedesignNotificationType.ALERT   -> disabled
    RedesignNotificationType.WARNING -> suspended
    RedesignNotificationType.INFO    -> openLoop
}

/**
 * The new notifications among [notifications], compared with the keys seen before ([seenKeys]). The most
 * serious comes first. A notification is the same one while its id, instance and date stay the same.
 */
fun redesignNewNotifications(notifications: List<AapsNotification>, seenKeys: Set<String>): List<AapsNotification> =
    notifications.filter { it.redesignKey() !in seenKeys }.sortedBy { it.level.priority }

/** What makes a notification the same one for the preview. */
fun AapsNotification.redesignKey(): String = "$id/$instanceKey/$date"

/**
 * A short preview of a notification that just arrived (step 8): its text in a card of its type's colour, at
 * the top of the home page, for [PREVIEW_MS]. A tap opens the notification sheet. The notifications there at
 * the start do not show (no burst when the app opens). The stock alarm of an URGENT notification (sound, full
 * screen) is not replaced: this is only a preview, so there is no second popup.
 */
@Composable
fun RedesignNotificationPreview(notifications: List<AapsNotification>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var seenKeys by remember { mutableStateOf<Set<String>?>(null) }
    var shown by remember { mutableStateOf<AapsNotification?>(null) }
    LaunchedEffect(notifications) {
        val keys = notifications.map { it.redesignKey() }.toSet()
        val before = seenKeys
        seenKeys = keys
        // A notification that was dismissed meanwhile is not previewed any more.
        if (shown?.redesignKey() !in keys) shown = null
        if (before != null) redesignNewNotifications(notifications, before).firstOrNull()?.let { shown = it }
    }
    LaunchedEffect(shown) {
        if (shown != null) {
            delay(PREVIEW_MS)
            shown = null
        }
    }
    val notification = shown ?: return
    val tint = redesignColors().notificationTint(notification.level.redesignType())
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = tint.background,
        border = BorderStroke(1.dp, tint.border),
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(Modifier.size(10.dp).background(tint.accent, CircleShape))
            Text(
                text = notification.text,
                color = redesignColors().textPrimary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** How long a preview stays. */
private const val PREVIEW_MS = 5_000L
