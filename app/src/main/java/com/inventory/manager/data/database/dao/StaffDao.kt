package com.inventory.manager.data.database.dao

import androidx.room.*
import com.inventory.manager.data.database.entity.Staff
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveStaff(): Flow<List<Staff>>

    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun getAllStaff(): Flow<List<Staff>>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getById(id: Int): Staff?

    @Query("SELECT * FROM staff WHERE name LIKE '%' || :query || '%' AND isActive = 1")
    fun searchStaff(query: String): Flow<List<Staff>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(staff: Staff): Long

    @Update
    suspend fun update(staff: Staff)

    @Query("UPDATE staff SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Int)
}
