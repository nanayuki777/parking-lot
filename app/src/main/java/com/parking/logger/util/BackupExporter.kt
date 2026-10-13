package com.parking.logger.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.parking.logger.data.model.Boundary
import com.parking.logger.data.model.ParkingType
import com.parking.logger.data.model.TestSession
import com.parking.logger.data.repository.SessionWithTester

/**
 * 导出备份工具：把测试数据序列化成 JSON 写入文件，可分享。
 */
object BackupExporter {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * 生成备份文件数据，返回一个可读的 JSON 字符串。
     */
    fun buildJson(sessions: List<SessionWithTester>): String {
        val data = sessions.map { sw ->
            mapOf(
                "sessionId" to sw.session.id,
                "tester" to sw.testerName,
                "location" to sw.session.location,
                "startTime" to sw.session.startTime,
                "records" to sw.records.map { r ->
                    mapOf(
                        "id" to r.id,
                        "timestamp" to r.timestamp,
                        "parkingType" to ParkingType.runCatching { valueOf(r.parkingType).label }
                            .getOrDefault(r.parkingType),
                        "condition" to r.condition,
                        "boundary" to Boundary.runCatching { valueOf(r.boundary).label }
                            .getOrDefault(r.boundary),
                        "totalCount" to r.totalCount,
                        "isIO" to r.isIO,
                        "ioIssues" to r.ioIssues,
                        "note" to r.note,
                        "photoPath" to r.photoPath,
                        "audioPath" to r.audioPath,
                        "pullOutType" to r.pullOutType,
                        "pullOutMoveCount" to r.pullOutMoveCount,
                        "pullOutResult" to r.pullOutResult
                    )
                }
            )
        }
        return gson.toJson(data)
    }

    /**
     * 把备份写入应用私有目录的 backup 文件夹，返回文件绝对路径。
     */
    fun saveBackup(context: Context, content: String, testers: Int, sessions: Int, records: Int): String {
        val dir = java.io.File(context.getExternalFilesDir(null), "ParkingLogger/backup")
        dir.mkdirs()
        val file = java.io.File(dir, "backup_${System.currentTimeMillis()}.json")
        file.writeText(
            "{\n" +
                "  \"exportedAt\": ${System.currentTimeMillis()},\n" +
                "  \"summary\": {\"testers\": $testers, \"sessions\": $sessions, \"records\": $records},\n" +
                "  \"data\": $content\n" +
                "}"
        )
        return file.absolutePath
    }
}
