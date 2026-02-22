package com.autoassistant.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.autoassistant.data.prefs.SettingsRepository
import com.autoassistant.data.voice.STTManager
import com.autoassistant.data.voice.TTSManager
import com.autoassistant.domain.statemachine.VoiceStateMachine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var sttManager: STTManager
    @Inject lateinit var ttsManager: TTSManager
    @Inject lateinit var voiceMachine: VoiceStateMachine
    
    private fun isNotificationAccessEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabled?.contains(packageName) == true
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager.initialize { }
        setContent {
            MaterialTheme {
                var hasNotificationAccess by remember { mutableStateOf(isNotificationAccessEnabled()) }
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasNotificationAccess = isNotificationAccessEnabled()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(Unit) {
                    if (!hasNotificationAccess) {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        }
                    }
                }

                AssistantScreen(
                    settingsRepo, 
                    voiceMachine, 
                    sttManager, 
                    hasNotificationAccess,
                    {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        }
                    }
                )
            }
        }
    }
    
    override fun onDestroy() { 
        super.onDestroy()
        sttManager.destroy()
        ttsManager.shutdown() 
    }
}

@Composable
fun AssistantScreen(
    settingsRepo: SettingsRepository, 
    voiceMachine: VoiceStateMachine, 
    sttManager: STTManager,
    hasNotificationAccess: Boolean,
    onGrantNotification: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var assistantEnabled by remember { mutableStateOf(false) }
    var btOnly by remember { mutableStateOf(false) }
    var confirmSend by remember { mutableStateOf(true) }
    var ignoreGroups by remember { mutableStateOf(true) }
    var showInstructions by remember { mutableStateOf(false) }
    val state by voiceMachine.state.collectAsState()
    
    LaunchedEffect(Unit) {
        settingsRepo.assistantEnabled.collect { assistantEnabled = it }
        settingsRepo.btOnly.collect { btOnly = it }
        settingsRepo.confirmSend.collect { confirmSend = it }
        settingsRepo.ignoreGroups.collect { ignoreGroups = it }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🎙️ АлексО",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    2.dp,
                    if (hasNotificationAccess) Color(0xFF00C853) else Color(0xFFFF3D00),
                    RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (hasNotificationAccess) 
                    Color(0xFF1B5E20) 
                else 
                    Color(0xFFBF360C)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasNotificationAccess) "✅ Уведомления" else "⚠️ Нет доступа",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (hasNotificationAccess) "РАЗРЕШЕНО" else "ЗАПРЕЩЕНО",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.End
                    )
                }
                Text(
                    text = if (hasNotificationAccess) 
                        "Приложение может читать уведомления" 
                    else 
                        "Нажмите кнопку ниже для настройки",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        if (!hasNotificationAccess) {
            Button(
                onClick = { onGrantNotification() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { 
                Text("🔔 ВКЛЮЧИТЬ ДОСТУП К УВЕДОМЛЕНИЯМ") 
            }
            
            val context = LocalContext.current
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE65100)
                )
            ) {
                Text("🔓 СНЯТЬ ОГРАНИЧЕНИЕ (АНДРОИД 13+)")
            }

            OutlinedButton(
                onClick = { showInstructions = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📋 ИНСТРУКЦИЯ (ЕСЛИ ДОСТУП ОГРАНИЧЕН)")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B5E20)
                )
            ) {
                Text(
                    text = "✅ Доступ к уведомлениям включён!\nТеперь включите ассистента ниже.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        if (showInstructions) {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = { showInstructions = false },
                title = { Text("� Снятие ограничения", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Это функция Android 13+ для защиты от сторонних приложений. Если после нажатия 'ВКЛЮЧИТЬ ДОСТУП' ползунок серый:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        Text("1. Нажмите оранжевую кнопку «СНЯТЬ ОГРАНИЧЕНИЕ» (откроются настройки приложения АлексО).", modifier = Modifier.padding(vertical = 4.dp))
                        Text("2. Нажмите на три точки в правом верхнем углу (или прокрутите в самый низ).", modifier = Modifier.padding(vertical = 4.dp))
                        Text("3. Выберите «Разрешить скрытые настройки» или «Управление уведомлениями».", modifier = Modifier.padding(vertical = 4.dp))
                        Text("4. Подтвердите отпечатком пальца или паролем (по запросу).", modifier = Modifier.padding(vertical = 4.dp))
                        Text("5. Теперь снова нажмите «ВКЛЮЧИТЬ ДОСТУП» — ползунок загорится!", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                confirmButton = {
                    Button(onClick = { 
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                        showInstructions = false 
                    }) { Text("ОТКРЫТЬ НАСТРОЙКИ") }
                },
                dismissButton = {
                    TextButton(onClick = { showInstructions = false }) { Text("ПОЗЖЕ") }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("⚙️ НАСТРОЙКИ", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 12.dp))
                
                SwitchWithLabel("Ассистент", "Включить голосового ассистента", assistantEnabled) { 
                    assistantEnabled = it
                    scope.launch { settingsRepo.setAssistantEnabled(it) } 
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SwitchWithLabel("Только Bluetooth", "Работать только с гарнитурой", btOnly) { 
                    btOnly = it
                    scope.launch { settingsRepo.setBtOnly(it) } 
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SwitchWithLabel("Подтверждение отправки", "Требовать команду 'отправить'", confirmSend) { 
                    confirmSend = it
                    scope.launch { settingsRepo.setConfirmSend(it) } 
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SwitchWithLabel("Игнорировать группы", "Не читать групповые чаты", ignoreGroups) { 
                    ignoreGroups = it
                    scope.launch { settingsRepo.setIgnoreGroups(it) } 
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (state) {
                    is VoiceStateMachine.State.Listening -> Color(0xFF1565C0)
                    is VoiceStateMachine.State.Speaking -> Color(0xFF6A1B9A)
                    is VoiceStateMachine.State.RecordingResponse -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = when (state) {
                        is VoiceStateMachine.State.Idle -> "✅ Ожидание"
                        is VoiceStateMachine.State.Listening -> "👂 Слушаю…"
                        is VoiceStateMachine.State.Processing -> "⚙️ Обрабатываю…"
                        is VoiceStateMachine.State.Speaking -> "🗣️ Говорю…"
                        is VoiceStateMachine.State.WaitingForCommand -> "💬 Сообщение: ${(state as VoiceStateMachine.State.WaitingForCommand).message}"
                        is VoiceStateMachine.State.RecordingResponse -> "🎙️ Запись ответа…"
                        else -> "✅ Ожидание"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state is VoiceStateMachine.State.Idle) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        Color.White
                )
                Text(
                    text = "Команды: «ответь» • «отправить» • «отмена»",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state is VoiceStateMachine.State.Idle) 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    else 
                        Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SwitchWithLabel(label: String, hint: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
