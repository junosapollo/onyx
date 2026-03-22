package com.onyx.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val categoryDao = db.categoryDao()

    val categories = categoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _editingCategory = MutableStateFlow<Category?>(null)
    val editingCategory: StateFlow<Category?> = _editingCategory.asStateFlow()

    fun showAddDialog() {
        _editingCategory.value = null
        _showDialog.value = true
    }

    fun showEditDialog(category: Category) {
        _editingCategory.value = category
        _showDialog.value = true
    }

    fun dismissDialog() {
        _showDialog.value = false
        _editingCategory.value = null
    }

    fun saveCategory(name: String, color: Long) {
        viewModelScope.launch {
            val existing = _editingCategory.value
            if (existing != null) {
                categoryDao.update(existing.copy(name = name, color = color))
            } else {
                categoryDao.insert(Category(name = name, color = color))
            }
            dismissDialog()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.delete(category)
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            val defaults = listOf(
                Category(name = "Bills", icon = "receipt_long", color = 0xFFEF5350),
                Category(name = "Entertainment", icon = "movie", color = 0xFFFFCA28),
                Category(name = "Food", icon = "restaurant", color = 0xFFFF7043),
                Category(name = "Health", icon = "favorite", color = 0xFF66BB6A),
                Category(name = "Shopping", icon = "shopping_bag", color = 0xFFAB47BC),
                Category(name = "Travel", icon = "directions_car", color = 0xFF42A5F5),
                Category(name = "Academics", icon = "school", color = 0xFF26A69A),
                Category(name = "Friends", icon = "group", color = 0xFF5C6BC0)
            )
            defaults.forEach { categoryDao.insert(it) }
        }
    }
}
