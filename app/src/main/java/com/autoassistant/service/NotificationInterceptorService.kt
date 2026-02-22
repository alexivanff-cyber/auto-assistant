package com.autoassistant.service
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.autoassistant.domain.statemachine.VoiceStateMachine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationInterceptorService : NotificationListenerService() {
    @Inject lateinit var voiceMachine: VoiceStateMachine
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in listOf("com.whatsapp", "org.telegram.messenger")) return
        val extras = sbn.notification.extras
        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
        voiceMachine.onNotificationReceived(pkg, title, text)
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
