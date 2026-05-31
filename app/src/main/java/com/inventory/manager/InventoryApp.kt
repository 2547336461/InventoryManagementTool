package com.inventory.manager

import android.app.Application
import com.inventory.manager.data.database.AppDatabase
import com.inventory.manager.data.repository.*

class InventoryApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val staffRepository by lazy { StaffRepository(database.staffDao()) }
    val deviceRepository by lazy { DeviceRepository(database.deviceDao()) }
    val recordRepository by lazy { RecordRepository(database.recordDao()) }
}
