package com.inventory.manager.viewmodel

import androidx.lifecycle.*
import com.inventory.manager.InventoryApp
import com.inventory.manager.data.database.entity.Category
import com.inventory.manager.data.repository.CategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val message: String? = null
)

class CategoryViewModel(private val categoryRepo: CategoryRepository) : ViewModel() {

    val uiState: StateFlow<CategoryUiState> = categoryRepo.getAll()
        .map { CategoryUiState(categories = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryUiState())

    fun insert(category: Category) {
        viewModelScope.launch { categoryRepo.insert(category) }
    }

    fun update(category: Category) {
        viewModelScope.launch { categoryRepo.update(category) }
    }

    fun delete(category: Category) {
        viewModelScope.launch { categoryRepo.delete(category) }
    }

    companion object {
        fun factory(app: InventoryApp) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CategoryViewModel(app.categoryRepository) as T
            }
        }
    }
}
