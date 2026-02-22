package com.autoassistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.autoassistant.data.prefs.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationInterceptorService : NotificationListenerService() {
    @Inject lateinit var settingsRepo: SettingsRepository
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "NotificationInterceptor"
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName
        val notification = sbn.notification
        
        Log.d(TAG, "Notification from: $packageName")
        
        scope.launch {
            try {
                val ignoreGroups = settingsRepo.ignoreGroups.first()
                
                if (ignoreGroups && sbn.isGroup) {
                    Log.d(TAG, "Skipping group notification")
                    return@launch
                }
                
                if (packageName == "com.whatsapp" || packageName == "org.telegram.messenger") {
                    val extras = notification.extras
                    val title = extras.getString("android.title") ?: ""
                    val text = extras.getString("android.text") ?: ""
                    
                    Log.d(TAG, "Title: $title, Text: $text")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification: ${e.message}")
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }
}
