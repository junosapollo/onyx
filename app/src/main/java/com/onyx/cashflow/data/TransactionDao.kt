package com.onyx.cashflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val categoryId: Long?,
    val categoryName: String?,
    val categoryColor: Long?,
    val total: Double
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("""
        SELECT * FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("""
        SELECT t.categoryId, c.name AS categoryName, c.color AS categoryColor, SUM(t.amount) AS total
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.date BETWEEN :startDate AND :endDate AND t.type = 'EXPENSE'
        GROUP BY t.categoryId
        ORDER BY total DESC
    """)
    fun getTotalByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<Transaction>>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate
    """)
    fun getTotalExpenses(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = 'INCOME' AND date BETWEEN :startDate AND :endDate
    """)
    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double?>
}
