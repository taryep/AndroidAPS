package app.aaps.ui.compose.redesign

import app.aaps.core.interfaces.notifications.AapsNotification
import app.aaps.core.interfaces.notifications.NotificationId
import app.aaps.core.interfaces.notifications.NotificationLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Step 8: the badge colour by type and the preview of new notifications. */
class RedesignNotificationsTest {

    private fun notification(level: NotificationLevel, instance: Int = 1, date: Long = 1_000L) =
        AapsNotification(id = NotificationId.PROFILE_SET_OK, instanceKey = instance, text = "text", level = level, date = date)

    @Test
    fun eachStockLevelHasAType() {
        assertEquals(RedesignNotificationType.ALERT, NotificationLevel.URGENT.redesignType())
        assertEquals(RedesignNotificationType.WARNING, NotificationLevel.IMPORTANT.redesignType())
        assertEquals(RedesignNotificationType.WARNING, NotificationLevel.NORMAL.redesignType())
        assertEquals(RedesignNotificationType.INFO, NotificationLevel.LOW.redesignType())
        assertEquals(RedesignNotificationType.INFO, NotificationLevel.INFO.redesignType())
        assertEquals(RedesignNotificationType.INFO, NotificationLevel.ANNOUNCEMENT.redesignType())
    }

    @Test
    fun theBadgeTakesTheMostSeriousType() {
        assertNull(redesignMostSeriousType(emptyList()))
        assertEquals(RedesignNotificationType.INFO, redesignMostSeriousType(listOf(notification(NotificationLevel.INFO))))
        assertEquals(
            RedesignNotificationType.ALERT,
            redesignMostSeriousType(listOf(notification(NotificationLevel.INFO), notification(NotificationLevel.URGENT, 2), notification(NotificationLevel.NORMAL, 3)))
        )
    }

    @Test
    fun onlyNewNotificationsArePreviewedTheMostSeriousFirst() {
        val old = notification(NotificationLevel.INFO)
        val newInfo = notification(NotificationLevel.INFO, instance = 2)
        val newWarning = notification(NotificationLevel.IMPORTANT, instance = 3)
        val fresh = redesignNewNotifications(listOf(old, newInfo, newWarning), seenKeys = setOf(old.redesignKey()))
        assertEquals(listOf(newWarning, newInfo), fresh)
        // The same notification posted again later (new date) is new again.
        assertEquals(1, redesignNewNotifications(listOf(old.copy(date = 2_000L)), setOf(old.redesignKey())).size)
    }
}
