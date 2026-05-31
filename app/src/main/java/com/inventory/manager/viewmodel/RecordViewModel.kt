package com.inventory.manager.viewmodel

import androidx.lifecycle.*
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.entity.RecordType
import com.inventory.manager.data.database.entity.StockRecord
import com.inventory.manager.data.repository.RecordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RecordUiState(
    val records: List<StockRecord> = emptyList(),
    val selectedType: RecordType? = null,
    val searchQuery: String = ""
)

class RecordViewModel(private val recordRepo: RecordRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    private val _selectedType = MutableStateFlow<RecordType?>(null)

    init {
        viewModelScope.launch {
            _selectedType.flatMapLatest { type ->
                if (type == null) recordRepo.getAll() else recordRepo.getByType(type)
            }.collect { list ->
                _uiState.update { it.copy(records = list) }
            }
        }
    }

    fun setTypeFilter(type: RecordType?) {
        _selectedType.value = type
        _uiState.update { it.copy(selectedType = type) }
    }

    companion object {
        fun factory(app: InventoryApp) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return RecordViewModel(app.recordRepository) as T
            }
        }
    }
}
