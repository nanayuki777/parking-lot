package com.parking.logger.util

import android.content.Context

/**
 * 距离语音识别器（对外门面）。
 *
 * 内部基于 Vosk 离线识别，不再依赖系统的 SpeechRecognizer（其依赖厂商云端，
 * 在无网或云端被拒时不可用）。对外回调签名保持不变，调用方可无缝切换。
 *
 * 调用 [start] 时若 Vosk 模型尚未加载，会自动异步加载，加载完成后立即开始连续监听。
 */
class VoiceDistanceListener(
    private val context: Context
) {

    private val recognizer = VoskDistanceRecognizer(context)

    var isStarted: Boolean = false
        private set

    /** 回调：targetIndex 命中的字段(0~5)或 null；value 数字或 null；skip 是否跳过；rawText 识别原文 */
    var onSpoken: ((targetIndex: Int?, value: Double?, skip: Boolean, rawText: String) -> Unit)? = null
    var onStatusChanged: ((isListening: Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    /** 实时转写（识别过程中不断更新，测试用） */
    var onPartialText: ((String) -> Unit)? = null
    /** 收音音量（0~1 相对强度），用于画波形 */
    var onRmsLevel: ((Float) -> Unit)? = null
    /** 模型加载状态变化（供界面提示：加载中 / 就绪 / 失败） */
    var onModelStateChanged: (() -> Unit)? = null

    /** Vosk 模型是否已加载就绪 */
    val isModelReady: Boolean get() = recognizer.isModelReady
    val isLoading: Boolean get() = recognizer.isLoading
    val loadError: String? get() = recognizer.loadError

    /** 判断是否可用：Vosk 随包自带，理论可用；实际看模型加载是否成功 */
    val isAvailable: Boolean get() = true

    /** 进入界面即预加载模型（不开始录音，只让模型尽快就绪） */
    fun preload() {
        recognizer.init()
    }

    init {
        recognizer.onStatusChanged = { listening ->
            isStarted = listening
            onStatusChanged?.invoke(listening)
        }
        recognizer.onError = { msg ->
            onError?.invoke(msg)
        }
        recognizer.onPartialText = { text ->
            onPartialText?.invoke(text)
        }
        recognizer.onRmsLevel = { level ->
            onRmsLevel?.invoke(level)
        }
        recognizer.onModelStateChanged = {
            onModelStateChanged?.invoke()
        }
        recognizer.onRecognized = { text, parsed ->
            onSpoken?.invoke(parsed.targetIndex, parsed.value, parsed.skip, text)
        }
    }

    fun start() {
        if (isStarted) return
        recognizer.start()
    }

    fun stop() {
        recognizer.stop()
        isStarted = false
    }

    fun release() {
        recognizer.release()
        isStarted = false
    }
}
