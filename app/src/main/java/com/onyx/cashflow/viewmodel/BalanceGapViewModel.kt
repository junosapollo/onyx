package com.onyx.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.BalanceGap
import com.onyx.cashflow.data.Transaction
import com.onyx.cashflow.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BalanceGapViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val gapDao = db.balanceGapDao()
    private val transactionDao = db.transactionDao()

    val unresolvedGaps: StateFlow<List<BalanceGap>> = gapDao.getUnresolved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unresolvedCount: StateFlow<Int> = gapDao.getUnresolvedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Dismiss a gap without adding a transaction (user knows what happened,
     * or it's a false positive).
     */
    fun dismissGap(gapId: Long) {
        viewModelScope.launch {
            gapDao.markResolved(gapId)
        }
    }

    /**
     * Resolve a gap by creating a transaction for the missing amount
     * and marking the gap as resolved.
     */
    fun resolveWithTransaction(gap: BalanceGap, categoryId: Long?, note: String) {
        viewModelScope.launch {
            transactionDao.insert(
                Transaction(
                    amount = gap.gapAmount,
                    categoryId = categoryId,
                    note = note.ifBlank { "Recovered from balance gap" },
                    date = gap.detectedAt,
                    type = gap.gapType
                )
            )
            gapDao.markResolved(gap.id)
        }
    }
}
