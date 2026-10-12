package com.parking.logger.data.repository

import com.parking.logger.data.dao.ParkingRecordDao
import com.parking.logger.data.dao.TestSessionDao
import com.parking.logger.data.dao.TesterDao
import com.parking.logger.data.model.ParkingRecord
import com.parking.logger.data.model.TestSession
import com.parking.logger.data.model.Tester
import kotlinx.coroutines.flow.Flow

class ParkingRepository(
    private val testerDao: TesterDao,
    private val testSessionDao: TestSessionDao,
    private val parkingRecordDao: ParkingRecordDao
) {
    val allTesters: Flow<List<Tester>> = testerDao.getAllTesters()

    suspend fun addTester(name: String): Long {
        return testerDao.insert(Tester(name = name))
    }

    suspend fun updateTester(tester: Tester) = testerDao.update(tester)

    suspend fun deleteTester(tester: Tester) = testerDao.delete(tester)

    suspend fun getTester(id: Long): Tester? = testerDao.getTesterById(id)

    suspend fun createSession(testerId: Long, location: String): Long {
        return testSessionDao.insert(TestSession(testerId = testerId, location = location))
    }

    fun getSessions(): Flow<List<TestSession>> = testSessionDao.getAllSessions()

    suspend fun getSession(id: Long): TestSession? = testSessionDao.getSessionById(id)

    suspend fun deleteSession(id: Long) {
        testSessionDao.getSessionById(id)?.let { testSessionDao.delete(it) }
    }

    fun getRecordsBySession(sessionId: Long): Flow<List<ParkingRecord>> =
        parkingRecordDao.getRecordsBySession(sessionId)

    suspend fun saveRecord(record: ParkingRecord): Long {
        return parkingRecordDao.insert(record)
    }

    suspend fun deleteRecord(record: ParkingRecord) = parkingRecordDao.delete(record)

    suspend fun getRecordById(id: Long): ParkingRecord? = parkingRecordDao.getRecordById(id)

    suspend fun updateRecord(record: ParkingRecord) = parkingRecordDao.update(record)
}