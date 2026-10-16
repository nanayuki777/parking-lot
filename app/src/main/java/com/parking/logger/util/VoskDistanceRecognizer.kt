package com.parking.logger.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * 基于 Vosk 的离线距离语音识别器。
 *
 * Vosk 把中文模型打包进 App，识别完全在本地完成，不依赖设备厂商的云端服务，
 * 现场无网也能用，不受小米等「云端建连失败」问题影响。
 *
 * 模型放置（二选一，支持从 assets 拷贝到内部存储）：
 *   1) 未压缩目录：`app/src/main/assets/vosk-model-small-cn-0.22/<模型内容>`
 *   2) 压缩包：`app/src/main/assets/vosk-model-small-cn-0.22.zip`
 * 首次启动会拷贝解压到 App 内部目录，之后直接加载。
 *
 * 音频：16kHz / 16bit / 单声道 PCM，连续监听。每次静音后视为一句话结束，
 * 解析出数字并回调，随后 reset 监听下一条。
 */
class VoskDistanceRecognizer(private val context: Context) {

    private val MODEL_NAME = "vosk-model-small-cn-0.22"
    private val SAMPLE_RATE = 16000

    private val mainHandler = Handler(Looper.getMainLooper())
    private val started = AtomicBoolean(false)
    private val disposed = AtomicBoolean(false)

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null

    @Volatile
    var isModelReady: Boolean = false
        private set

    @Volatile
    var isLoading: Boolean = false
        private set

    @Volatile
    var loadError: String? = null
        private set

    // ---- 对外回调 ----
    var onStatusChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onPartialText: ((String) -> Unit)? = null
    var onRmsLevel: ((Float) -> Unit)? = null
    var onRecognized: ((String, VoiceDistanceParser.ParseResult) -> Unit)? = null
    /** 模型加载状态变化（成功/失败），界面刷新用 */
    var onModelStateChanged: (() -> Unit)? = null

    /** 加载并准备模型（异步，成功后回调 onStatusChanged(true)） */
    fun init() {
        if (isModelReady || isLoading) return
        isLoading = true
        loadError = null
        Thread({ loadModelBlocking() }, "VoskModelInit").start()
    }

    private fun loadModelBlocking() {
        try {
            val modelDir = File(context.filesDir, MODEL_NAME)
            if (!File(modelDir, "AmExport.ini").exists() && !hasModelFiles(modelDir)) {
                // 从 assets 拷贝/解压到内部目录
                copyAssetsModel(modelDir)
            }
            val m = Model(modelDir.absolutePath)
            model = m
            isModelReady = true
            isLoading = false
            mainHandler.post {
                onModelStateChanged?.invoke()
                onStatusChanged?.invoke(true)
                // 用户已在等待：模型就绪后立即开始监听
                if (pendingStart) {
                    pendingStart = false
                    beginRecording()
                }
            }
        } catch (e: Exception) {
            isLoading = false
            pendingStart = false
            val msg = "Vosk 模型加载失败: ${e.message}"
            loadError = msg
            mainHandler.post {
                onModelStateChanged?.invoke()
                onError?.invoke(msg)
            }
        }
    }

    private fun hasModelFiles(dir: File): Boolean =
        (dir.listFiles()?.size ?: 0) > 0

    /** 支持 assets 下的目录或 .zip，拷到内部目录 */
    private fun copyAssetsModel(targetDir: File) {
        val am = context.assets
        // assets 根目录下找到模型顶级条目：目录 <NAME> 或 文件 <NAME>.zip
        val topLevel = am.list("")?.toList() ?: emptyList()

        val dirName = topLevel.firstOrNull { it == MODEL_NAME }
        val zipName = topLevel.firstOrNull { it == "$MODEL_NAME.zip" }

        when {
            dirName != null -> copyAssetDir(am, MODEL_NAME, targetDir)
            zipName != null -> {
                am.open(zipName).use { input -> extractZip(input, targetDir) }
            }
            else -> throw IllegalStateException(
                "assets 中找不到中文模型。请放置：\n" +
                    "  app/src/main/assets/$MODEL_NAME/ 目录 或 $MODEL_NAME.zip"
            )
        }
    }

