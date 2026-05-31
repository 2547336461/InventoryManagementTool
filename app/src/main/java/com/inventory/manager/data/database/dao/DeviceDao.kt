package com.inventory.manager.data.database.dao

import androidx.room.*
import com.inventory.manager.data.database.entity.Device
import com.inventory.manager.data.database.entity.DeviceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getByCategory(categoryId: Int): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: DeviceStatus): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE categoryId = :categoryId AND status = :status ORDER BY createdAt DESC")
    fun getByCategoryAndStatus(categoryId: Int, status: DeviceStatus): Flow<List<Device>>

    @Query("""SELECT * FROM devices WHERE
        brand LIKE '%' || :query || '%' OR
        model LIKE '%' || :query || '%' OR
        assetCode LIKE '%' || :query || '%' OR
        serialNumber LIKE '%' || :query || '%'
        ORDER BY createdAt DESC""")
    fun search(query: String): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getById(id: Int): Device?

    @Query("SELECT COUNT(*) FROM devices WHERE status = :status")
    fun countByStatus(status: DeviceStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM devices")
    fun countAll(): Flow<Int>

    @Query("SELECT COUNT(*) FROM devices WHERE categoryId = :categoryId")
    fun countByCategory(categoryId: Int): Flow<Int>

    @Query("""SELECT * FROM devices WHERE
        warrantyDate IS NOT NULL AND
        warrantyDate <= :deadline AND
        status != 'SCRAPPED'
        ORDER BY warrantyDate ASC""")
    fun getWarrantyExpiring(deadline: Long): Flow<List<Device>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: Device): Long

    @Update
    suspend fun update(device: Device)

    @Delete
    suspend fun delete(device: Device)

    @Query("SELECT * FROM devices WHERE currentStaffId = :staffId")
    fun getByStaff(staffId: Int): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE assetCode = :code ORDER BY createdAt DESC LIMIT 1")
    suspend fun getByAssetCode(code: String): Device?

    @Query("SELECT * FROM devices WHERE serialNumber = :serialNumber ORDER BY CASE status WHEN 'SCRAPPED' THEN 1 ELSE 0 END, createdAt DESC LIMIT 1")
    suspend fun getBySerialNumber(serialNumber: String): Device?

    @Query("SELECT * FROM devices WHERE assetCode = :code AND status != 'SCRAPPED' AND id != :excludeId LIMIT 1")
    suspend fun getByAssetCodeNonScrapped(code: String, excludeId: Int = -1): Device?

    @Query("SELECT COUNT(*) FROM devices WHERE categoryId = :categoryId")
    suspend fun countByCategorySync(categoryId: Int): Int

    @Query("SELECT * FROM devices WHERE currentStaffId = :staffId AND status = 'IN_USE'")
    suspend fun getInUseByStaff(staffId: Int): List<Device>
}
