package com.onyx.cashflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records a detected gap between expected and actual balance.
 * Created when the balance reported in an SMS doesn't match
 * what we expected based on the previous balance and the transaction amount.
 */
@Entity(tableName = "balance_gaps")
data class BalanceGap(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: String,           // Last 4 digits of account number
    val expectedBalance: Double,     // What we calculated: previous ± transaction
    val actualBalance: Double,       // What the SMS reported
    val gapAmount: Double,           // |expected - actual| — the missing transaction amount
    val gapType: TransactionType,    // EXPENSE if money is missing, INCOME if extra money
    val detectedAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false
)
