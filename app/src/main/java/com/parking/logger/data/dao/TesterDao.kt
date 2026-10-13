package com.parking.logger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.parking.logger.data.model.Tester
import kotlinx.coroutines.flow.Flow

@Dao
interface TesterDao {
    @Query("SELECT * FROM testers ORDER BY name ASC")
    fun getAllTesters(): Flow<List<Tester>>

    @Query("SELECT * FROM testers WHERE id = :id")
    suspend fun getTesterById(id: Long): Tester?

    @Query("SELECT * FROM testers")
    suspend fun getAllTestersSync(): List<Tester>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tester: Tester): Long

    @Update
    suspend fun update(tester: Tester)

    @Delete
    suspend fun delete(tester: Tester)

    @Query("DELETE FROM testers")
    suspend fun clearAll()
}