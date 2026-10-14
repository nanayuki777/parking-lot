package com.parking.logger.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.parking.logger.data.model.ParkingCondition
import com.parking.logger.data.model.ParkingType
import com.parking.logger.data.repository.SessionWithTester
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupExporter {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun buildJson(sessions: List<SessionWithTester>): String {
        val triggerList = sessions.flatMap { sw ->
            sw.records.map { r ->
                val typeOfManeuver = if (r.maneuver.isNotEmpty()) r.maneuver else
                    if (r.parkingType == ParkingType.FISHBONE.name) "Park-out" else "Park-in"

                val parkingSituation = runCatching {
                    ParkingCondition.valueOf(r.condition)
                }.getOrNull()?.let { condition ->
                    when (condition) {
                        ParkingCondition.ONE_SIDE_CAR, ParkingCondition.CAR_AND_PILLAR,
                        ParkingCondition.FRONT_CAR, ParkingCondition.REAR_CAR -> "Free one side"
                        ParkingCondition.BOTH_SIDES_CAR, ParkingCondition.BOTH_CAR -> "Between two object"
                        ParkingCondition.NO_CAR, ParkingCondition.NO_CAR_AROUND,
                        ParkingCondition.PILLAR_AND_LINE, ParkingCondition.FISHBONE -> "Free both side"
                        ParkingCondition.SIDE_PULL_OUT, ParkingCondition.STRAIGHT_PULL_OUT -> "Free both side"
                    }
                } ?: "Free both side"

                val noteValue = if (r.isIO) "Good" else r.ioIssues

                mapOf(
                    "location" to sw.session.location,
                    "datetime" to formatTimestamp(r.timestamp),
                    "note" to noteValue,
                    "issue" to (if (r.issue.isNotEmpty()) r.issue else
                        runCatching { ParkingType.valueOf(r.parkingType).issueLabel }
                            .getOrDefault(r.parkingType)),
                    "type_of_maneuver" to typeOfManeuver,
                    "parking_situation" to parkingSituation,
                    "image_link" to (r.photoPath.ifEmpty { "" }),
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
        return gson.toJson(mapOf("trigger_list" to triggerList))
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun saveBackup(context: Context, content: String, testers: Int, sessions: Int, records: Int): String {
        val dir = java.io.File(context.getExternalFilesDir(null), "ParkingLogger/backup")
        dir.mkdirs()
        val file = java.io.File(dir, "backup_${System.currentTimeMillis()}.json")
        file.writeText(content)
        return file.absolutePath
    }
}
