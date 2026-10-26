package com.parking.logger.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File

class AudioPlayer(context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var playbackFile: File? = null
    val mainHandler = Handler(Looper.getMainLooper())

    var isPlaying: Boolean = false
        private set
    var onProgress: ((Float) -> Unit)? = null
    var onCompletion: (() -> Unit)? = null

    fun play(filePath: String) {
        stop()
        val file = File(filePath)
        if (!file.exists()) {
            onCompletion?.invoke()
            return
        }
        playbackFile = file
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener {
                start()
                isPlaying = true
                trackProgress(this)
            }
            setOnCompletionListener {
                isPlaying = false
                mainHandler.post { onCompletion?.invoke() }
            }
            prepareAsync()
        }
    }

    private fun trackProgress(mp: MediaPlayer) {
        Thread {
            while (mp.isPlaying) {
                val progress = mp.currentPosition.toFloat() / mp.duration.toFloat()
                mainHandler.post { onProgress?.invoke(progress) }
                try { Thread.sleep(200) } catch (_: InterruptedException) { break }
            }
        }.apply { isDaemon = true; start() }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }

    fun release() {
        stop()
    }
}