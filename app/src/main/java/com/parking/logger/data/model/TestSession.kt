package com.parking.logger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_sessions")
data class TestSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testerId: Long,
    val location: String,
    val startTime: Long = System.currentTimeMillis(),
    val note: String = ""
)
