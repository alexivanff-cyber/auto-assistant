package com.autoassistant.domain.statemachine
import com.autoassistant.data.voice.STTManager
import com.autoassistant.data.voice.TTSManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceStateMachine @Inject constructor(private val sttManager: STTManager, private val ttsManager: TTSManager) {
    sealed class State {
        object Idle : State()
        object Listening : State()
        object Processing : State()
        object Speaking : State()
        data class WaitingForCommand(val message: String) : State()
        data class RecordingResponse(val originalMessage: String) : State()
    }
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state
    private val messageQueue = ArrayDeque<String>()
    fun onNotificationReceived(app: String, title: String, text: String) {
        if (app !in listOf("com.whatsapp", "org.telegram.messenger")) return
        messageQueue.addLast("$title: $text")
        processQueue()
    }
    private fun processQueue() {
        if (_state.value !is State.Idle) return
        val msg = messageQueue.removeFirstOrNull() ?: return
        _state.value = State.WaitingForCommand(msg)
        ttsManager.speak("Новое сообщение. $msg. Скажите 'ответь' для записи ответа.")
    }
    fun onVoiceCommand(command: String) {
        val lower = command.lowercase()
        when {
            lower.contains("ответь") || lower.contains("запиши") -> {
                _state.value = State.RecordingResponse(messageQueue.firstOrNull() ?: "")
                ttsManager.speak("Говорите. Скажите 'отправить' когда закончите.")
                sttManager.startListening { onVoiceCommand(it) }
            }
            lower.contains("отправить") -> {
                ttsManager.speak("Сообщение отправлено.")
                _state.value = State.Idle
                messageQueue.clear()
            }
            lower.contains("отмена") || lower.contains("игнор") -> {
                ttsManager.speak("Отменено.")
                _state.value = State.Idle
                messageQueue.clear()
            }
        }
    }
    fun cancel() {
        messageQueue.clear()
        sttManager.stopListening()
        ttsManager.stop()
        _state.value = State.Idle
    }
}
