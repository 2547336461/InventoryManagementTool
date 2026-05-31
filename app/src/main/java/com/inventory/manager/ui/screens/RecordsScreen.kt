package com.inventory.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.inventory.manager.data.database.entity.StockRecord
import com.inventory.manager.ui.components.EmptyState
import com.inventory.manager.utils.DateUtils
import com.inventory.manager.viewmodel.RecordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen() {
    val app = LocalContext.current.applicationContext as InventoryApp
    val vm: RecordViewModel = viewModel(factory = RecordViewModel.factory(app))
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("操作记录", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedType == null,
                        onClick = { vm.setTypeFilter(null) },
                        label = { Text("全部") }
                    )
                }
                items(RecordType.entries) { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { vm.setTypeFilter(if (state.selectedType == type) null else type) },
                        label = { Text("${type.icon} ${type.label}") }
                    )
                }
            }

            if (state.records.isEmpty()) {
                EmptyState("暂无记录", Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.records, key = { it.id }) { record ->
                        RecordCard(record)
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: StockRecord) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(record.recordType.icon, fontSize = 22.sp, modifier = Modifier.padding(top = 2.dp, end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(record.recordType.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(DateUtils.formatDateTime(record.operationTime), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${record.deviceName} · ${record.deviceAssetCode}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.75f)
                )
                if (record.staffName != null) {
                    Text("人员: ${record.staffName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
                if (record.supplier != null) {
                    Text("来源: ${record.supplier}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
                if (record.condition != null) {
                    Text("状况: ${record.condition.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
                if (record.description.isNotBlank()) {
                    Text(record.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
                if (record.cost != null) {
                    Text("费用: ¥${String.format("%.2f", record.cost)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                if (record.notes.isNotBlank()) {
                    Text("备注: ${record.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
        }
    }
}
