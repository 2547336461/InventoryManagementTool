package com.inventory.manager.viewmodel

import androidx.lifecycle.*
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.AppDatabase
import com.inventory.manager.data.database.entity.*
import com.inventory.manager.data.repository.*
import com.inventory.manager.utils.SqlDumpGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class BackupUiState(
    val message: String? = null,
    val backupProgress: String? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false
)

data class BackupStats(
    val devices: Int,
    val staff: Int,
    val records: Int,
    val categories: Int,
    val totalDevicePrice: Double
)

class BackupViewModel(
    private val deviceRepo: DeviceRepository,
    private val staffRepo: StaffRepository,
    private val recordRepo: RecordRepository,
    private val categoryRepo: CategoryRepository,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    suspend fun generateBackupContent(): Triple<String, String, BackupStats> {
        val devices = deviceRepo.getAll().first()
        val staff = staffRepo.getAllStaff().first()
        val records = recordRepo.getAll().first()
        val categories = categoryRepo.getAll().first()

        val sqlContent = buildSqlDump(devices, staff, records, categories)
        val stats = BackupStats(
            devices = devices.size,
            staff = staff.size,
            records = records.size,
            categories = categories.size,
            totalDevicePrice = devices.sumOf { it.price }
        )
        val metadata = """
            {
              "backupTime": "${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())}",
              "appVersion": "1.0",
              "databaseVersion": 2,
              "statistics": {
                "categories": ${stats.categories},
                "devices": ${stats.devices},
                "staff": ${stats.staff},
                "stockRecords": ${stats.records},
                "totalDevicePrice": ${stats.totalDevicePrice}
              }
            }
        """.trimIndent()

        return Triple(sqlContent, metadata, stats)
    }

    fun createBackup(sqlContent: String, metadata: String, devices: Int, staff: Int, records: Int): Result<Unit> {
        return try {
            _uiState.update {
                it.copy(
                    isBackingUp = false,
                    backupProgress = null,
                    message = "✅ 备份成功\n\n设备: $devices 台\n人员: $staff 人\n记录: $records 条"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            _uiState.update { it.copy(isBackingUp = false, message = "❌ 备份失败: ${e.message}") }
            Result.failure(e)
        }
    }

    fun restoreFromBackup(sqlContent: String, isAutoBackup: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRestoring = true, backupProgress = "恢复数据中...") }

                // 执行 SQL 恢复
                val db = database.openHelper.writableDatabase
                db.beginTransaction()
                var successCount = 0
                var failureCount = 0
                var failedStatements = mutableListOf<String>()

                try {
                    // 清空现有数据（保留预设分类）
                    db.execSQL("DELETE FROM stock_records")
                    db.execSQL("DELETE FROM devices")
                    db.execSQL("DELETE FROM staff")

                    // 执行恢复的 SQL 脚本
                    val statements = sqlContent.split(";")
                    statements.forEach { statement ->
                        val trimmed = statement.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("--") && !trimmed.startsWith("/*")) {
                            if (!trimmed.startsWith("BEGIN") && !trimmed.startsWith("COMMIT")) {
                                try {
                                    db.execSQL(trimmed)
                                    successCount++
                                } catch (e: Exception) {
                                    failureCount++
                                    failedStatements.add(trimmed.take(100))
                                    android.util.Log.e("BackupRestore", "SQL执行失败: $trimmed\n原因: ${e.message}", e)
                                }
                            }
                        }
                    }
                    db.setTransactionSuccessful()

                    val backupType = if (isAutoBackup) "自动备份" else "用户备份"
                    val message = if (failureCount == 0) {
                        "✅ 恢复成功\n\n数据已从 $backupType 恢复\n成功执行 $successCount 条语句"
                    } else {
                        "⚠️ 部分恢复\n\n成功: $successCount 条\n失败: $failureCount 条\n\n失败的语句:\n${failedStatements.take(3).joinToString("\n")}" +
                        if (failedStatements.size > 3) "\n..." else ""
                    }

                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            backupProgress = null,
                            message = message
                        )
                    }
                } catch (e: Exception) {
                    throw e
                } finally {
                    if (db.inTransaction()) {
                        db.endTransaction()
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        backupProgress = null,
                        message = "❌ 恢复失败\n${e.javaClass.simpleName}\n${e.message}"
                    )
                }
            }
        }
    }


    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun buildSqlDump(
        devices: List<Device>,
        staff: List<Staff>,
        records: List<StockRecord>,
        categories: List<Category>
    ): String {
        return buildString {
            append(SqlDumpGenerator.generateHeader())
            append("\n\n")

            // 分类（跳过预设的 8 个分类，只保留自定义的）
            append("-- 自定义分类数据（保留预设分类）\n")
            categories.filter { it.id > 8 }.forEach { c ->
                append("INSERT INTO categories (id, name, icon, sortOrder) VALUES (${c.id}, ${SqlDumpGenerator.escapeString(c.name)}, ${SqlDumpGenerator.escapeString(c.icon)}, ${c.sortOrder});\n")
            }

            append("\n-- 人员数据\n")
            staff.forEach { s ->
                append("INSERT INTO staff (id, staffCode, name, department, phone, notes, isActive) VALUES (${s.id}, ${SqlDumpGenerator.escapeString(s.staffCode)}, ${SqlDumpGenerator.escapeString(s.name)}, ${SqlDumpGenerator.escapeString(s.department)}, ${SqlDumpGenerator.escapeString(s.phone)}, ${SqlDumpGenerator.escapeString(s.notes)}, ${if (s.isActive) 1 else 0});\n")
            }

            append("\n-- 设备数据\n")
            devices.forEach { d ->
                append("INSERT INTO devices (id, categoryId, categoryName, brand, model, assetCode, serialNumber, purchaseDate, warrantyDate, price, status, currentStaffId, currentStaffName, notes, createdAt) VALUES (${d.id}, ${d.categoryId}, ${SqlDumpGenerator.escapeString(d.categoryName)}, ${SqlDumpGenerator.escapeString(d.brand)}, ${SqlDumpGenerator.escapeString(d.model)}, ${SqlDumpGenerator.escapeString(d.assetCode)}, ${SqlDumpGenerator.escapeString(d.serialNumber)}, ${d.purchaseDate}, ${SqlDumpGenerator.escapeLong(d.warrantyDate)}, ${d.price}, ${SqlDumpGenerator.escapeString(d.status.name)}, ${SqlDumpGenerator.escapeInt(d.currentStaffId)}, ${SqlDumpGenerator.escapeString(d.currentStaffName)}, ${SqlDumpGenerator.escapeString(d.notes)}, ${d.createdAt});\n")
            }

            append("\n-- 操作记录\n")
            records.forEach { r ->
                append("INSERT INTO stock_records (id, deviceId, deviceName, deviceAssetCode, recordType, staffId, staffName, operationTime, condition, cost, supplier, description, notes) VALUES (${r.id}, ${r.deviceId}, ${SqlDumpGenerator.escapeString(r.deviceName)}, ${SqlDumpGenerator.escapeString(r.deviceAssetCode)}, ${SqlDumpGenerator.escapeString(r.recordType.name)}, ${SqlDumpGenerator.escapeInt(r.staffId)}, ${SqlDumpGenerator.escapeString(r.staffName)}, ${r.operationTime}, ${SqlDumpGenerator.escapeString(r.condition?.name)}, ${SqlDumpGenerator.escapeDouble(r.cost)}, ${SqlDumpGenerator.escapeString(r.supplier)}, ${SqlDumpGenerator.escapeString(r.description)}, ${SqlDumpGenerator.escapeString(r.notes)});\n")
            }

            append("\n")
            val maxDeviceId = (devices.maxByOrNull { it.id }?.id ?: 0)
            val maxStaffId = (staff.maxByOrNull { it.id }?.id ?: 0)
            val maxRecordId = (records.maxByOrNull { it.id }?.id ?: 0)
            append(SqlDumpGenerator.generateFooter(maxDeviceId, maxStaffId, maxRecordId))
        }
    }


    companion object {
        fun factory(app: InventoryApp) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BackupViewModel(
                    app.deviceRepository,
                    app.staffRepository,
                    app.recordRepository,
                    app.categoryRepository,
                    app.database
                ) as T
            }
        }
    }
}
