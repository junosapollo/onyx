package com.onyx.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.Transaction
import com.onyx.cashflow.data.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TransactionFormState(
    val amount: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

class TransactionViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()

    val categories = categoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    fun updateAmount(value: String) {
        _formState.update { it.copy(amount = value, error = null) }
    }

    fun updateCategory(id: Long) {
        _formState.update { it.copy(categoryId = id) }
    }

    fun updateNote(value: String) {
        _formState.update { it.copy(note = value) }
    }

    fun updateDate(millis: Long) {
        _formState.update { it.copy(date = millis) }
    }

    fun updateType(type: TransactionType) {
        _formState.update { it.copy(type = type) }
    }

    fun save() {
        val state = _formState.value
        val amount = state.amount.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _formState.update { it.copy(error = "Enter a valid amount") }
            return
        }
        if (state.categoryId == null) {
            _formState.update { it.copy(error = "Select a category") }
            return
        }

        _formState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            transactionDao.insert(
                Transaction(
                    amount = amount,
                    categoryId = state.categoryId,
                    note = state.note.trim(),
                    date = state.date,
                    type = state.type
                )
            )
            _formState.update { it.copy(isSaving = false, saved = true) }
        }
    }

    fun resetForm() {
        _formState.value = TransactionFormState()
    }
}
