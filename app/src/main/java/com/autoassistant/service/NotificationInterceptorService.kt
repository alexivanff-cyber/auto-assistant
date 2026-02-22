package com.autoassistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.autoassistant.data.prefs.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        
        // Проверяем настройки
        scope.launch {
            val ignoreGroups = settingsRepo.ignoreGroups.collectFirst()
            
            // Пропускаем групповые чаты если настроено
            if (ignoreGroups && sbn.isGroup) {
                Log.d(TAG, "Skipping group notification")
                return@launch
            }
            
            // Обрабатываем только WhatsApp и Telegram
            if (packageName == "com.whatsapp" || packageName == "org.telegram.messenger") {
                val extras = notification.extras
                val title = extras.getString("android.title") ?: ""
                val text = extras.getString("android.text") ?: ""
                
                Log.d(TAG, "Title: $title, Text: $text")
                
                // Здесь будет логика озвучивания через TTSManager
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }
}