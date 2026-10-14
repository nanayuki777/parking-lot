package com.parking.logger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.parking.logger.data.dao.ParkingRecordDao
import com.parking.logger.data.dao.TestSessionDao
import com.parking.logger.data.dao.TesterDao
import com.parking.logger.data.model.ParkingRecord
import com.parking.logger.data.model.TestSession
import com.parking.logger.data.model.Tester

@Database(
    entities = [Tester::class, TestSession::class, ParkingRecord::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun testerDao(): TesterDao
    abstract fun testSessionDao(): TestSessionDao
    abstract fun parkingRecordDao(): ParkingRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val PARKING_RECORDS_SCHEMA = """
            CREATE TABLE parking_records_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                issue TEXT NOT NULL DEFAULT '',
                maneuver TEXT NOT NULL DEFAULT '',
                parkingType TEXT NOT NULL,
                condition TEXT NOT NULL,
                boundary TEXT NOT NULL,
                totalCount INTEGER NOT NULL,
                isIO INTEGER NOT NULL,
                ioIssues TEXT NOT NULL,
                note TEXT NOT NULL,
                skewAngle REAL NOT NULL DEFAULT 0,
                distanceBL REAL NOT NULL DEFAULT 0,
                distanceBack REAL NOT NULL DEFAULT 0,
                distanceBR REAL NOT NULL DEFAULT 0,
                distanceFR REAL NOT NULL DEFAULT 0,
                distanceFront REAL NOT NULL DEFAULT 0,
                distanceFL REAL NOT NULL DEFAULT 0,
                photoPath TEXT NOT NULL,
                audioPath TEXT NOT NULL,
                pullOutType TEXT NOT NULL,
                pullOutMoveCount INTEGER NOT NULL,
                pullOutResult TEXT NOT NULL
            )
        """

        // 2 -> v4：旧 schema 无 issue/maneuver/skewAngle/distance*，需补默认值迁移
        private val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(PARKING_RECORDS_SCHEMA)
                db.execSQL(
                    """
                    INSERT INTO parking_records_new (
                        id, sessionId, timestamp, parkingType, condition, boundary,
                        totalCount, isIO, ioIssues, note, photoPath, audioPath,
                        pullOutType, pullOutMoveCount, pullOutResult,
                        issue, maneuver, skewAngle,
                        distanceBL, distanceBack, distanceBR,
                        distanceFR, distanceFront, distanceFL
                    ) SELECT
                        id, sessionId, timestamp, parkingType, condition, boundary,
                        totalCount, isIO, ioIssues, note, photoPath, audioPath,
                        pullOutType, pullOutMoveCount, pullOutResult,
                        '', '', 0,
                        0, 0, 0,
                        0, 0, 0
                    FROM parking_records
                    """
                )
                db.execSQL("DROP TABLE parking_records")
                db.execSQL("ALTER TABLE parking_records_new RENAME TO parking_records")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_parking_records_sessionId ON parking_records(sessionId)")
            }
        }

        // 3 -> v4：schema 列已存在但缺 DEFAULT，重建以补默认值
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(PARKING_RECORDS_SCHEMA)
                db.execSQL(
                    """
                    INSERT INTO parking_records_new (
                        id, sessionId, timestamp, issue, maneuver, parkingType, condition,
                        boundary, totalCount, isIO, ioIssues, note,
                        skewAngle, distanceBL, distanceBack, distanceBR,
                        distanceFR, distanceFront, distanceFL,
                        photoPath, audioPath, pullOutType, pullOutMoveCount, pullOutResult
                    ) SELECT
                        id, sessionId, timestamp, issue, maneuver, parkingType, condition,
                        boundary, totalCount, isIO, ioIssues, note,
                        skewAngle, distanceBL, distanceBack, distanceBR,
                        distanceFR, distanceFront, distanceFL,
                        photoPath, audioPath, pullOutType, pullOutMoveCount, pullOutResult
                    FROM parking_records
                    """
                )
                db.execSQL("DROP TABLE parking_records")
                db.execSQL("ALTER TABLE parking_records_new RENAME TO parking_records")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_parking_records_sessionId ON parking_records(sessionId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parking_logger_db"
                )
                    .addMigrations(MIGRATION_2_4, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
