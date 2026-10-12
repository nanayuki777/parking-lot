package com.parking.logger

import android.app.Application
import com.parking.logger.data.database.AppDatabase
import com.parking.logger.data.repository.ParkingRepository

class ParkingApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        ParkingRepository(
            database.testerDao(),
            database.testSessionDao(),
            database.parkingRecordDao()
        )
    }
}