package org.jetbrains.kotlinconf

import android.app.*
import android.app.NotificationManager
import android.content.*
import android.os.*
import androidx.core.app.*
import io.ktor.util.date.*
import org.jetbrains.kotlinconf.storage.*

private val CHANNEL_ID = "KOTLIN_CONF_CHANNEL_ID"

actual class NotificationManager actual constructor(
    private val context: ApplicationContext
) {
    actual suspend fun requestPermission(): Boolean {
        createNotificationChannel(context.activity)
        return true
    }

    actual suspend fun schedule(
        sessionData: SessionData
    ): String? {
        val date = sessionData.startsAt - 15 * 60 * 1000
        val id = sessionData.id
        if (date > GMTDate()) return null

        val delay = date.timestamp - GMTDate().timestamp
        scheduleNotification(delay, sessionData.title, "Starts in 15 minutes", id.toInt())
        return id
    }

    actual fun cancel(id: String) {
        val appContext = context.activity
        val notificationIntent = Intent(appContext, Publisher::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext, id.toInt(), notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT
        )
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    actual suspend fun isEnabled(): Boolean {
        return true
    }

    private fun scheduleNotification(
        delay: Long,
        title: String,
        text: String,
        notificationId: Int
    ) {
        val appContext = context.activity.applicationContext
        val showTime = System.currentTimeMillis() + delay

        val intent: Intent = appContext.packageManager
            .getLaunchIntentForPackage("com.jetbrains.kotlinconf")!!

        val activity = PendingIntent.getActivity(
            appContext, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setAutoCancel(true)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(activity)
            .setWhen(showTime)
            .setSmallIcon(context.notificationIcon)
            .build()

        val notificationIntent = Intent(appContext, Publisher::class.java).apply {
            putExtra(Publisher.NOTIFICATION_ID, notificationId)
            putExtra(Publisher.NOTIFICATION, notification)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext, notificationId, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT
        )

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, showTime, pendingIntent)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "KotlinConf"
            val descriptionText = "KotlinConf notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }
}

class Publisher : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = intent.getParcelableExtra<Notification>(NOTIFICATION)
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        var NOTIFICATION_ID = "notification_id"
        var NOTIFICATION = "notification"
    }
}

