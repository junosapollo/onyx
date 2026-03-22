package com.onyx.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onyx.cashflow.data.AppDatabase
import com.onyx.cashflow.data.PendingTransaction
import com.onyx.cashflow.data.Transaction
import com.onyx.cashflow.data.TrustedSender
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PendingViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val pendingDao = db.pendingTransactionDao()
    private val trustedSenderDao = db.trustedSenderDao()
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()

    val pendingTransactions = pendingDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount = pendingDao.getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trustedSenders = trustedSenderDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveSender(pending: PendingTransaction) {
        viewModelScope.launch {
            // 1. Trust the sender
            trustedSenderDao.insert(
                TrustedSender(
                    address = pending.senderAddress,
                    label = pending.senderAddress
                )
            )

            // 2. Find "Bills" category for default assignment
            val otherCategory = categoryDao.getAll().first().find {
                it.name.equals("Bills", ignoreCase = true)
            }

            // 3. Save as confirmed transaction
            transactionDao.insert(
                Transaction(
                    amount = pending.amount,
                    categoryId = otherCategory?.id,
                    note = pending.merchant,
                    date = pending.date,
                    type = pending.type
                )
            )

            // 4. Remove from pending
            pendingDao.delete(pending)
        }
    }

    fun dismiss(pending: PendingTransaction) {
        viewModelScope.launch {
            pendingDao.delete(pending)
        }
    }

    fun removeTrustedSender(sender: TrustedSender) {
        viewModelScope.launch {
            trustedSenderDao.delete(sender)
        }
    }
}
