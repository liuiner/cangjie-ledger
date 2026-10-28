package com.cangjie.ledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY archived, createdAt")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE archived = 0 ORDER BY createdAt")
    suspend fun getActive(): List<BookEntity>

    @Query("SELECT * FROM books ORDER BY archived, createdAt")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("DELETE FROM books")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type DESC, sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type DESC, sortOrder, id")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE bookId = :bookId ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeByBook(bookId: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: Long): RecordEntity?

    @Insert
    suspend fun insert(record: RecordEntity): Long

    @Update
    suspend fun update(record: RecordEntity)

    @Delete
    suspend fun delete(record: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM records")
    suspend fun clear()

    @Query("SELECT * FROM records ORDER BY dateEpochDay DESC, createdAt DESC")
    suspend fun getAllSnapshot(): List<RecordEntity>
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getByNormalizedName(normalizedName: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id NOT IN (SELECT tagId FROM record_tags)")
    suspend fun deleteUnused()

    @Query("DELETE FROM tags")
    suspend fun clear()
}

@Dao
interface RecordTagDao {
    @Query("SELECT * FROM record_tags")
    fun observeAll(): Flow<List<RecordTagCrossRef>>

    @Query("SELECT * FROM record_tags")
    suspend fun getAll(): List<RecordTagCrossRef>

    @Query("DELETE FROM record_tags WHERE recordId = :recordId")
    suspend fun deleteForRecord(recordId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<RecordTagCrossRef>)

    @Query("DELETE FROM record_tags")
    suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY monthKey DESC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE bookId = :bookId AND categoryId = :categoryId AND monthKey = :monthKey LIMIT 1")
    suspend fun get(bookId: Long, categoryId: Long, monthKey: String): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun clear()
}

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules ORDER BY enabled DESC, id DESC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules ORDER BY enabled DESC, id DESC")
    suspend fun getAll(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getById(id: Long): RecurringRuleEntity?

    @Insert
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recurring_rules")
    suspend fun clear()
}

@Dao
interface RecurringOccurrenceDao {
    @Query("SELECT * FROM recurring_occurrences WHERE ruleId = :ruleId AND periodKey = :periodKey LIMIT 1")
    suspend fun find(ruleId: Long, periodKey: String): RecurringOccurrenceEntity?

    @Query("SELECT * FROM recurring_occurrences ORDER BY generatedAt DESC")
    suspend fun getAll(): List<RecurringOccurrenceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(occurrence: RecurringOccurrenceEntity): Long

    @Query("DELETE FROM recurring_occurrences")
    suspend fun clear()
}

