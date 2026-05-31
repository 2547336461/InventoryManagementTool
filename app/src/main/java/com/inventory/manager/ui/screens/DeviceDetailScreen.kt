package com.inventory.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.entity.*
import com.inventory.manager.ui.components.ConfirmDialog
import com.inventory.manager.ui.components.SectionHeader
import com.inventory.manager.ui.components.StatusBadge
import com.inventory.manager.utils.DateUtils
import com.inventory.manager.viewmodel.DeviceViewModel
import com.inventory.manager.viewmodel.StaffViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val app = LocalContext.current.applicationContext as InventoryApp
    val deviceVm: DeviceViewModel = viewModel(factory = DeviceViewModel.factory(app))
    val staffVm: StaffViewModel = viewModel(factory = StaffViewModel.factory(app))

    val allDevices by deviceVm.uiState.collectAsStateWithLifecycle()
    val device = allDevices.devices.find { it.id == deviceId }
    val deviceRecords by deviceVm.getDeviceRecords(deviceId).collectAsStateWithLifecycle(emptyList())

    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(allDevices.message) {
        allDevices.message?.let {
            snackbarHostState.showSnackbar(it)
            deviceVm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设备详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (device != null) {
                        IconButton(onClick = { onNavigateToEdit(deviceId) }) {
                            Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (device == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item { DeviceInfoCard(device) }
                item { ActionButtons(device = device, staffVm = staffVm, deviceVm = deviceVm) }
                item { SectionHeader("操作历史 (${deviceRecords.size})") }
                if (deviceRecords.isEmpty()) {
                    item { Text("暂无操作记录", color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) }
                } else {
                    items(deviceRecords) { record -> RecordHistoryItem(record) }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            if (showDeleteDialog) {
                ConfirmDialog(
                    title = "删除设备",
                    text = "确认删除 ${device.displayName}？此操作不可恢复。",
                    onConfirm = { deviceVm.deleteDevice(device); onNavigateBack() },
                    onDismiss = { showDeleteDialog = false }
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(device: Device) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(device.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                StatusBadge(device.status)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            InfoRow("类型", device.categoryName)
            InfoRow("资产编号", device.assetCode)
            if (device.serialNumber.isNotBlank()) InfoRow("序列号", device.serialNumber)
            InfoRow("购入日期", DateUtils.formatDate(device.purchaseDate))
            if (device.warrantyDate != null) InfoRow("保修到期", DateUtils.formatDate(device.warrantyDate))
            if (device.price > 0) InfoRow("购入价格", "¥${String.format("%.2f", device.price)}")
            if (device.currentStaffName != null) InfoRow("当前使用人", device.currentStaffName)
            if (device.notes.isNotBlank()) InfoRow("备注", device.notes)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.width(90.dp), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
private fun ActionButtons(device: Device, staffVm: StaffViewModel, deviceVm: DeviceViewModel) {
    var showStockOut by remember { mutableStateOf(false) }
    var showReturn by remember { mutableStateOf(false) }
    var showMaintStart by remember { mutableStateOf(false) }
    var showMaintEnd by remember { mutableStateOf(false) }
    var showScrap by remember { mutableStateOf(false) }
    val staffState by staffVm.uiState.collectAsStateWithLifecycle()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader("快捷操作")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (device.status == DeviceStatus.IN_STOCK) {
                    Button(onClick = { showStockOut = true }, modifier = Modifier.weight(1f)) { Text("📤 出库") }
                }
                if (device.status == DeviceStatus.IN_USE) {
                    Button(onClick = { showReturn = true }, modifier = Modifier.weight(1f)) { Text("🔄 归还") }
                }
                if (device.status == DeviceStatus.IN_STOCK || device.status == DeviceStatus.IN_USE) {
                    OutlinedButton(onClick = { showMaintStart = true }, modifier = Modifier.weight(1f)) { Text("🔧 送修") }
                }
                if (device.status == DeviceStatus.MAINTENANCE) {
                    Button(onClick = { showMaintEnd = true }, modifier = Modifier.weight(1f)) { Text("✅ 修好了") }
                }
            }
            if (device.status != DeviceStatus.SCRAPPED) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showScrap = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("🗑️ 报废") }
            }
        }
    }

    if (showStockOut) {
        StockOutDialog(device, staffState.staffList, onConfirm = { staff, notes ->
            deviceVm.stockOut(device, staff, notes); showStockOut = false
        }, onDismiss = { showStockOut = false })
    }
    if (showReturn) {
        ReturnDialog(device, onConfirm = { cond, notes ->
            deviceVm.returnDevice(device, cond, notes); showReturn = false
        }, onDismiss = { showReturn = false })
    }
    if (showMaintStart) {
        MaintenanceStartDialog(onConfirm = { desc, notes ->
            deviceVm.startMaintenance(device, desc, notes); showMaintStart = false
        }, onDismiss = { showMaintStart = false })
    }
    if (showMaintEnd) {
        MaintenanceEndDialog(onConfirm = { result, cost, notes ->
            deviceVm.endMaintenance(device, result, cost, notes); showMaintEnd = false
        }, onDismiss = { showMaintEnd = false })
    }
    if (showScrap) {
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showScrap = false },
            title = { Text("确认报废") },
            text = {
                Column {
                    Text("确认将 ${device.displayName} 标记为已报废？")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("报废原因（可选）") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { deviceVm.scrapDevice(device, notes); showScrap = false }) {
                    Text("报废", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showScrap = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun RecordHistoryItem(record: StockRecord) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(record.recordType.icon, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(record.recordType.label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            if (record.staffName != null) Text("操作人: ${record.staffName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            if (record.description.isNotBlank()) Text(record.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            if (record.cost != null) Text("费用: ¥${String.format("%.2f", record.cost)}", fontSize = 12.sp)
            if (record.notes.isNotBlank()) Text(record.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        }
        Text(DateUtils.formatDateTime(record.operationTime), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
    }
    HorizontalDivider(thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOutDialog(device: Device, staffList: List<Staff>, onConfirm: (Staff, String) -> Unit, onDismiss: () -> Unit) {
    var selectedStaff by remember { mutableStateOf<Staff?>(null) }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("出库 - ${device.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedStaff?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择领用人 *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        staffList.forEach { staff ->
                            DropdownMenuItem(
                                text = { Text("${staff.name}${if (staff.department.isNotBlank()) " (${staff.department})" else ""}") },
                                onClick = { selectedStaff = staff; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { selectedStaff?.let { onConfirm(it, notes) } }, enabled = selectedStaff != null) { Text("确认出库") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ReturnDialog(device: Device, onConfirm: (DeviceCondition, String) -> Unit, onDismiss: () -> Unit) {
    var condition by remember { mutableStateOf(DeviceCondition.GOOD) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("归还 - ${device.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("归还人: ${device.currentStaffName ?: "-"}")
                Text("设备状况:", fontWeight = FontWeight.Medium)
                DeviceCondition.entries.forEach { cond ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = condition == cond, onClick = { condition = cond })
                        Text(cond.label)
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(condition, notes) }) { Text("确认归还") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun MaintenanceStartDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("送修登记") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("故障描述 *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(description, notes) }, enabled = description.isNotBlank()) { Text("确认送修") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun MaintenanceEndDialog(onConfirm: (String, Double?, String) -> Unit, onDismiss: () -> Unit) {
    var result by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("维修完成登记") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = result, onValueChange = { result = it }, label = { Text("维修结果 *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = costStr, onValueChange = { costStr = it }, label = { Text("维修费用（元）") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(result, costStr.toDoubleOrNull(), notes) }, enabled = result.isNotBlank()) { Text("确认完成") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
