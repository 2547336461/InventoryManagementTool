package com.inventory.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.entity.*
import com.inventory.manager.utils.DateUtils
import com.inventory.manager.viewmodel.DeviceViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeviceScreen(
    deviceId: Int? = null,
    initialSerialNumber: String? = null,
    onNavigateBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as InventoryApp
    val vm: DeviceViewModel = viewModel(factory = DeviceViewModel.factory(app))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var assetCode by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf(initialSerialNumber ?: "") }

    val serialScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { serialNumber = it }
    }
    var purchaseDateStr by remember { mutableStateOf(DateUtils.formatDate(System.currentTimeMillis())) }
    var warrantyDateStr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    // Derived validation
    val purchaseDateError: String? = if (purchaseDateStr.isNotBlank() && DateUtils.parseDate(purchaseDateStr) == null)
        "格式无效，请使用 yyyy-MM-dd" else null

    val warrantyDateError: String? = if (warrantyDateStr.isNotBlank()) {
        val wd = DateUtils.parseDate(warrantyDateStr)
        when {
            wd == null -> "格式无效，请使用 yyyy-MM-dd"
            else -> {
                val pd = DateUtils.parseDate(purchaseDateStr)
                if (pd != null && wd <= pd) "保修到期应晚于购入日期" else null
            }
        }
    } else null

    val priceError: String? = if (priceStr.isNotBlank()) {
        val v = priceStr.toDoubleOrNull()
        when {
            v == null -> "请输入有效数字"
            v < 0 -> "价格不能为负数"
            else -> null
        }
    } else null

    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) {
            vm.clearNavigateBack()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    LaunchedEffect(deviceId, state.devices) {
        if (!initialized && deviceId != null) {
            val existing = state.devices.find { it.id == deviceId }
                ?: app.deviceRepository.getById(deviceId)
            existing?.let { d ->
                brand = d.brand
                model = d.model
                assetCode = d.assetCode
                serialNumber = d.serialNumber
                purchaseDateStr = DateUtils.formatDate(d.purchaseDate)
                warrantyDateStr = d.warrantyDate?.let { DateUtils.formatDate(it) } ?: ""
                priceStr = if (d.price > 0) d.price.toString() else ""
                notes = d.notes
                selectedCategory = state.categories.find { it.id == d.categoryId }
                initialized = true
            }
        } else if (!initialized && deviceId == null) {
            initialized = true
        }
    }

    LaunchedEffect(state.categories, selectedCategory) {
        if (selectedCategory == null && state.categories.isNotEmpty() && deviceId == null) {
            selectedCategory = state.categories.first()
        }
    }

    val isEditMode = deviceId != null
    val title = if (isEditMode) "编辑设备" else "新设备入库"

    fun validate() = brand.isNotBlank() && model.isNotBlank() && assetCode.isNotBlank() &&
        selectedCategory != null && purchaseDateError == null && warrantyDateError == null && priceError == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = selectedCategory?.let { "${it.icon} ${it.name}" } ?: "选择设备类型",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("设备类型 *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    state.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.icon} ${cat.name}") },
                            onClick = { selectedCategory = cat; categoryExpanded = false }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("品牌 *") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("型号 *") }, modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = assetCode,
                    onValueChange = { assetCode = it },
                    label = { Text("资产编号 *") },
                    modifier = Modifier.weight(1f)
                )
                if (!isEditMode) {
                    OutlinedButton(
                        onClick = {
                            selectedCategory?.let { cat ->
                                scope.launch {
                                    val codes = app.deviceRepository.getAll().first().map { it.assetCode }
                                    assetCode = vm.generateAssetCode(cat.name, codes)
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) { Text("自动") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("序列号") },
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = {
                        serialScanLauncher.launch(ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES)
                            setPrompt("扫描序列号条形码")
                            setBeepEnabled(true)
                            setOrientationLocked(false)
                        })
                    }
                ) {
                    Icon(Icons.Default.QrCodeScanner, "扫描序列号")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = purchaseDateStr,
                    onValueChange = { purchaseDateStr = it },
                    label = { Text("购入日期 *") },
                    placeholder = { Text("yyyy-MM-dd") },
                    isError = purchaseDateError != null,
                    supportingText = purchaseDateError?.let { err -> { Text(err) } },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = warrantyDateStr,
                    onValueChange = { warrantyDateStr = it },
                    label = { Text("保修到期") },
                    placeholder = { Text("yyyy-MM-dd") },
                    isError = warrantyDateError != null,
                    supportingText = warrantyDateError?.let { err -> { Text(err) } },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("购入价格（元）") },
                isError = priceError != null,
                supportingText = priceError?.let { err -> { Text(err) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (!isEditMode) {
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("来源/供应商") }, modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val cat = selectedCategory ?: return@Button
                    val purchaseDate = DateUtils.parseDate(purchaseDateStr) ?: System.currentTimeMillis()
                    val warrantyDate = DateUtils.parseDate(warrantyDateStr)
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    if (isEditMode && deviceId != null) {
                        scope.launch {
                            val existing = app.deviceRepository.getById(deviceId)
                            existing?.let {
                                vm.updateDevice(it.copy(
                                    categoryId = cat.id,
                                    categoryName = cat.name,
                                    brand = brand.trim(),
                                    model = model.trim(),
                                    assetCode = assetCode.trim(),
                                    serialNumber = serialNumber.trim(),
                                    purchaseDate = purchaseDate,
                                    warrantyDate = warrantyDate,
                                    price = price,
                                    notes = notes.trim()
                                ))
                            }
                        }
                    } else {
                        vm.insertDevice(
                            Device(
                                categoryId = cat.id,
                                categoryName = cat.name,
                                brand = brand.trim(),
                                model = model.trim(),
                                assetCode = assetCode.trim(),
                                serialNumber = serialNumber.trim(),
                                purchaseDate = purchaseDate,
                                warrantyDate = warrantyDate,
                                price = price,
                                notes = notes.trim()
                            ),
                            supplier = supplier.trim(),
                            notes = ""
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = validate()
            ) {
                Text(if (isEditMode) "保存修改" else "确认入库", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
