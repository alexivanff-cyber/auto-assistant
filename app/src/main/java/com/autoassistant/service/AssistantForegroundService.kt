package com.autoassistant.service
import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.autoassistant.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AssistantForegroundService : Service() {
    companion object {
        private const val CHANNEL_ID = "AssistantChannel"
        private const val NOTIF_ID = 1001
    }
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("АлексО")
            .setContentText("Ассистент активен")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Голосовой ассистент", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Уведомления ассистента"
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
    override fun onDestroy() { super.onDestroy(); stopForeground(STOP_FOREGROUND_REMOVE) }
}
