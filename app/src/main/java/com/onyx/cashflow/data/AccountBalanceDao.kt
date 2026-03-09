package com.onyx.cashflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountBalanceDao {

    @Query("SELECT * FROM account_balances WHERE accountId = :accountId")
    suspend fun getByAccountId(accountId: String): AccountBalance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(accountBalance: AccountBalance)

    @Query("SELECT * FROM account_balances ORDER BY lastTransactionDate DESC")
    fun getAll(): Flow<List<AccountBalance>>
}
