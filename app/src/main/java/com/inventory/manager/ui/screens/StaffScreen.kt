package com.inventory.manager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    var selectedDepartment by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val departments = state.staffList.map { it.department }.filter { it.isNotBlank() }.distinct().sorted()
    val displayedStaff = if (selectedDepartment == null) state.staffList
                         else state.staffList.filter { it.department == selectedDepartment }

    LaunchedEffect(departments) {
        if (selectedDepartment != null && selectedDepartment !in departments) {
            selectedDepartment = null
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessage() }
    }

    val generateCode: suspend () -> String = {
        val codes = app.staffRepository.getAllStaff().first().map { it.staffCode }
        vm.generateStaffCode(codes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("人员管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.PersonAdd, "添加员工")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                placeholder = { Text("搜索姓名、编号、部门、电话...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { vm.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (departments.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDepartment == null,
                            onClick = { selectedDepartment = null },
                            label = { Text("全部") }
                        )
                    }
                    items(departments) { dept ->
                        FilterChip(
                            selected = selectedDepartment == dept,
                            onClick = { selectedDepartment = if (selectedDepartment == dept) null else dept },
                            label = { Text(dept) }
                        )
                    }
                }
            }

            if (displayedStaff.isEmpty()) {
                EmptyState(
                    message = if (state.searchQuery.isNotBlank() || selectedDepartment != null)
                        "无匹配员工" else "暂无员工，点击右下角添加",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(displayedStaff, key = { it.id }) { staff ->
                        StaffCard(
                            staff = staff,
                            vm = vm,
                            isExpanded = expandedStaffId == staff.id,
                            onToggleExpand = {
                                expandedStaffId = if (expandedStaffId == staff.id) null else staff.id
                            },
                            onEdit = { editingStaff = staff },
                            onDelete = { deletingStaff = staff }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        StaffEditDialog(
            staff = null,
            onSave = { vm.insertStaff(it); showAddDialog = false },
            onDismiss = { showAddDialog = false },
            onGenerateCode = generateCode
        )
    }
    editingStaff?.let { s ->
        StaffEditDialog(
            staff = s,
            onSave = { vm.updateStaff(it); editingStaff = null },
            onDismiss = { editingStaff = null },
            onGenerateCode = generateCode
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(staff.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (staff.staffCode.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    staff.staffCode,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
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
                        Text(
                            "${devices.size}台设备",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
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
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
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
private fun StaffEditDialog(
    staff: Staff?,
    onSave: (Staff) -> Unit,
    onDismiss: () -> Unit,
    onGenerateCode: suspend () -> String
) {
    var staffCode by remember { mutableStateOf(staff?.staffCode ?: "") }
    var name by remember { mutableStateOf(staff?.name ?: "") }
    var department by remember { mutableStateOf(staff?.department ?: "") }
    var phone by remember { mutableStateOf(staff?.phone ?: "") }
    var notes by remember { mutableStateOf(staff?.notes ?: "") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (staff == null) "添加员工" else "编辑员工") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = staffCode,
                        onValueChange = { staffCode = it },
                        label = { Text("员工编号") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = { scope.launch { staffCode = onGenerateCode() } },
                        modifier = Modifier.padding(top = 4.dp)
                    ) { Text("自动") }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名 *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = department, onValueChange = { department = it }, label = { Text("部门") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("电话") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(Staff(
                        id = staff?.id ?: 0,
                        staffCode = staffCode.trim(),
                        name = name.trim(),
                        department = department.trim(),
                        phone = phone.trim(),
                        notes = notes.trim()
                    ))
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
