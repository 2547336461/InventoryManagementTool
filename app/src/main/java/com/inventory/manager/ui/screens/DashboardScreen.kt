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
import com.inventory.manager.data.database.entity.RecordType
import com.inventory.manager.ui.components.SectionHeader
import com.inventory.manager.ui.components.StatCard
import com.inventory.manager.ui.theme.*
import com.inventory.manager.utils.DateUtils
import com.inventory.manager.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToDevices: () -> Unit,
    onNavigateToAddDevice: () -> Unit
) {
    val app = LocalContext.current.applicationContext as InventoryApp
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(app))
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("库存管理", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToAddDevice) {
                        Icon(Icons.Default.Add, "入库", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("全部设备", state.totalCount, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatCard("在库", state.inStockCount, InStockColor, Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("使用中", state.inUseCount, InUseColor, Modifier.weight(1f))
                    StatCard("维修中", state.maintenanceCount, MaintenanceColor, Modifier.weight(1f))
                    StatCard("已报废", state.scrappedCount, ScrappedColor, Modifier.weight(1f))
                }
            }

            if (state.warningDevices.isNotEmpty()) {
                item { SectionHeader("⚠️ 保修即将到期（30天内）") }
                items(state.warningDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = WarningColor.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(device.displayName, fontWeight = FontWeight.Medium)
                                Text(device.assetCode, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }
                            Text(
                                "到期: ${DateUtils.formatDate(device.warrantyDate)}",
                                color = WarningColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item { SectionHeader("最近操作") }
            if (state.recentRecords.isEmpty()) {
                item {
                    Text(
                        "暂无操作记录",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(state.recentRecords) { record ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(record.recordType.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${record.recordType.label} · ${record.deviceName}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    buildString {
                                        append(record.deviceAssetCode)
                                        if (record.staffName != null) append(" · ${record.staffName}")
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                )
                            }
                            Text(
                                DateUtils.formatDate(record.operationTime),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
