package com.inventory.manager.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecordType(val label: String, val icon: String) {
    STOCK_IN("入库", "📥"),
    STOCK_OUT("出库", "📤"),
    RETURN("归还", "🔄"),
    MAINTENANCE_START("送修", "🔧"),
    MAINTENANCE_END("维修完成", "✅"),
    SCRAP("报废", "🗑️")
}

enum class DeviceCondition(val label: String) {
    GOOD("完好"),
    DAMAGED("有损坏"),
    NEEDS_REPAIR("需维修")
}

@Entity(tableName = "stock_records")
data class StockRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: Int,
    val deviceName: String,
    val deviceAssetCode: String,
    val recordType: RecordType,
    val staffId: Int? = null,
    val staffName: String? = null,
    val operationTime: Long = System.currentTimeMillis(),
    val condition: DeviceCondition? = null,
    val cost: Double? = null,
    val supplier: String? = null,
    val description: String = "",
    val notes: String = ""
)
