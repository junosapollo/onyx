package com.onyx.cashflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_category_rules")
data class MerchantCategoryRule(
    @PrimaryKey
    val normalizedKey: String,
    val merchant: String,
    val categoryId: Long
)
