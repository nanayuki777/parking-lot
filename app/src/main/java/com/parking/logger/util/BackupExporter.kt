package com.parking.logger.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.parking.logger.data.model.ParkingCondition
import com.parking.logger.data.model.ParkingType
import com.parking.logger.data.repository.SessionWithTester
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupExporter {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun exportZip(context: Context, sessions: List<SessionWithTester>): File? {
        val exportDir = File(context.cacheDir, "export")
        exportDir.mkdirs()
        val photosDir = File(exportDir, "photos").also { it.mkdirs() }
        val audioDir = File(exportDir, "audio").also { it.mkdirs() }

        val photoMap = mutableMapOf<String, String>() // oldPath -> relative
        val audioMap = mutableMapOf<String, String>()

        // Collect all unique photo/audio files
        for (sw in sessions) {
            for (r in sw.records) {
                if (r.photoPath.isNotBlank()) {
                    val src = File(r.photoPath)
                    if (src.exists() && !photoMap.containsKey(src.absolutePath)) {
                        val name = "photo_${r.id}_${src.name}"
                        val dst = File(photosDir, name)
                        try { src.inputStream().use { inp -> dst.outputStream().use { out -> inp.copyTo(out) } } } catch (_: Exception) {}
                        photoMap[src.absolutePath] = "photos/$name"
                    }
                }
                if (r.audioPath.isNotBlank()) {
                    val src = File(r.audioPath)
                    if (src.exists() && !audioMap.containsKey(src.absolutePath)) {
                        val name = "audio_${r.id}_${src.name}"
                        val dst = File(audioDir, name)
                        try { src.inputStream().use { inp -> dst.outputStream().use { out -> inp.copyTo(out) } } } catch (_: Exception) {}
                        audioMap[src.absolutePath] = "audio/$name"
                    }
                }
            }
        }

        // Build JSON with relative paths
        val triggerList = sessions.flatMap { sw ->
            sw.records.map { r ->
                mapOf(
                    "location" to sw.session.location,
                    "datetime" to formatTimestamp(r.timestamp),
                    "note" to (if (r.isIO) "Good" else r.ioIssues),
                    "issue" to (if (r.issue.isNotEmpty()) r.issue
                        else runCatching { ParkingType.valueOf(r.parkingType).issueLabel }.getOrDefault(r.parkingType)),
                    "type_of_maneuver" to (if (r.maneuver.isNotEmpty()) r.maneuver
                        else if (r.parkingType == ParkingType.FISHBONE.name) "Park-out" else "Park-in"),
                    "parking_situation" to (when (r.condition) {
                        ParkingCondition.BETWEEN_TWO_OBJECT.name -> "Between two object"
                        ParkingCondition.FREE_ONE_SIDE.name -> "Free one side"
                        ParkingCondition.FREE_BOTH_SIDE.name -> "Free both side"
                        else -> "Free both side"
                    }),
                    "image_link" to (photoMap[r.photoPath] ?: ""),
                    "audio_link" to (audioMap[r.audioPath] ?: ""),
                    "number_of_strokes" to r.totalCount,
                    "skew_angle" to r.skewAngle.toDouble(),
                    "distance" to mapOf(
                        "BL" to r.distanceBL.toDouble(),
                        "Back" to r.distanceBack.toDouble(),
                        "BR" to r.distanceBR.toDouble(),
                        "FR" to r.distanceFR.toDouble(),
                        "Front" to r.distanceFront.toDouble(),
                        "FL" to r.distanceFL.toDouble()
                    )
                )
            }
        }

        val jsonContent = gson.toJson(mapOf("trigger_list" to triggerList))
        File(exportDir, "data.json").writeText(jsonContent)

        // Create ZIP
        val zipFile = File(context.cacheDir, "export_parking_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            exportDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(exportDir).path
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        // Clean temp
        exportDir.deleteRecursively()
        return zipFile
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}