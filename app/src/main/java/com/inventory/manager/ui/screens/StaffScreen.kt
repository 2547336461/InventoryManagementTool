package com.inventory.manager.ui.screens

import androidx.compose.foundation.clickable
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
import com.inventory.manager.data.database.entity.Device
import com.inventory.manager.data.database.entity.Staff
import com.inventory.manager.ui.components.ConfirmDialog
import com.inventory.manager.ui.components.EmptyState
import com.inventory.manager.ui.components.StatusBadge
import com.inventory.manager.viewmodel.StaffViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListScreen(onNavigateBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as InventoryApp
    val vm: StaffViewModel = viewModel(factory = StaffViewModel.factory(app))
    val state by vm.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<Staff?>(null) }
    var deletingStaff by remember { mutableStateOf<Staff?>(null) }
    var expandedStaffId by remember { mutableStateOf<Int?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("人员管理", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.PersonAdd, "添加员工") } },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.staffList.isEmpty()) {
            EmptyState("暂无员工，点击右下角添加", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(state.staffList, key = { it.id }) { staff ->
                    StaffCard(
                        staff = staff,
                        vm = vm,
                        isExpanded = expandedStaffId == staff.id,
                        onToggleExpand = { expandedStaffId = if (expandedStaffId == staff.id) null else staff.id },
                        onEdit = { editingStaff = staff },
                        onDelete = { deletingStaff = staff }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        StaffEditDialog(
            staff = null,
            onSave = { vm.insertStaff(it); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
    editingStaff?.let { s ->
        StaffEditDialog(
            staff = s,
            onSave = { vm.updateStaff(it); editingStaff = null },
            onDismiss = { editingStaff = null }
        )
    }
    deletingStaff?.let { s ->
        ConfirmDialog(
            title = "移除员工",
            text = "确认移除 ${s.name}？",
            onConfirm = { vm.deleteStaff(s); deletingStaff = null },
            onDismiss = { deletingStaff = null }
        )
    }
}

@Composable
private fun StaffCard(
    staff: Staff,
    vm: StaffViewModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val devices by vm.getStaffDevices(staff.id).collectAsStateWithLifecycle(emptyList())

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(staff.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (staff.department.isNotBlank()) {
                        Text(staff.department, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                    if (staff.phone.isNotBlank()) {
                        Text(staff.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
                if (devices.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text("${devices.size}台设备", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.width(4.dp))
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }

            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                if (devices.isEmpty()) {
                    Text("当前没有持有设备", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                } else {
                    devices.forEach { device -> DeviceChip(device) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("编辑")
                    }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.PersonRemove, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("移除")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceChip(device: Device) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        StatusBadge(device.status)
        Spacer(Modifier.width(8.dp))
        Text("${device.displayName} (${device.assetCode})", fontSize = 13.sp)
    }
}

@Composable
private fun StaffEditDialog(staff: Staff?, onSave: (Staff) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(staff?.name ?: "") }
    var department by remember { mutableStateOf(staff?.department ?: "") }
    var phone by remember { mutableStateOf(staff?.phone ?: "") }
    var notes by remember { mutableStateOf(staff?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (staff == null) "添加员工" else "编辑员工") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名 *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("部门") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("电话") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(Staff(id = staff?.id ?: 0, name = name.trim(), department = department.trim(), phone = phone.trim(), notes = notes.trim()))
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
