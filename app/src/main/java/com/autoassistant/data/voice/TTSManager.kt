package com.autoassistant.data.voice
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSManager @Inject constructor(@ApplicationContext private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking
    private var onInitComplete: ((Boolean) -> Unit)? = null
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
                override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
                override fun onError(utteranceId: String?) { _isSpeaking.value = false }
            })
            onInitComplete?.invoke(true)
        } else { onInitComplete?.invoke(false) }
    }
    fun initialize(onComplete: (Boolean) -> Unit) { onInitComplete = onComplete; tts = TextToSpeech(context, this) }
    fun speak(text: String, language: String? = null) {
        if (tts == null) return
        language?.let { lang -> tts?.setLanguage(Locale.forLanguageTag(lang)) }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "msg")
    }
    fun stop() { tts?.stop(); _isSpeaking.value = false }
    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null }
}
