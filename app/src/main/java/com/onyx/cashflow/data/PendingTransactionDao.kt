package com.onyx.cashflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pending: PendingTransaction): Long

    @Delete
    suspend fun delete(pending: PendingTransaction)

    @Query("SELECT * FROM pending_transactions ORDER BY date DESC")
    fun getAll(): Flow<List<PendingTransaction>>

    @Query("SELECT COUNT(*) FROM pending_transactions")
    fun getCount(): Flow<Int>
}
