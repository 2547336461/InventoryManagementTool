package com.inventory.manager.viewmodel

import androidx.lifecycle.*
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.entity.Category
import com.inventory.manager.data.repository.CategoryRepository
import com.inventory.manager.data.repository.DeviceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val message: String? = null
)

class CategoryViewModel(
    private val categoryRepo: CategoryRepository,
    private val deviceRepo: DeviceRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryRepo.getAll(),
        _message
    ) { cats, msg -> CategoryUiState(categories = cats, message = msg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryUiState())

    fun insert(category: Category) {
        viewModelScope.launch { categoryRepo.insert(category) }
    }

    fun update(category: Category) {
        viewModelScope.launch { categoryRepo.update(category) }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            val count = deviceRepo.countByCategorySync(category.id)
            if (count > 0) {
                _message.value = "「${category.name}」下还有 $count 台设备，无法删除"
            } else {
                categoryRepo.delete(category)
            }
        }
    }

    fun clearMessage() { _message.value = null }

    companion object {
        fun factory(app: InventoryApp) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CategoryViewModel(app.categoryRepository, app.deviceRepository) as T
            }
        }
    }
}
