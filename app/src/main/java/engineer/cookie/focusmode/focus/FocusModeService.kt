package engineer.cookie.focusmode.focus

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import engineer.cookie.focusmode.AppGraph
import engineer.cookie.focusmode.R
import engineer.cookie.focusmode.ui.MainActivity

/**
 * Foreground service that keeps Focus Mode represented by a silent, ongoing
 * "dummy" notification (the standard trick used by apps that must stay active
 * without Google services).
 */
class FocusModeService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                AppGraph.focusModeController.setEnabled(false)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                // Re-apply the policy defensively in case the system reset it.
                AppGraph.dndController.enable()
                return START_STICKY
            }
        }
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val stopIntent = Intent(this, FocusModeService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(this, REQUEST_STOP, stopIntent, flags)

        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java),
            flags,
        )

        return NotificationCompat.Builder(this, NotificationChannels.STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_action_disable), stopPending)
            .build()
    }

    companion object {
        const val ACTION_START = "engineer.cookie.focusmode.action.START"
        const val ACTION_STOP = "engineer.cookie.focusmode.action.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_STOP = 101
        private const val REQUEST_OPEN = 102
    }
}
