package com.inventory.manager.data.database.dao

import androidx.room.*
import com.inventory.manager.data.database.entity.RecordType
import com.inventory.manager.data.database.entity.StockRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM stock_records ORDER BY operationTime DESC")
    fun getAll(): Flow<List<StockRecord>>

    @Query("SELECT * FROM stock_records WHERE deviceId = :deviceId ORDER BY operationTime DESC")
    fun getByDevice(deviceId: Int): Flow<List<StockRecord>>

    @Query("SELECT * FROM stock_records WHERE staffId = :staffId ORDER BY operationTime DESC")
    fun getByStaff(staffId: Int): Flow<List<StockRecord>>

    @Query("SELECT * FROM stock_records WHERE recordType = :type ORDER BY operationTime DESC")
    fun getByType(type: RecordType): Flow<List<StockRecord>>

    @Query("SELECT * FROM stock_records WHERE operationTime >= :from AND operationTime <= :to ORDER BY operationTime DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<StockRecord>>

    @Query("SELECT * FROM stock_records ORDER BY operationTime DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<StockRecord>>

    @Insert
    suspend fun insert(record: StockRecord): Long

    @Query("DELETE FROM stock_records WHERE id = :id")
    suspend fun deleteById(id: Int)
}
