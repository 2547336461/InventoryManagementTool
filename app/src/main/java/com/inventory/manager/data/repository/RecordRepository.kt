package com.inventory.manager.data.repository

import com.inventory.manager.data.database.dao.RecordDao
import com.inventory.manager.data.database.entity.RecordType
import com.inventory.manager.data.database.entity.StockRecord
import kotlinx.coroutines.flow.Flow

class RecordRepository(private val dao: RecordDao) {
    fun getAll(): Flow<List<StockRecord>> = dao.getAll()
    fun getByDevice(deviceId: Int): Flow<List<StockRecord>> = dao.getByDevice(deviceId)
    fun getByStaff(staffId: Int): Flow<List<StockRecord>> = dao.getByStaff(staffId)
    fun getByType(type: RecordType): Flow<List<StockRecord>> = dao.getByType(type)
    fun getByDateRange(from: Long, to: Long): Flow<List<StockRecord>> = dao.getByDateRange(from, to)
    fun getRecent(limit: Int = 10): Flow<List<StockRecord>> = dao.getRecent(limit)
    suspend fun insert(record: StockRecord): Long = dao.insert(record)
    suspend fun deleteById(id: Int) = dao.deleteById(id)
}
