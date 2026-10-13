package com.parking.logger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.parking.logger.data.model.ParkingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingRecordDao {
    @Query("SELECT * FROM parking_records WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getRecordsBySession(sessionId: Long): Flow<List<ParkingRecord>>

    @Query("SELECT * FROM parking_records WHERE id = :id")
    suspend fun getRecordById(id: Long): ParkingRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ParkingRecord): Long

    @Update
    suspend fun update(record: ParkingRecord)

    @Delete
    suspend fun delete(record: ParkingRecord)

    @Query("SELECT * FROM parking_records")
    suspend fun getAllRecords(): List<ParkingRecord>

    @Query("DELETE FROM parking_records")
    suspend fun clearAll()
}