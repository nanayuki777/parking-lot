package com.parking.logger.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * 连续语音识别监听器：把每次识别的语音解析成距离数字。
 * 识别结果通过 [onSpoken] 回调抛出（带可选字段标签），
 * 由调用方（屏幕层）决定填入哪个字段。
 * 持续监听到调用方主动 [stop] 或设备酸度报错为止。
 */
class VoiceDistanceListener(
    private val context: Context
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    var isStarted: Boolean = false
        private set

    /** 回调：targetIndex 命中的字段(0~5)或 null；value 数字或 null；skip 是否跳过；rawText 识别原文 */
    var onSpoken: ((targetIndex: Int?, value: Double?, skip: Boolean, rawText: String) -> Unit)? = null
    var onStatusChanged: ((isListening: Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /** 判断设备是否支持语音识别 */
    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (isStarted) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(recognitionListener)
        isStarted = true
        startListening()
        onStatusChanged?.invoke(true)
    }

    fun stop() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        val wasStarted = isStarted
        isStarted = false
        isListening = false
        if (wasStarted) onStatusChanged?.invoke(false)
    }

    fun release() {
        stop()
    }

    private fun startListening() {
        if (!isStarted) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // 尽量连续：安静多久（毫秒）后结束一次识别
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
        }
        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            onError?.invoke("语音识别启动失败: ${e.message}")
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onError(error: Int) {
            when (error) {
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    if (isStarted) startListening()
                }
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    if (isStarted) startListening()
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    onError?.invoke("缺少录音权限，无法语音录入")
                    stop()
                }
                else -> {
                    if (isStarted) startListening()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?: return
            if (matches.isNotEmpty()) {
                val text = matches[0]
                val parsed = VoiceDistanceParser.parse(text)
                onSpoken?.invoke(parsed.targetIndex, parsed.value, parsed.skip, text)
            }
            // 继续监听（除非调用方已 stop）
            if (isStarted) startListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
