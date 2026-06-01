package com.inventory.manager.ui.screens

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.entity.Device
import com.inventory.manager.data.database.entity.DeviceCondition
import com.inventory.manager.data.database.entity.RecordType
import com.inventory.manager.data.database.entity.Staff
import com.inventory.manager.data.database.entity.StockRecord
import com.inventory.manager.viewmodel.BackupViewModel
import com.inventory.manager.viewmodel.DeviceViewModel
import com.inventory.manager.viewmodel.RecordViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToStaff: () -> Unit,
    onNavigateToCategories: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as InventoryApp
    val deviceVm: DeviceViewModel = viewModel(factory = DeviceViewModel.factory(app))
    val recordVm: RecordViewModel = viewModel(factory = RecordViewModel.factory(app))
    val backupVm: BackupViewModel = viewModel(factory = BackupViewModel.factory(app))
    val scope = rememberCoroutineScope()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showBackupConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var restoreFileContent by remember { mutableStateOf<String?>(null) }

    val backupState by backupVm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { snackbarHostState.showSnackbar(it); statusMessage = null }
    }

    LaunchedEffect(backupState.message) {
        backupState.message?.let { snackbarHostState.showSnackbar(it); backupVm.clearMessage() }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isEmpty()) {
                        statusMessage = "备份文件为空"
                        return@launch
                    }

                    val content = if (bytes.size > 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                        // ZIP 文件，提取 backup.sql
                        try {
                            val zipInputStream = java.util.zip.ZipInputStream(bytes.inputStream())
                            var sqlContent = ""
                            var entry = zipInputStream.nextEntry
                            while (entry != null) {
                                if (entry.name == "backup.sql") {
                                    sqlContent = zipInputStream.bufferedReader(Charsets.UTF_8).readText()
                                    break
                                }
                                entry = zipInputStream.nextEntry
                            }
                            zipInputStream.close()

                            if (sqlContent.isEmpty()) {
                                statusMessage = "ZIP 文件中未找到 backup.sql"
                                return@launch
                            }
                            sqlContent
                        } catch (e: Exception) {
                            statusMessage = "解析 ZIP 文件失败: ${e.message}"
                            return@launch
                        }
                    } else {
                        // 直接的 SQL 文件
                        try {
                            String(bytes, Charsets.UTF_8)
                        } catch (e: Exception) {
                            statusMessage = "读取 SQL 文件失败: ${e.message}"
                            return@launch
                        }
                    }

                    if (content.isBlank()) {
                        statusMessage = "备份文件内容为空"
                        return@launch
                    }

                    restoreFileContent = content
                    showRestoreConfirm = true
                }
            } catch (e: Exception) {
                statusMessage = "读取备份文件失败: ${e.message}"
            }
        }
    }

    val importStaffLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                var count = 0
                var skipped = 0
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val lines = stream.bufferedReader(Charsets.UTF_8).readLines()
                    val existingStaff = app.staffRepository.getActiveStaff().first()
                    val existingCodes = existingStaff.map { it.staffCode }.filter { it.isNotBlank() }.toHashSet()
                    val existingNames = existingStaff.map { it.name }.toHashSet()
                    lines.drop(1).forEach { line ->
                        if (line.isBlank()) return@forEach
                        val cols = parseCsvLine(line)
                        if (cols.size < 2) return@forEach
                        val staffCode = cols[0].trim()
                        val name = cols[1].trim()
                        if (name.isBlank()) return@forEach
                        // 优先按员工编号去重，无编号时按姓名去重
                        if (staffCode.isNotBlank() && existingCodes.contains(staffCode)) { skipped++; return@forEach }
                        if (staffCode.isBlank() && existingNames.contains(name)) { skipped++; return@forEach }
                        val department = if (cols.size > 2) cols[2].trim() else ""
                        val phone = if (cols.size > 3) cols[3].trim() else ""
                        val notes = if (cols.size > 4) cols[4].trim() else ""
                        app.staffRepository.insert(Staff(staffCode = staffCode, name = name, department = department, phone = phone, notes = notes))
                        existingCodes.add(staffCode)
                        existingNames.add(name)
                        count++
                    }
                }
                statusMessage = if (skipped > 0) "导入 $count 名员工，跳过 $skipped 条重名"
                               else "成功导入 $count 名员工"
            } catch (e: Exception) {
                statusMessage = "导入失败: ${e.message}"
            }
        }
    }

    val importRecordsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                var count = 0
                var skipped = 0
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val lines = stream.bufferedReader(Charsets.UTF_8).readLines()
                    lines.drop(1).forEach { line ->
                        if (line.isBlank()) return@forEach
                        val cols = parseCsvLine(line)
                        if (cols.size < 8) return@forEach
                        val operationTime = cols[0].trim().toLongOrNull() ?: return@forEach
                        val recordType = RecordType.entries.find { it.label == cols[1].trim() } ?: return@forEach
                        val deviceName = cols[2].trim()
                        val deviceAssetCode = cols[3].trim()
                        val staffName = cols[4].trim().takeIf { it.isNotBlank() }
                        val supplier = cols[5].trim().takeIf { it.isNotBlank() }
                        val condition = DeviceCondition.entries.find { it.label == cols[6].trim() }
                        val cost = cols[7].trim().toDoubleOrNull()
                        val description = if (cols.size > 8) cols[8].trim() else ""
                        val notes = if (cols.size > 9) cols[9].trim() else ""
                        val serialNumber = if (cols.size > 10) cols[10].trim() else ""
                        // 优先按序列号查找（条形码扫描可靠），找不到再按资产编号兜底
                        val device = if (serialNumber.isNotBlank())
                            app.deviceRepository.getBySerialNumber(serialNumber)
                                ?: app.deviceRepository.getByAssetCode(deviceAssetCode)
                        else
                            app.deviceRepository.getByAssetCode(deviceAssetCode)
                        if (device == null) { skipped++; return@forEach }
                        app.recordRepository.insert(
                            StockRecord(
                                deviceId = device.id,
                                deviceName = deviceName.ifBlank { device.displayName },
                                deviceAssetCode = deviceAssetCode,
                                recordType = recordType,
                                staffName = staffName,
                                operationTime = operationTime,
                                condition = condition,
                                cost = cost,
                                supplier = supplier,
                                description = description,
                                notes = notes
                            )
                        )
                        count++
                    }
                }
                statusMessage = if (skipped > 0) "导入 $count 条记录，跳过 $skipped 条（设备不存在）"
                               else "成功导入 $count 条操作记录"
            } catch (e: Exception) {
                statusMessage = "导入失败: ${e.message}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                var count = 0
                var skipped = 0
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val lines = stream.bufferedReader(Charsets.UTF_8).readLines()
                    val categories = app.categoryRepository.getAll().first()
                    lines.drop(1).forEach { line ->
                        if (line.isBlank()) return@forEach
                        val cols = parseCsvLine(line)
                        if (cols.size < 9) return@forEach
                        val assetCode = cols[0].trim()
                        val brand = cols[1].trim()
                        val model = cols[2].trim()
                        val categoryName = cols[3].trim()
                        val serialNumber = cols[5].trim()
                        val purchaseDate = cols[6].trim().toLongOrNull() ?: System.currentTimeMillis()
                        val warrantyDate = cols[7].trim().toLongOrNull()
                        val price = cols[8].trim().toDoubleOrNull() ?: 0.0
                        val notes = if (cols.size > 10) cols[10].trim() else ""
                        if (brand.isBlank() || model.isBlank()) return@forEach
                        // 跳过资产编号与现有非报废设备重复的行
                        if (assetCode.isNotBlank() && app.deviceRepository.getByAssetCodeNonScrapped(assetCode) != null) {
                            skipped++
                            return@forEach
                        }
                        val category = categories.find { it.name == categoryName }
                            ?: categories.firstOrNull()
                            ?: return@forEach
                        val device = Device(
                            categoryId = category.id,
                            categoryName = category.name,
                            brand = brand,
                            model = model,
                            assetCode = assetCode,
                            serialNumber = serialNumber,
                            purchaseDate = purchaseDate,
                            warrantyDate = warrantyDate,
                            price = price,
                            notes = notes
                        )
                        val deviceId = app.deviceRepository.insert(device).toInt()
                        app.recordRepository.insert(
                            StockRecord(
                                deviceId = deviceId,
                                deviceName = device.displayName,
                                deviceAssetCode = device.assetCode,
                                recordType = RecordType.STOCK_IN,
                                notes = "CSV导入"
                            )
                        )
                        count++
                    }
                }
                statusMessage = if (skipped > 0) "导入 $count 台，跳过 $skipped 条重复编号"
                               else "成功导入 $count 台设备"
            } catch (e: Exception) {
                statusMessage = "导入失败: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("更多", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            MoreItem(icon = Icons.Default.People, title = "人员管理", subtitle = "添加、编辑员工信息", onClick = onNavigateToStaff)
            MoreItem(icon = Icons.Default.Category, title = "设备类型管理", subtitle = "自定义设备分类", onClick = onNavigateToCategories)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MoreItem(
                icon = Icons.Default.Download,
                title = "导出设备清单 (CSV)",
                subtitle = "导出所有设备数据到下载目录",
                onClick = {
                    scope.launch {
                        try {
                            val devices = app.deviceRepository.getAll().first()
                            val csv = buildString {
                                appendLine("资产编号,品牌,型号,类型,状态,序列号,购入日期,保修到期,价格,当前使用人,备注")
                                devices.forEach { d ->
                                    appendLine("${d.assetCode},${d.brand},${d.model},${d.categoryName},${d.status.label},${d.serialNumber},${d.purchaseDate},${d.warrantyDate ?: ""},${d.price},${d.currentStaffName ?: ""},\"${d.notes}\"")
                                }
                            }
                            saveCSV(context, "devices_export.csv", csv)
                            statusMessage = "设备清单已导出到下载目录"
                        } catch (e: Exception) {
                            statusMessage = "导出失败: ${e.message}"
                        }
                    }
                }
            )

            MoreItem(
                icon = Icons.Default.Upload,
                title = "导入设备数据 (CSV)",
                subtitle = "从导出的 CSV 文件恢复/追加设备",
                onClick = { importLauncher.launch(arrayOf("text/*", "*/*")) }
            )

            MoreItem(
                icon = Icons.Default.History,
                title = "导出操作记录 (CSV)",
                subtitle = "导出所有操作历史到下载目录",
                onClick = {
                    scope.launch {
                        try {
                            val records = app.recordRepository.getAll().first()
                            val serialByCode = app.deviceRepository.getAll().first()
                                .associateBy({ it.assetCode }, { it.serialNumber })
                            val csv = buildString {
                                appendLine("操作时间,操作类型,设备名称,资产编号,操作人员,供应商,设备状况,维修费用,描述,备注,序列号")
                                records.forEach { r ->
                                    val serial = serialByCode[r.deviceAssetCode] ?: ""
                                    appendLine("${r.operationTime},${r.recordType.label},${r.deviceName},${r.deviceAssetCode},${r.staffName ?: ""},${r.supplier ?: ""},${r.condition?.label ?: ""},${r.cost ?: ""},\"${r.description}\",\"${r.notes}\",$serial")
                                }
                            }
                            saveCSV(context, "records_export.csv", csv)
                            statusMessage = "操作记录已导出到下载目录"
                        } catch (e: Exception) {
                            statusMessage = "导出失败: ${e.message}"
                        }
                    }
                }
            )

            MoreItem(
                icon = Icons.Default.FileUpload,
                title = "导入操作记录 (CSV)",
                subtitle = "从导出的记录 CSV 文件恢复历史操作",
                onClick = { importRecordsLauncher.launch(arrayOf("text/*", "*/*")) }
            )

            MoreItem(
                icon = Icons.Default.PersonSearch,
                title = "导出人员数据 (CSV)",
                subtitle = "导出所有员工信息到下载目录",
                onClick = {
                    scope.launch {
                        try {
                            val staffList = app.staffRepository.getActiveStaff().first()
                            val csv = buildString {
                                appendLine("员工编号,姓名,部门,电话,备注")
                                staffList.forEach { s ->
                                    appendLine("${s.staffCode},${s.name},${s.department},${s.phone},\"${s.notes}\"")
                                }
                            }
                            saveCSV(context, "staff_export.csv", csv)
                            statusMessage = "人员数据已导出到下载目录"
                        } catch (e: Exception) {
                            statusMessage = "导出失败: ${e.message}"
                        }
                    }
                }
            )

            MoreItem(
                icon = Icons.Default.GroupAdd,
                title = "导入人员数据 (CSV)",
                subtitle = "从 CSV 批量添加员工，重名自动跳过",
                onClick = { importStaffLauncher.launch(arrayOf("text/*", "*/*")) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MoreItem(
                icon = Icons.Default.DownloadForOffline,
                title = "完整数据备份 (SQL)",
                subtitle = if (backupState.isBackingUp) "备份中..." else "备份所有数据到文件",
                onClick = { showBackupConfirm = true }
            )

            MoreItem(
                icon = Icons.Default.Restore,
                title = "数据恢复",
                subtitle = if (backupState.isRestoring) "恢复中..." else "从备份文件恢复数据（仅限 .zip）",
                onClick = { restoreLauncher.launch(arrayOf("application/zip")) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关于", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("库存管理工具 v1.0", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    Text("适用于小型工作室的设备资产管理", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    Text("本地存储，无需联网", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            }
        }
    }

    if (showBackupConfirm) {
        AlertDialog(
            onDismissRequest = { showBackupConfirm = false },
            title = { Text("确认备份") },
            text = { Text("确认要备份所有数据吗？\n\n备份文件将保存到\nDownloads/inventory_backups/") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupConfirm = false
                        scope.launch {
                            try {
                                val (sqlContent, metadata) = backupVm.generateBackupContent()
                                val devices = app.deviceRepository.getAll().first()
                                val staff = app.staffRepository.getAllStaff().first()
                                val records = app.recordRepository.getAll().first()

                                val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                                val timestamp = timeFormat.format(Date())
                                val zipFileName = "inventory_backup_$timestamp.zip"

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.Downloads.DISPLAY_NAME, zipFileName)
                                        put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                                        put(MediaStore.Downloads.IS_PENDING, 1)
                                    }
                                    val resolver = context.contentResolver
                                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                                    uri?.let {
                                        resolver.openOutputStream(it)?.use { os ->
                                            val sqlFile = File(context.cacheDir, "backup.sql")
                                            val metadataFile = File(context.cacheDir, "metadata.json")
                                            sqlFile.writeText(sqlContent, Charsets.UTF_8)
                                            metadataFile.writeText(metadata, Charsets.UTF_8)

                                            val zos = java.util.zip.ZipOutputStream(os)
                                            zos.putNextEntry(java.util.zip.ZipEntry("backup.sql"))
                                            sqlFile.inputStream().use { it.copyTo(zos) }
                                            zos.closeEntry()
                                            zos.putNextEntry(java.util.zip.ZipEntry("metadata.json"))
                                            metadataFile.inputStream().use { it.copyTo(zos) }
                                            zos.closeEntry()
                                            zos.close()

                                            sqlFile.delete()
                                            metadataFile.delete()
                                        }
                                        values.clear()
                                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                                        resolver.update(it, values, null, null)
                                    }
                                } else {
                                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    val backupDir = File(dir, "inventory_backups")
                                    backupDir.mkdirs()

                                    val sqlFile = File(backupDir, "backup_$timestamp.sql")
                                    val metadataFile = File(backupDir, "metadata_$timestamp.json")
                                    sqlFile.writeText(sqlContent, Charsets.UTF_8)
                                    metadataFile.writeText(metadata, Charsets.UTF_8)

                                    val zipFile = File(backupDir, zipFileName)
                                    val zos = java.util.zip.ZipOutputStream(zipFile.outputStream())
                                    zos.putNextEntry(java.util.zip.ZipEntry("backup.sql"))
                                    sqlFile.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                    zos.putNextEntry(java.util.zip.ZipEntry("metadata.json"))
                                    metadataFile.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                    zos.close()

                                    sqlFile.delete()
                                    metadataFile.delete()
                                }

                                backupVm.createBackup(sqlContent, metadata, devices.size, staff.size, records.size)
                            } catch (e: Exception) {
                                statusMessage = "备份失败: ${e.message}"
                            }
                        }
                    }
                ) { Text("确认备份") }
            },
            dismissButton = {
                TextButton(onClick = { showBackupConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showRestoreConfirm && restoreFileContent != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("确认恢复") },
            text = {
                Text(
                    """
                    ⚠️ 警告：恢复将替换所有当前数据

                    系统将在恢复前自动备份当前数据

                    确认要继续恢复吗？
                    """.trimIndent()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        val content = restoreFileContent
                        restoreFileContent = null

                        if (content == null) {
                            statusMessage = "恢复文件内容为空"
                            return@TextButton
                        }

                        scope.launch {
                            try {
                                // 先创建自动备份
                                val (sqlContent, _) = backupVm.generateBackupContent()
                                val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                                val timestamp = timeFormat.format(Date())
                                val autoBackupFileName = "inventory_auto_backup_before_restore_$timestamp.sql"

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.Downloads.DISPLAY_NAME, autoBackupFileName)
                                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                                        put(MediaStore.Downloads.IS_PENDING, 1)
                                    }
                                    val resolver = context.contentResolver
                                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                                    uri?.let {
                                        resolver.openOutputStream(it)?.use { os ->
                                            os.write(sqlContent.toByteArray(Charsets.UTF_8))
                                        }
                                        values.clear()
                                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                                        resolver.update(it, values, null, null)
                                    }
                                } else {
                                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    val backupDir = File(dir, "inventory_backups")
                                    backupDir.mkdirs()
                                    File(backupDir, autoBackupFileName).writeText(sqlContent, Charsets.UTF_8)
                                }

                                // 执行恢复（不需要额外处理异常，ViewModel 会处理）
                                backupVm.restoreFromBackup(content)
                            } catch (e: Exception) {
                                statusMessage = "创建自动备份失败: ${e.message}"
                            }
                        }
                    }
                ) { Text("确认恢复") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false; restoreFileContent = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MoreItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
        }
    }
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var inQuote = false
    val current = StringBuilder()
    for (ch in line) {
        when {
            ch == '"' -> inQuote = !inQuote
            ch == ',' && !inQuote -> { result.add(current.toString()); current.clear() }
            else -> current.append(ch)
        }
    }
    result.add(current.toString())
    return result
}

private fun saveCSV(context: android.content.Context, filename: String, content: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            resolver.openOutputStream(it)?.use { os -> os.write(content.toByteArray(Charsets.UTF_8)) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(it, values, null, null)
        }
    } else {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        File(dir, filename).writeText(content, Charsets.UTF_8)
    }
}
