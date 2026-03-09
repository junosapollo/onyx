package com.onyx.cashflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the last-known balance for each bank account, identified by its last 4 digits.
 * Updated every time we parse an SMS that includes balance information.
 */
@Entity(tableName = "account_balances")
data class AccountBalance(
    @PrimaryKey
    val accountId: String,           // Last 4 digits of account number
    val lastBalance: Double,         // Balance reported in the most recent SMS
    val lastTransactionDate: Long,   // Timestamp of the SMS that reported this balance
    val bankSender: String = ""      // Sender ID for context (e.g., "BOBSMS")
)
