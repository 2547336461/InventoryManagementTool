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
import com.inventory.manager.data.database.entity.Category
import com.inventory.manager.ui.components.ConfirmDialog
import com.inventory.manager.ui.components.EmptyState
import com.inventory.manager.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(onNavigateBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as InventoryApp
    val vm: CategoryViewModel = viewModel(factory = CategoryViewModel.factory(app))
    val state by vm.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设备类型管理", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "添加类型") } },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.categories.isEmpty()) {
            EmptyState("暂无类型", Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                items(state.categories, key = { it.id }) { cat ->
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.icon, fontSize = 24.sp)
                            Spacer(Modifier.width(16.dp))
                            Text(cat.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { editingCategory = cat }) { Icon(Icons.Default.Edit, "编辑") }
                            IconButton(onClick = { deletingCategory = cat }) {
                                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        CategoryEditDialog(category = null, onSave = { vm.insert(it); showAddDialog = false }, onDismiss = { showAddDialog = false })
    }
    editingCategory?.let { cat ->
        CategoryEditDialog(category = cat, onSave = { vm.update(it); editingCategory = null }, onDismiss = { editingCategory = null })
    }
    deletingCategory?.let { cat ->
        ConfirmDialog(
            title = "删除类型",
            text = "确认删除「${cat.name}」类型？",
            onConfirm = { vm.delete(cat); deletingCategory = null },
            onDismiss = { deletingCategory = null }
        )
    }
}

@Composable
private fun CategoryEditDialog(category: Category?, onSave: (Category) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "📦") }
    val iconOptions = listOf("🖥️", "🖥", "🖱️", "⌨️", "🎧", "📷", "🖨️", "🖊️", "📱", "💾", "📦", "🔌")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "添加设备类型" else "编辑设备类型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("类型名称 *") }, modifier = Modifier.fillMaxWidth())
                Text("选择图标:", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    iconOptions.take(6).forEach { opt ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (icon == opt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { icon = opt }
                        ) { Text(opt, fontSize = 20.sp, modifier = Modifier.padding(8.dp)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    iconOptions.drop(6).forEach { opt ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (icon == opt) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { icon = opt }
                        ) { Text(opt, fontSize = 20.sp, modifier = Modifier.padding(8.dp)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(Category(id = category?.id ?: 0, name = name.trim(), icon = icon)) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
