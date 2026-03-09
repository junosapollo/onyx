package com.onyx.cashflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Transaction::class,
        Category::class,
        TrustedSender::class,
        PendingTransaction::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun trustedSenderDao(): TrustedSenderDao
    abstract fun pendingTransactionDao(): PendingTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS trusted_senders (
                        address TEXT NOT NULL PRIMARY KEY,
                        label TEXT NOT NULL DEFAULT '',
                        approvedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        merchant TEXT NOT NULL DEFAULT '',
                        senderAddress TEXT NOT NULL,
                        rawSms TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        type TEXT NOT NULL DEFAULT 'EXPENSE'
                    )
                """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cashflow.db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .addCallback(SeedCallback())
                .build()
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.categoryDao()
                    val defaults = listOf(
                        Category(name = "Food", icon = "restaurant", color = 0xFFFF7043),
                        Category(name = "Travel", icon = "directions_car", color = 0xFF42A5F5),
                        Category(name = "Bills", icon = "receipt_long", color = 0xFFEF5350),
                        Category(name = "Shopping", icon = "shopping_bag", color = 0xFFAB47BC),
                        Category(name = "Entertainment", icon = "movie", color = 0xFFFFCA28),
                        Category(name = "Health", icon = "favorite", color = 0xFF66BB6A),
                        Category(name = "Other", icon = "more_horiz", color = 0xFF78909C),
                    )
                    defaults.forEach { dao.insert(it) }
                }
            }
        }
    }
}
