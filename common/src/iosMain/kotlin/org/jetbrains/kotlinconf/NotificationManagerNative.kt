package org.jetbrains.kotlinconf

import io.ktor.util.date.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import org.jetbrains.kotlinconf.storage.*
import platform.Foundation.*
import platform.UserNotifications.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*

actual class NotificationManager actual constructor(context: ApplicationContext) {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    actual suspend fun isEnabled(): Boolean = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            continuation.resume(settings?.alertSetting == UNNotificationSettingEnabled)
        }
    }

    actual suspend fun requestPermission(): Boolean = suspendCancellableCoroutine {
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert) { allowed, error ->
            if (error != null) {
                it.resumeWithException(error.asException())
            } else {
                it.resume(allowed)
            }
        }
    }

    actual suspend fun schedule(
        sessionData: SessionData
    ): String? {
        val title = ""
        val text = ""
        val date = sessionData.startsAt

        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(text)
        }

        val id = NSUUID().UUIDString
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            date.toNSDateComponents(), false
        )

        val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger).freeze()
        val withCompletionHandler: (NSError?) -> Unit = { error: NSError? -> }.freeze()

        center.addNotificationRequest(request, withCompletionHandler)
        return id
    }

    actual fun cancel(id: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
    }
}

internal fun GMTDate.toNSDateComponents(): NSDateComponents = NSDateComponents().apply {
    setYear(this@toNSDateComponents.year.convert())
    setMonth(this@toNSDateComponents.month.ordinal.convert())
    setDay(this@toNSDateComponents.dayOfMonth.convert())
    setHour(this@toNSDateComponents.hours.convert())
    setMinute(this@toNSDateComponents.minutes.convert())
    setSecond(this@toNSDateComponents.seconds.convert())
}

internal class NativeException(val error: NSError) : Exception()

internal fun NSError.asException(): NativeException = NativeException(this)
