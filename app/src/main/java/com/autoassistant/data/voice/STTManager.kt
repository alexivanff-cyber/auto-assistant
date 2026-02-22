package com.autoassistant.data.voice
import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class STTManager @Inject constructor(@ApplicationContext private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    private val supportedLanguages = mapOf(
        "ru" to Locale("ru", "RU"), "en" to Locale("en", "US"), "es" to Locale("es", "ES"),
        "fr" to Locale("fr", "FR"), "de" to Locale("de", "DE"), "it" to Locale("it", "IT"),
        "pt" to Locale("pt", "BR"), "zh" to Locale("zh", "CN"), "ja" to Locale("ja", "JP"),
        "ko" to Locale("ko", "KR"), "ar" to Locale("ar", "SA"), "hi" to Locale("hi", "IN")
    )
    private fun detectLanguage(): Locale {
        val deviceLang = Locale.getDefault().language
        return supportedLanguages[deviceLang] ?: supportedLanguages["ru"]!!
    }
    fun startListening(onResult: (String) -> Unit) {
        if (speechRecognizer == null) speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val locale = detectLanguage()
        val params = Bundle().apply {
            putString(SpeechRecognizer.EXTRA_LANGUAGE, locale.toLanguageTag())
            putBoolean(SpeechRecognizer.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { onResult(it) }
                _isListening.value = false
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { onResult(it) }
            }
            override fun onError(error: Int) { _isListening.value = false }
            override fun onBeginningOfSpeech() { _isListening.value = true }
            override fun onEndOfSpeech() { _isListening.value = false }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(params)
    }
    fun stopListening() { speechRecognizer?.stopListening(); _isListening.value = false }
    fun destroy() { speechRecognizer?.destroy(); speechRecognizer = null }
}
