package com.inventory.manager.viewmodel

import androidx.lifecycle.*
import com.inventory.manager.data.database.entity.Device
import com.inventory.manager.data.database.entity.DeviceStatus
import com.inventory.manager.data.database.entity.StockRecord
import com.inventory.manager.data.repository.DeviceRepository
import com.inventory.manager.data.repository.RecordRepository
import com.inventory.manager.utils.DateUtils
import kotlinx.coroutines.flow.*

data class DashboardUiState(
    val totalCount: Int = 0,
    val inStockCount: Int = 0,
    val inUseCount: Int = 0,
    val maintenanceCount: Int = 0,
    val scrappedCount: Int = 0,
    val recentRecords: List<StockRecord> = emptyList(),
    val warningDevices: List<Device> = emptyList()
)

class DashboardViewModel(
    private val deviceRepo: DeviceRepository,
    private val recordRepo: RecordRepository
) : ViewModel() {

    private val countsFlow = combine(
        deviceRepo.countAll(),
        deviceRepo.countByStatus(DeviceStatus.IN_STOCK),
        deviceRepo.countByStatus(DeviceStatus.IN_USE),
        deviceRepo.countByStatus(DeviceStatus.MAINTENANCE),
        deviceRepo.countByStatus(DeviceStatus.SCRAPPED)
    ) { total, inStock, inUse, maintenance, scrapped ->
        listOf(total, inStock, inUse, maintenance, scrapped)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        countsFlow,
        recordRepo.getRecent(8),
        deviceRepo.getWarrantyExpiring(DateUtils.daysFromNow(30))
    ) { counts, recent, warning ->
        DashboardUiState(
            totalCount = counts[0],
            inStockCount = counts[1],
            inUseCount = counts[2],
            maintenanceCount = counts[3],
            scrappedCount = counts[4],
            recentRecords = recent,
            warningDevices = warning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    companion object {
        fun factory(app: com.inventory.manager.InventoryApp) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(app.deviceRepository, app.recordRepository) as T
            }
        }
    }
}
