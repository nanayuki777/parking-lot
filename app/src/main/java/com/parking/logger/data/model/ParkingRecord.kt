package com.parking.logger.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parking_records",
    foreignKeys = [
        ForeignKey(
            entity = TestSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ParkingRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val issue: String,
    @ColumnInfo(defaultValue = "''") val maneuver: String = "",
    val parkingType: String,
    val condition: String,
    val boundary: String,
    val totalCount: Int = 0,
    val isIO: Boolean = true,
    val ioIssues: String = "",
    val note: String = "",
    @ColumnInfo(defaultValue = "0") val skewAngle: Float = 0f,
    @ColumnInfo(defaultValue = "0") val distanceBL: Float = 0f,
    @ColumnInfo(defaultValue = "0") val distanceBack: Float = 0f,
    @ColumnInfo(defaultValue = "0") val distanceBR: Float = 0f,
    @ColumnInfo(defaultValue = "0") val distanceFR: Float = 0f,
    @ColumnInfo(defaultValue = "0") val distanceFront: Float = 0f,
    @ColumnInfo(defaultValue = "0") val distanceFL: Float = 0f,
    val photoPath: String = "",
    val audioPath: String = "",
    val pullOutType: String = "",
    val pullOutMoveCount: Int = 0,
    val pullOutResult: String = ""
)