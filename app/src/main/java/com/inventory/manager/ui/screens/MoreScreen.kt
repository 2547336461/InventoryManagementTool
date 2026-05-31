package com.inventory.manager.ui.screens

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inventory.manager.InventoryApp
import com.inventory.manager.viewmodel.DeviceViewModel
import com.inventory.manager.viewmodel.RecordViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

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
    val scope = rememberCoroutineScope()

    var exportMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportMessage) {
        exportMessage?.let { snackbarHostState.showSnackbar(it); exportMessage = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("更多", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            exportMessage = "设备清单已导出到下载目录"
                        } catch (e: Exception) {
                            exportMessage = "导出失败: ${e.message}"
                        }
                    }
                }
            )

            MoreItem(
                icon = Icons.Default.History,
                title = "导出操作记录 (CSV)",
                subtitle = "导出所有操作历史到下载目录",
                onClick = {
                    scope.launch {
                        try {
                            val records = app.recordRepository.getAll().first()
                            val csv = buildString {
                                appendLine("操作时间,操作类型,设备名称,资产编号,操作人员,供应商,设备状况,维修费用,描述,备注")
                                records.forEach { r ->
                                    appendLine("${r.operationTime},${r.recordType.label},${r.deviceName},${r.deviceAssetCode},${r.staffName ?: ""},${r.supplier ?: ""},${r.condition?.label ?: ""},${r.cost ?: ""},\"${r.description}\",\"${r.notes}\"")
                                }
                            }
                            saveCSV(context, "records_export.csv", csv)
                            exportMessage = "操作记录已导出到下载目录"
                        } catch (e: Exception) {
                            exportMessage = "导出失败: ${e.message}"
                        }
                    }
                }
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
