package com.parking.logger.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.lang.ref.WeakReference

class AudioRecorder(context: Context) {
    private val appContext: WeakReference<Context> = WeakReference(context.applicationContext)
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    fun getAudioRoot(context: Context): File {
        val root = File(context.getExternalFilesDir(null), "ParkingLogger/audio")
        root.mkdirs()
        return root
    }

    fun start(): File {
        val ctx = appContext.get() ?: throw IllegalStateException("Context lost")
        outputFile = File(getAudioRoot(ctx), "audio_${System.currentTimeMillis()}.m4a")
        outputFile?.parentFile?.mkdirs()

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(ctx)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(outputFile?.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                outputFile?.delete()
                outputFile = null
                recorder = null
                throw e
            }
        }
        return outputFile!!
    }

    fun stop(): String? {
        val path = outputFile?.absolutePath
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // ignore stop errors: file may be empty
        }
        recorder?.release()
        recorder = null
        val file = outputFile
        outputFile = null
        if (file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            return null
        }
        return path
    }

    fun release() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // ignore
        }
        recorder?.release()
        recorder = null
    }
}
