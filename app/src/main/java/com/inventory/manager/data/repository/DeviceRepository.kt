package com.inventory.manager.data.repository

import com.inventory.manager.data.database.dao.DeviceDao
import com.inventory.manager.data.database.entity.Device
import com.inventory.manager.data.database.entity.DeviceStatus
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val dao: DeviceDao) {
    fun getAll(): Flow<List<Device>> = dao.getAll()
    fun getByCategory(categoryId: Int): Flow<List<Device>> = dao.getByCategory(categoryId)
    fun getByStatus(status: DeviceStatus): Flow<List<Device>> = dao.getByStatus(status)
    fun getByCategoryAndStatus(categoryId: Int, status: DeviceStatus): Flow<List<Device>> =
        dao.getByCategoryAndStatus(categoryId, status)
    fun search(query: String): Flow<List<Device>> = dao.search(query)
    suspend fun getById(id: Int): Device? = dao.getById(id)
    fun countByStatus(status: DeviceStatus): Flow<Int> = dao.countByStatus(status)
    fun countAll(): Flow<Int> = dao.countAll()
    fun countByCategory(categoryId: Int): Flow<Int> = dao.countByCategory(categoryId)
    fun getWarrantyExpiring(deadline: Long): Flow<List<Device>> = dao.getWarrantyExpiring(deadline)
    fun getByStaff(staffId: Int): Flow<List<Device>> = dao.getByStaff(staffId)
    suspend fun insert(device: Device): Long = dao.insert(device)
    suspend fun update(device: Device) = dao.update(device)
    suspend fun delete(device: Device) = dao.delete(device)
}
