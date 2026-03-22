package com.onyx.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.Category
import com.onyx.cashflow.data.CategoryTotal
import com.onyx.cashflow.data.MerchantCategoryRule
import com.onyx.cashflow.data.MerchantNormalizer
import com.onyx.cashflow.data.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val merchantRuleDao = db.merchantCategoryRuleDao()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())
    val selectedMonth: StateFlow<Calendar> = _selectedMonth.asStateFlow()

    // Transaction being edited
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    val editingTransaction: StateFlow<Transaction?> = _editingTransaction.asStateFlow()

    // Snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val monthStart: StateFlow<Long> = _selectedMonth.map { cal ->
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), getStartOfMonth())

    val monthEnd: StateFlow<Long> = _selectedMonth.map { cal ->
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        c.timeInMillis
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), getEndOfMonth())

    val categoryTotals: StateFlow<List<CategoryTotal>> = combine(monthStart, monthEnd) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        transactionDao.getTotalByCategory(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenses: StateFlow<Double> = combine(monthStart, monthEnd) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        transactionDao.getTotalExpenses(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = combine(monthStart, monthEnd) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        transactionDao.getTotalIncome(start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentTransactions: StateFlow<List<Transaction>> = transactionDao.getRecent(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousMonth() {
        _selectedMonth.update { cal ->
            (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        }
    }

    fun nextMonth() {
        _selectedMonth.update { cal ->
            (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        }
    }

    fun startEditCategory(transaction: Transaction) {
        _editingTransaction.value = transaction
    }

    fun dismissEditCategory() {
        _editingTransaction.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    /**
     * Updates category for the current transaction.
     * @param applyToAll if true, saves a merchant rule + backfills all matching transactions
     */
    fun updateTransactionCategory(newCategoryId: Long, applyToAll: Boolean) {
        val transaction = _editingTransaction.value ?: return
        val merchant = transaction.note.trim()

        viewModelScope.launch {
            // Always update this specific transaction
            transactionDao.update(transaction.copy(categoryId = newCategoryId))

            if (applyToAll && merchant.isNotEmpty()) {
                val normalizedKey = MerchantNormalizer.normalizeKey(merchant)
                val displayName = MerchantNormalizer.displayName(merchant)

                // Save the merchant → category rule
                merchantRuleDao.insertRule(
                    MerchantCategoryRule(normalizedKey, displayName, newCategoryId)
                )

                // Backfill all past transactions with matching normalized key
                val allTransactions = transactionDao.getAllTransactions()
                var updatedCount = 0
                for (tx in allTransactions) {
                    if (tx.id == transaction.id) continue
                    val txKey = MerchantNormalizer.normalizeKey(tx.note.trim())
                    if (txKey == normalizedKey && tx.categoryId != newCategoryId) {
                        transactionDao.update(tx.copy(categoryId = newCategoryId))
                        updatedCount++
                    }
                }

                _snackbarMessage.value = "Category updated for all \"$displayName\" transactions ($updatedCount updated)"
            }

            _editingTransaction.value = null
        }
    }

    // Stores last deleted transaction for undo
    private var lastDeletedTransaction: Transaction? = null

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.delete(transaction)
            lastDeletedTransaction = transaction
            _snackbarMessage.value = "UNDO_DELETE"
        }
    }

    fun undoDelete() {
        val tx = lastDeletedTransaction ?: return
        viewModelScope.launch {
            transactionDao.insert(tx)
            lastDeletedTransaction = null
        }
    }

    private fun getStartOfMonth(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getEndOfMonth(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }
}
