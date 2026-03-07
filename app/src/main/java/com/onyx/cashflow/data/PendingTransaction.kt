package com.onyx.cashflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String = "",
    val senderAddress: String,
    val rawSms: String,
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE
)
