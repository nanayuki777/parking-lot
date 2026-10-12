package com.parking.logger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.parking.logger.data.dao.ParkingRecordDao
import com.parking.logger.data.dao.TestSessionDao
import com.parking.logger.data.dao.TesterDao
import com.parking.logger.data.model.ParkingRecord
import com.parking.logger.data.model.TestSession
import com.parking.logger.data.model.Tester

@Database(
    entities = [Tester::class, TestSession::class, ParkingRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun testerDao(): TesterDao
    abstract fun testSessionDao(): TestSessionDao
    abstract fun parkingRecordDao(): ParkingRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parking_logger_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}