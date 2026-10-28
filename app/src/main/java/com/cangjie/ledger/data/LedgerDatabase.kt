package com.cangjie.ledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        CategoryEntity::class,
        RecordEntity::class,
        TagEntity::class,
        RecordTagCrossRef::class,
        BudgetEntity::class,
        RecurringRuleEntity::class,
        RecurringOccurrenceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recordDao(): RecordDao
    abstract fun tagDao(): TagDao
    abstract fun recordTagDao(): RecordTagDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun recurringOccurrenceDao(): RecurringOccurrenceDao

    companion object {
        @Volatile
        private var instance: LedgerDatabase? = null

        fun get(context: Context): LedgerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "cangjie-ledger.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}

