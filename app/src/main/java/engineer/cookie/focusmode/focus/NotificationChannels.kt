package engineer.cookie.focusmode.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import engineer.cookie.focusmode.R

object NotificationChannels {

    const val STATUS_CHANNEL_ID = "focus_mode_status"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            STATUS_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }
}
