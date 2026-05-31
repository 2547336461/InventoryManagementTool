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
import com.inventory.manager.data.database.entity.Category
import com.inventory.manager.data.database.entity.Device
import com.inventory.manager.data.database.entity.DeviceStatus
import com.inventory.manager.ui.components.EmptyState
import com.inventory.manager.ui.components.StatusBadge
import com.inventory.manager.utils.DateUtils
import com.inventory.manager.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val app = LocalContext.current.applicationContext as InventoryApp
    val vm: DeviceViewModel = viewModel(factory = DeviceViewModel.factory(app))
    val state by vm.uiState.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("设备列表", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, "筛选", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                )
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { vm.setSearchQuery(it) },
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("搜索品牌、型号、资产编号...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                ) {}
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, "添加设备")
            }
        }
    ) { padding ->
        if (state.devices.isEmpty()) {
            EmptyState("暂无设备，点击右下角 + 添加", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(state.devices, key = { it.id }) { device ->
                    DeviceCard(device = device, onClick = { onNavigateToDetail(device.id) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            selectedStatus = state.selectedStatus,
            onApply = { catId, status ->
                vm.setFilter(catId, status)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun DeviceCard(device: Device, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        device.categoryName.let {
                            when {
                                it.contains("主机") -> "🖥️"
                                it.contains("显示") -> "🖥"
                                it.contains("鼠标") -> "🖱️"
                                it.contains("键盘") -> "⌨️"
                                it.contains("耳机") -> "🎧"
                                it.contains("摄像") -> "📷"
                                it.contains("打印") -> "🖨️"
                                else -> "📦"
                            }
                        },
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(device.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "资产编号: ${device.assetCode}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                if (device.currentStaffName != null) {
                    Text(
                        "使用人: ${device.currentStaffName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
                if (device.warrantyDate != null) {
                    val daysLeft = DateUtils.daysUntil(device.warrantyDate)
                    val color = when {
                        daysLeft < 0 -> MaterialTheme.colorScheme.error
                        daysLeft <= 30 -> com.inventory.manager.ui.theme.WarningColor
                        else -> MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    }
                    Text(
                        "保修至: ${DateUtils.formatDate(device.warrantyDate)}",
                        fontSize = 11.sp,
                        color = color
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(device.status)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    categories: List<Category>,
    selectedCategoryId: Int?,
    selectedStatus: DeviceStatus?,
    onApply: (Int?, DeviceStatus?) -> Unit,
    onDismiss: () -> Unit
) {
    var localCatId by remember { mutableStateOf(selectedCategoryId) }
    var localStatus by remember { mutableStateOf(selectedStatus) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("筛选设备", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Text("设备类型", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = localCatId == null,
                        onClick = { localCatId = null },
                        label = { Text("全部") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = localCatId == cat.id,
                        onClick = { localCatId = if (localCatId == cat.id) null else cat.id },
                        label = { Text("${cat.icon} ${cat.name}") }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("设备状态", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = localStatus == null,
                        onClick = { localStatus = null },
                        label = { Text("全部") }
                    )
                }
                items(DeviceStatus.entries) { status ->
                    FilterChip(
                        selected = localStatus == status,
                        onClick = { localStatus = if (localStatus == status) null else status },
                        label = { Text(status.label) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onApply(null, null) },
                    modifier = Modifier.weight(1f)
                ) { Text("重置") }
                Button(
                    onClick = { onApply(localCatId, localStatus) },
                    modifier = Modifier.weight(1f)
                ) { Text("应用") }
            }
        }
    }
}
