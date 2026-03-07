package com.onyx.cashflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedSenderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sender: TrustedSender)

    @Delete
    suspend fun delete(sender: TrustedSender)

    @Query("SELECT * FROM trusted_senders ORDER BY label ASC")
    fun getAll(): Flow<List<TrustedSender>>

    @Query("SELECT EXISTS(SELECT 1 FROM trusted_senders WHERE address = :address)")
    suspend fun isTrusted(address: String): Boolean
}
