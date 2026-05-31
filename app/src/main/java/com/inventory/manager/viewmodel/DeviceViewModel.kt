package com.inventory.manager.viewmodel

import androidx.lifecycle.*
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.entity.*
import com.inventory.manager.data.repository.*
import com.inventory.manager.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DeviceListUiState(
    val devices: List<Device> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Int? = null,
    val selectedStatus: DeviceStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)

class DeviceViewModel(
    private val deviceRepo: DeviceRepository,
    private val categoryRepo: CategoryRepository,
    private val recordRepo: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    private val _selectedStatus = MutableStateFlow<DeviceStatus?>(null)
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            categoryRepo.getAll().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        viewModelScope.launch {
            combine(_selectedCategoryId, _selectedStatus, _searchQuery) { cat, status, query ->
                Triple(cat, status, query)
            }.flatMapLatest { (catId, status, query) ->
                when {
                    query.isNotBlank() -> deviceRepo.search(query)
                    catId != null && status != null -> deviceRepo.getByCategoryAndStatus(catId, status)
                    catId != null -> deviceRepo.getByCategory(catId)
                    status != null -> deviceRepo.getByStatus(status)
                    else -> deviceRepo.getAll()
                }
            }.collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(categoryId: Int?, status: DeviceStatus?) {
        _selectedCategoryId.value = categoryId
        _selectedStatus.value = status
        _uiState.update { it.copy(selectedCategoryId = categoryId, selectedStatus = status) }
    }

    fun insertDevice(device: Device, supplier: String, notes: String) {
        viewModelScope.launch {
            val id = deviceRepo.insert(device)
            recordRepo.insert(
                StockRecord(
                    deviceId = id.toInt(),
                    deviceName = device.displayName,
                    deviceAssetCode = device.assetCode,
                    recordType = RecordType.STOCK_IN,
                    supplier = supplier.takeIf { it.isNotBlank() },
                    notes = notes
                )
            )
            _uiState.update { it.copy(message = "设备入库成功") }
        }
    }

    fun stockOut(device: Device, staff: Staff, notes: String) {
        viewModelScope.launch {
            deviceRepo.update(
                device.copy(
                    status = DeviceStatus.IN_USE,
                    currentStaffId = staff.id,
                    currentStaffName = staff.name
                )
            )
            recordRepo.insert(
                StockRecord(
                    deviceId = device.id,
                    deviceName = device.displayName,
                    deviceAssetCode = device.assetCode,
                    recordType = RecordType.STOCK_OUT,
                    staffId = staff.id,
                    staffName = staff.name,
                    notes = notes
                )
            )
            _uiState.update { it.copy(message = "出库成功，已分配给 ${staff.name}") }
        }
    }

    fun returnDevice(device: Device, condition: DeviceCondition, notes: String) {
        viewModelScope.launch {
            recordRepo.insert(
                StockRecord(
                    deviceId = device.id,
                    deviceName = device.displayName,
                    deviceAssetCode = device.assetCode,
                    recordType = RecordType.RETURN,
                    staffId = device.currentStaffId,
                    staffName = device.currentStaffName,
                    condition = condition,
                    notes = notes
                )
            )
            deviceRepo.update(
                device.copy(
                    status = DeviceStatus.IN_STOCK,
                    currentStaffId = null,
                    currentStaffName = null
                )
            )
            _uiState.update { it.copy(message = "设备归还成功") }
        }
    }

    fun startMaintenance(device: Device, description: String, notes: String) {
        viewModelScope.launch {
            deviceRepo.update(device.copy(status = DeviceStatus.MAINTENANCE))
            recordRepo.insert(
                StockRecord(
                    deviceId = device.id,
                    deviceName = device.displayName,
                    deviceAssetCode = device.assetCode,
                    recordType = RecordType.MAINTENANCE_START,
                    description = description,
                    notes = notes
                )
            )
            _uiState.update { it.copy(message = "设备已标记为维修中") }
        }
    }

    fun endMaintenance(device: Device, result: String, cost: Double?, notes: String) {
        viewModelScope.launch {
            deviceRepo.update(device.copy(status = DeviceStatus.IN_STOCK))
            recordRepo.insert(
                StockRecord(
                    deviceId = device.id,
                    deviceName = device.displayName,
                    deviceAssetCode = device.assetCode,
                    recordType = RecordType.MAINTENANCE_END,
                    cost = cost,
                    description = result,
                    notes = notes
                )
            )
            _uiState.update { it.copy(message = "维修完成，设备已归库") }
        }
    }

    fun scrapDevice(device: Device, notes: String) {
        viewModelScope.launch {
            deviceRepo.update(
                device.copy(
                    status = DeviceStatus.SCRAPPED,
                    currentStaffId = null,
                    currentStaffName = null
                )
            )
            recordRepo.insert(
                StockRecord(
                    deviceId = device.id,
                    deviceName = device.displayName,
                    deviceAssetCode = device.assetCode,
                    recordType = RecordType.SCRAP,
                    notes = notes
                )
            )
            _uiState.update { it.copy(message = "设备已报废") }
        }
    }

    fun updateDevice(device: Device) {
        viewModelScope.launch { deviceRepo.update(device) }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            deviceRepo.delete(device)
            _uiState.update { it.copy(message = "设备已删除") }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    fun getDeviceRecords(deviceId: Int): Flow<List<StockRecord>> = recordRepo.getByDevice(deviceId)

    fun generateAssetCode(categoryName: String, existingCodes: List<String>): String {
        val prefix = when {
            categoryName.contains("主机") -> "PC"
            categoryName.contains("显示") -> "MON"
            categoryName.contains("鼠标") -> "MS"
            categoryName.contains("键盘") -> "KB"
            categoryName.contains("耳机") -> "HP"
            categoryName.contains("摄像") -> "CAM"
            categoryName.contains("打印") -> "PRT"
            else -> "DEV"
        }
        val maxNum = existingCodes.filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).trimStart('-').toIntOrNull() }
            .maxOrNull() ?: 0
        return "$prefix-${String.format("%03d", maxNum + 1)}"
    }

    companion object {
        fun factory(app: InventoryApp) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DeviceViewModel(app.deviceRepository, app.categoryRepository, app.recordRepository) as T
            }
        }
    }
}
