package com.inventory.manager.data.repository

import com.inventory.manager.data.database.dao.StaffDao
import com.inventory.manager.data.database.entity.Staff
import kotlinx.coroutines.flow.Flow

class StaffRepository(private val dao: StaffDao) {
    fun getActiveStaff(): Flow<List<Staff>> = dao.getActiveStaff()
    fun getAllStaff(): Flow<List<Staff>> = dao.getAllStaff()
    suspend fun getById(id: Int): Staff? = dao.getById(id)
    fun searchStaff(query: String): Flow<List<Staff>> = dao.searchStaff(query)
    suspend fun getByStaffCode(code: String): Staff? = dao.getByStaffCode(code)
    suspend fun insert(staff: Staff): Long = dao.insert(staff)
    suspend fun update(staff: Staff) = dao.update(staff)
    suspend fun softDelete(id: Int) = dao.softDelete(id)
}
