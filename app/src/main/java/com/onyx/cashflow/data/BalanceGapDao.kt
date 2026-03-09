package com.onyx.cashflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceGapDao {

    @Insert
    suspend fun insert(gap: BalanceGap): Long

    @Query("SELECT * FROM balance_gaps WHERE resolved = 0 ORDER BY detectedAt DESC")
    fun getUnresolved(): Flow<List<BalanceGap>>

    @Query("SELECT COUNT(*) FROM balance_gaps WHERE resolved = 0")
    fun getUnresolvedCount(): Flow<Int>

    @Query("UPDATE balance_gaps SET resolved = 1 WHERE id = :id")
    suspend fun markResolved(id: Long)
}
