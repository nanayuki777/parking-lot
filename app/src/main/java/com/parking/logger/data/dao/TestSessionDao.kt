package com.parking.logger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parking.logger.data.model.TestSession
import kotlinx.coroutines.flow.Flow

@Dao
interface TestSessionDao {
    @Query("SELECT * FROM test_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<TestSession>>

    @Query("SELECT * FROM test_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): TestSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TestSession): Long

    @Delete
    suspend fun delete(session: TestSession)

    @Query("DELETE FROM test_sessions")
    suspend fun clearAll()

    @Query("SELECT * FROM test_sessions")
    suspend fun getAllSessionsSync(): List<TestSession>
}