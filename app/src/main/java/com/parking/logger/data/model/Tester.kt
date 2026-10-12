package com.parking.logger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "testers")
data class Tester(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
