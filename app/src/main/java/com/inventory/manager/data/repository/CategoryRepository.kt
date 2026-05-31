package com.inventory.manager.data.repository

import com.inventory.manager.data.database.dao.CategoryDao
import com.inventory.manager.data.database.entity.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {
    fun getAll(): Flow<List<Category>> = dao.getAll()
    suspend fun getById(id: Int): Category? = dao.getById(id)
    suspend fun insert(category: Category): Long = dao.insert(category)
    suspend fun update(category: Category) = dao.update(category)
    suspend fun delete(category: Category) = dao.delete(category)
    suspend fun count(): Int = dao.count()
}
