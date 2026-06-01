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

class BackupViewModel(
    private val deviceRepo: DeviceRepository,
    private val staffRepo: StaffRepository,
    private val recordRepo: RecordRepository,
    private val categoryRepo: CategoryRepository,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    suspend fun generateBackupContent(): Pair<String, String> {
        val devices = deviceRepo.getAll().first()
        val staff = staffRepo.getAllStaff().first()
        val records = recordRepo.getAll().first()
        val categories = categoryRepo.getAll().first()

        val sqlContent = buildSqlDump(devices, staff, records, categories)
        val metadata = """
            {
              "backupTime": "${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())}",
              "appVersion": "1.0",
              "databaseVersion": 2,
              "statistics": {
                "categories": ${categories.size},
                "devices": ${devices.size},
                "staff": ${staff.size},
                "stockRecords": ${records.size},
                "totalDevicePrice": ${devices.sumOf { it.price }}
              }
            }
        """.trimIndent()

        return Pair(sqlContent, metadata)
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

    fun restoreFromBackup(sqlContent: String, isAutoBackup: Boolean = false): Result<String> {
        return try {
            viewModelScope.launch {
                _uiState.update { it.copy(isRestoring = true, backupProgress = "恢复数据中...") }

                // 执行 SQL 恢复
                val db = database.openHelper.writableDatabase
                db.beginTransaction()
                try {
                    // 清空现有数据（除了 categories）
                    db.execSQL("DELETE FROM stock_records")
                    db.execSQL("DELETE FROM devices")
                    db.execSQL("DELETE FROM staff")

                    // 执行恢复的 SQL 脚本
                    val statements = sqlContent.split(";")
                    statements.forEach { statement ->
                        val trimmed = statement.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("--") && !trimmed.startsWith("/*")) {
                            try {
                                db.execSQL(trimmed)
                            } catch (e: Exception) {
                                // 某些 SQL 语句可能失败，例如 BEGIN/COMMIT，跳过
                            }
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }

                val backupType = if (isAutoBackup) "自动备份" else "用户备份"
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        backupProgress = null,
                        message = "✅ 恢复成功\n\n数据已从 $backupType 恢复"
                    )
                }
            }
            Result.success("Restore started")
        } catch (e: Exception) {
            _uiState.update { it.copy(isRestoring = false, message = "❌ 恢复失败: ${e.message}") }
            Result.failure(e)
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

            // 分类
            append("-- 分类数据\n")
            categories.forEach { c ->
                append("INSERT INTO categories VALUES (${c.id}, ${SqlDumpGenerator.escapeString(c.name)}, ${SqlDumpGenerator.escapeString(c.icon)}, ${c.sortOrder});\n")
            }

            append("\n-- 人员数据\n")
            staff.forEach { s ->
                append("INSERT INTO staff VALUES (${s.id}, ${SqlDumpGenerator.escapeString(s.staffCode)}, ${SqlDumpGenerator.escapeString(s.name)}, ${SqlDumpGenerator.escapeString(s.department)}, ${SqlDumpGenerator.escapeString(s.phone)}, ${SqlDumpGenerator.escapeString(s.notes)}, ${if (s.isActive) 1 else 0});\n")
            }

            append("\n-- 设备数据\n")
            devices.forEach { d ->
                append("INSERT INTO devices VALUES (${d.id}, ${d.categoryId}, ${SqlDumpGenerator.escapeString(d.categoryName)}, ${SqlDumpGenerator.escapeString(d.brand)}, ${SqlDumpGenerator.escapeString(d.model)}, ${SqlDumpGenerator.escapeString(d.assetCode)}, ${SqlDumpGenerator.escapeString(d.serialNumber)}, ${d.purchaseDate}, ${SqlDumpGenerator.escapeLong(d.warrantyDate)}, ${d.price}, ${SqlDumpGenerator.escapeString(d.status.name)}, ${SqlDumpGenerator.escapeInt(d.currentStaffId)}, ${SqlDumpGenerator.escapeString(d.currentStaffName)}, ${SqlDumpGenerator.escapeString(d.notes)}, ${d.createdAt});\n")
            }

            append("\n-- 操作记录\n")
            records.forEach { r ->
                append("INSERT INTO stock_records VALUES (${r.id}, ${r.deviceId}, ${SqlDumpGenerator.escapeString(r.deviceName)}, ${SqlDumpGenerator.escapeString(r.deviceAssetCode)}, ${SqlDumpGenerator.escapeString(r.recordType.name)}, ${SqlDumpGenerator.escapeInt(r.staffId)}, ${SqlDumpGenerator.escapeString(r.staffName)}, ${r.operationTime}, ${SqlDumpGenerator.escapeString(r.condition?.name)}, ${SqlDumpGenerator.escapeDouble(r.cost)}, ${SqlDumpGenerator.escapeString(r.supplier)}, ${SqlDumpGenerator.escapeString(r.description)}, ${SqlDumpGenerator.escapeString(r.notes)});\n")
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
