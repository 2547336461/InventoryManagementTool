package com.inventory.manager.viewmodel

import androidx.lifecycle.*
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.entity.Staff
import com.inventory.manager.data.repository.DeviceRepository
import com.inventory.manager.data.repository.StaffRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StaffUiState(
    val staffList: List<Staff> = emptyList(),
    val searchQuery: String = "",
    val message: String? = null
)

class StaffViewModel(
    private val staffRepo: StaffRepository,
    private val deviceRepo: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery.flatMapLatest { query ->
                if (query.isBlank()) staffRepo.getActiveStaff() else staffRepo.searchStaff(query)
            }.collect { list ->
                _uiState.update { it.copy(staffList = list) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun insertStaff(staff: Staff) {
        viewModelScope.launch {
            staffRepo.insert(staff)
            _uiState.update { it.copy(message = "员工 ${staff.name} 已添加") }
        }
    }

    fun updateStaff(staff: Staff) {
        viewModelScope.launch {
            staffRepo.update(staff)
            _uiState.update { it.copy(message = "员工信息已更新") }
        }
    }

    fun deleteStaff(staff: Staff) {
        viewModelScope.launch {
            val activeDevices = deviceRepo.getInUseByStaff(staff.id)
            if (activeDevices.isNotEmpty()) {
                _uiState.update { it.copy(message = "${staff.name} 还持有 ${activeDevices.size} 台设备，请先完成归还再移除") }
                return@launch
            }
            staffRepo.softDelete(staff.id)
            _uiState.update { it.copy(message = "${staff.name} 已移除") }
        }
    }

    fun getStaffDevices(staffId: Int) = deviceRepo.getByStaff(staffId)

    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    companion object {
        fun factory(app: InventoryApp) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return StaffViewModel(app.staffRepository, app.deviceRepository) as T
            }
        }
    }
}
