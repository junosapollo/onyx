package com.onyx.cashflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_senders")
data class TrustedSender(
    @PrimaryKey
    val address: String,
    val label: String = "",
    val approvedAt: Long = System.currentTimeMillis()
)
