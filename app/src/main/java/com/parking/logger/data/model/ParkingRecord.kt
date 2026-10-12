package com.parking.logger.data.model

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
    val parkingType: String,
    val condition: String,
    val boundary: String,
    val forwardCount: Int = 0,
    val backwardCount: Int = 0,
    val count3: Int = 0,
    val count4: Int = 0,
    val count5: Int = 0,
    val count6: Int = 0,
    val isIO: Boolean = true,
    val ioIssues: String = "",
    val note: String = "",
    val photoPath: String = "",
    val audioPath: String = "",
    val pullOutType: String = "",
    val pullOutMoveCount: Int = 0,
    val pullOutResult: String = ""
)