    private fun copyAssetDir(am: android.content.res.AssetManager, assetPath: String, outDir: File) {
        outDir.mkdirs()
        val children = am.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            // 叶子文件
            am.open(assetPath).use { input ->
                val outFile = File(outDir, assetPath.substringAfterLast('/'))
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { out ->
                    input.copyTo(out)
                }
            }
        } else {
            for (child in children) {
                val childAsset = "$assetPath/$child"
                val childOut = File(outDir, child)
                if (am.list(childAsset).isNullOrEmpty()) {
                    am.open(childAsset).use { input ->
                        childOut.parentFile?.mkdirs()
                        FileOutputStream(childOut).use { out -> input.copyTo(out) }
                    }
                } else {
                    copyAssetDir(am, childAsset, childOut)
                }
            }
        }
    }

    private fun extractZip(input: java.io.InputStream, targetDir: File) {
        targetDir.parentFile?.mkdirs()
        val zipFile = File(targetDir.parentFile, "$MODEL_NAME.zip")
        FileOutputStream(zipFile).use { out -> input.copyTo(out) }
        val zipInputStream = java.util.zip.ZipInputStream(zipFile.inputStream())
        zipInputStream.use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        zipFile.delete()
    }

    /** 是否已在等待模型加载完成后自动开始监听 */
    private var pendingStart = false

    fun start() {
        if (started.get()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onError?.invoke("缺少录音权限")
            return
        }
        if (!isModelReady) {
            // 模型未就绪：先触发加载，加载完成后自动开始
            pendingStart = true
            init()
            return
        }
        pendingStart = false
        beginRecording()
    }

    private fun beginRecording() {
        if (started.get()) return
        started.set(true)
        onStatusChanged?.invoke(true)
        recordThread = Thread({ recordLoop() }, "VoskRecord").apply { start() }
    }

    fun stop() {
        started.set(false)
        recordThread = null
        runCatching { audioRecord?.stop() }
        audioRecord?.release()
        audioRecord = null
        recognizer?.close()
        recognizer = null
        onStatusChanged?.invoke(false)
    }

    fun release() {
        disposed.set(true)
        stop()
        model?.close()
        model = null
        isModelReady = false
    }

    private fun recordLoop() {
        if (disposed.get()) return
        val recorder = try {
            createAudioRecord()
        } catch (e: Exception) {
            mainHandler.post { onError?.invoke("创建录音器失败: ${e.message}") }
            started.set(false)
            return
        }
        audioRecord = recorder
        try {
            recorder.startRecording()
        } catch (e: Exception) {
            mainHandler.post { onError?.invoke("开始录音失败: ${e.message}") }
            started.set(false)
            return
        }

        recognizer = Recognizer(model, 16000f)

        val bufSize = 4000
        val shortBuf = ShortArray(bufSize)
        while (started.get() && !disposed.get()) {
            if (Thread.interrupted()) break
            val read = recorder.read(shortBuf, 0, shortBuf.size)
            if (read > 0) {
                val level = computeRms(shortBuf, read)
                mainHandler.post { onRmsLevel?.invoke(level) }

                val bytes = shortsToBytes(shortBuf, read)
                feed(bytes)
            }
        }

        runCatching { recorder.stop() }
        recorder.release()
        if (audioRecord === recorder) audioRecord = null
        if (!disposed.get()) {
            mainHandler.post { onStatusChanged?.invoke(false) }
        }
    }

    private fun createAudioRecord(): AudioRecord {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        return AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            (minBuf * 4).coerceAtLeast(16000)
        )
    }

    private fun feed(bytes: ByteArray) {
        val r = recognizer ?: return
        val accepted = r.acceptWaveForm(bytes, bytes.size)

        val partialText = parseField(r.getPartialResult(), "partial")
        if (!partialText.isNullOrEmpty()) {
            mainHandler.post { onPartialText?.invoke(partialText) }
        }
        if (accepted) {
            val text = parseField(r.getResult(), "text")
            if (!text.isNullOrEmpty()) {
                val parsed = VoiceDistanceParser.parse(text)
                mainHandler.post { onRecognized?.invoke(text, parsed) }
            }
            r.reset()
        }
    }

    private fun parseField(json: String?, field: String): String? {
        if (json == null) return null
        val key = "\"$field\":\""
        val start = json.indexOf(key)
        if (start < 0) return null
        val vStart = start + key.length
        val end = json.indexOf('"', vStart)
        if (end < 0) return null
        return json.substring(vStart, end)
    }

    private fun computeRms(shortBuf: ShortArray, length: Int): Float {
        if (length <= 0) return 0f
        var sum = 0.0
        for (i in 0 until length) {
            val v = shortBuf[i].toInt()
            sum += v.toDouble() * v
        }
        val rms = sqrt(sum / length)
        return (rms / 32767.0).toFloat().coerceIn(0f, 1f)
    }

    private fun shortsToBytes(shortBuf: ShortArray, length: Int): ByteArray {
        val bytes = ByteArray(length * 2)
        for (i in 0 until length) {
            val v = shortBuf[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}
