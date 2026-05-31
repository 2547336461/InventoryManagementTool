package com.inventory.manager.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DeviceStatus(val label: String) {
    IN_STOCK("在库"),
    IN_USE("使用中"),
    MAINTENANCE("维修中"),
    SCRAPPED("已报废")
}

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val categoryName: String,
    val brand: String,
    val model: String,
    val assetCode: String,
    val serialNumber: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val warrantyDate: Long? = null,
    val price: Double = 0.0,
    val status: DeviceStatus = DeviceStatus.IN_STOCK,
    val currentStaffId: Int? = null,
    val currentStaffName: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String get() = "$brand $model"
}
