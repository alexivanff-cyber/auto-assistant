package com.autoassistant.ui
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    private val notificationAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager.initialize { }
        setContent {
            MaterialTheme {
                AssistantScreen(settingsRepo, voiceMachine, sttManager, {
                    notificationAccessLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                })
            }
        }
    }
    override fun onDestroy() { super.onDestroy(); sttManager.destroy(); ttsManager.shutdown() }
}

@Composable
fun AssistantScreen(settingsRepo: SettingsRepository, voiceMachine: VoiceStateMachine, sttManager: STTManager, onGrantNotification: () -> Unit) {
    val scope = rememberCoroutineScope()
    var assistantEnabled by remember { mutableStateOf(false) }
    var btOnly by remember { mutableStateOf(false) }
    var confirmSend by remember { mutableStateOf(true) }
    var ignoreGroups by remember { mutableStateOf(true) }
    val state by voiceMachine.state.collectAsState()
    LaunchedEffect(Unit) {
        settingsRepo.assistantEnabled.collect { assistantEnabled = it }
        settingsRepo.btOnly.collect { btOnly = it }
        settingsRepo.confirmSend.collect { confirmSend = it }
        settingsRepo.ignoreGroups.collect { ignoreGroups = it }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "🎙️ Ассистент слушает WhatsApp и Telegram.\nСкажите «ответь», чтобы начать запись.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = onGrantNotification) { Text("🔔 ДОСТУП К УВЕДОМЛЕНИЯМ") }
        Text(text = "Нажмите для доступа к уведомлениям — это позволит ассистенту читать сообщения.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        SwitchWithLabel("Ассистент", "Включить/выключить голосового ассистента", assistantEnabled) { assistantEnabled = it; scope.launch { settingsRepo.setAssistantEnabled(it) } }
        SwitchWithLabel("Только Bluetooth", "Работать только при подключённой гарнитуре", btOnly) { btOnly = it; scope.launch { settingsRepo.setBtOnly(it) } }
        SwitchWithLabel("Подтверждение отправки", "Требовать команду 'отправить' перед отправкой", confirmSend) { confirmSend = it; scope.launch { settingsRepo.setConfirmSend(it) } }
        SwitchWithLabel("Игнорировать группы", "Не обрабатывать сообщения из групповых чатов", ignoreGroups) { ignoreGroups = it; scope.launch { settingsRepo.setIgnoreGroups(it) } }
        Spacer(modifier = Modifier.weight(1f))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = when (state) { is VoiceStateMachine.State.Idle -> "✅ Ожидание"; is VoiceStateMachine.State.Listening -> "👂 Слушаю…"; is VoiceStateMachine.State.Processing -> "⚙️ Обрабатываю…"; is VoiceStateMachine.State.Speaking -> "🗣️ Говорю…"; is VoiceStateMachine.State.WaitingForCommand -> "💬 Сообщение: ${(state as VoiceStateMachine.State.WaitingForCommand).message}"; is VoiceStateMachine.State.RecordingResponse -> "🎙️ Запись ответа…"; else -> "✅ Ожидание" }, style = MaterialTheme.typography.titleMedium)
                Text(text = "Команды: «ответь» • «отправить» • «отмена» • «запиши»", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun SwitchWithLabel(label: String, hint: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { Text(text = label, style = MaterialTheme.typography.bodyLarge); Text(text = hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
