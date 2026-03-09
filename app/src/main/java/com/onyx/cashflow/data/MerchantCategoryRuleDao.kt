package com.onyx.cashflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MerchantCategoryRuleDao {
    
    @Query("SELECT * FROM merchant_category_rules")
    suspend fun getAllRules(): List<MerchantCategoryRule>

    @Query("SELECT * FROM merchant_category_rules WHERE merchant = :merchant LIMIT 1")
    suspend fun getRuleForMerchant(merchant: String): MerchantCategoryRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MerchantCategoryRule)

    @Query("DELETE FROM merchant_category_rules WHERE merchant = :merchant")
    suspend fun deleteRule(merchant: String)
}
