package com.cangjie.ledger.data

import android.content.Context
import androidx.room.withTransaction
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.widget.WidgetUpdater
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LedgerRepository(
    private val context: Context,
    private val database: LedgerDatabase,
    val preferences: PreferencesRepository,
) {
    private val bookDao = database.bookDao()
    private val categoryDao = database.categoryDao()
    private val recordDao = database.recordDao()
    private val tagDao = database.tagDao()
    private val recordTagDao = database.recordTagDao()
    private val budgetDao = database.budgetDao()
    private val ruleDao = database.recurringRuleDao()
    private val occurrenceDao = database.recurringOccurrenceDao()

    val books: Flow<List<BookEntity>> = bookDao.observeAll()
    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val records: Flow<List<RecordEntity>> = recordDao.observeAll()
    val tags: Flow<List<TagEntity>> = tagDao.observeAll()
    val recordTags: Flow<List<RecordTagCrossRef>> = recordTagDao.observeAll()
    val budgets: Flow<List<BudgetEntity>> = budgetDao.observeAll()
    val recurringRules: Flow<List<RecurringRuleEntity>> = ruleDao.observeAll()
    val settings: Flow<AppSettings> = preferences.settings

    suspend fun initialize() {
        database.withTransaction {
            if (bookDao.count() == 0) {
                bookDao.insert(BookEntity(name = "日常"))
            }
            if (categoryDao.count() == 0) {
                categoryDao.insertAll(
                    SeedData.categories.map { seed ->
                        CategoryEntity(
                            name = seed.name,
                            type = seed.type,
                            emoji = seed.emoji,
                            color = seed.color,
                            sortOrder = seed.sortOrder,
                        )
                    },
                )
            }
        }
    }

    suspend fun ensureValidDefaultBook(configuredId: Long): Long {
        val active = bookDao.getActive()
        if (active.isEmpty()) {
            val id = bookDao.insert(BookEntity(name = "日常"))
            preferences.setDefaultBookId(id)
            return id
        }
        val valid = active.firstOrNull { it.id == configuredId } ?: active.first()
        if (valid.id != configuredId) preferences.setDefaultBookId(valid.id)
        return valid.id
    }

    suspend fun saveBook(book: BookEntity): BookEntity {
        val cleanName = book.name.trim()
        require(cleanName.isNotEmpty()) { "账本名称不能为空" }
        return if (book.id == 0L) {
            val id = bookDao.insert(book.copy(name = cleanName))
            book.copy(id = id, name = cleanName)
        } else {
            bookDao.update(book.copy(name = cleanName))
            book.copy(name = cleanName)
        }
    }

    suspend fun archiveBook(bookId: Long, archived: Boolean) {
        val book = bookDao.getById(bookId) ?: return
        val active = bookDao.getActive()
        require(!archived || active.size > 1 || book.archived) { "至少需要保留一个可用账本" }
        bookDao.update(book.copy(archived = archived))
        val settings = settings.first()
        if (archived && settings.defaultBookId == bookId) {
            val next = bookDao.getActive().firstOrNull()
            if (next != null) preferences.setDefaultBookId(next.id)
        }
    }

    suspend fun saveRecord(draft: RecordDraft): Long {
        require(draft.amountCents > 0) { "金额必须大于 0" }
        require(draft.categoryId > 0) { "请选择分类" }
        val now = System.currentTimeMillis()
        val recordId = database.withTransaction {
            val existing = if (draft.id == 0L) null else recordDao.getById(draft.id)
            val id = if (existing == null) {
                recordDao.insert(
                    RecordEntity(
                        bookId = draft.bookId,
                        categoryId = draft.categoryId,
                        type = draft.type,
                        amountCents = draft.amountCents,
                        dateEpochDay = draft.dateEpochDay,
                        note = draft.note.trim(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                recordDao.update(
                    existing.copy(
                        bookId = draft.bookId,
                        categoryId = draft.categoryId,
                        type = draft.type,
                        amountCents = draft.amountCents,
                        dateEpochDay = draft.dateEpochDay,
                        note = draft.note.trim(),
                        updatedAt = now,
                    ),
                )
                existing.id
            }
            saveTags(id, draft.tags)
            id
        }
        WidgetUpdater.requestUpdate(context)
        return recordId
    }

    suspend fun saveImportedRecord(
        bookId: Long,
        categoryId: Long,
        type: String,
        amountCents: Long,
        dateEpochDay: Long,
        note: String,
        tags: List<String>,
    ): Long = database.withTransaction {
        val id = recordDao.insert(
            RecordEntity(
                bookId = bookId,
                categoryId = categoryId,
                type = type,
                amountCents = amountCents,
                dateEpochDay = dateEpochDay,
                note = note,
            ),
        )
        saveTags(id, tags)
        id
    }

    private suspend fun saveTags(recordId: Long, rawTags: List<String>) {
        recordTagDao.deleteForRecord(recordId)
        val normalized = rawTags.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.take(24) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(12)
            .toList()
        val refs = normalized.map { name ->
            val key = name.lowercase(Locale.ROOT)
            val tag = tagDao.getByNormalizedName(key) ?: run {
                val id = tagDao.insert(TagEntity(name = name, normalizedName = key))
                tagDao.getByNormalizedName(key) ?: TagEntity(id = id, name = name, normalizedName = key)
            }
            RecordTagCrossRef(recordId, tag.id)
        }
        if (refs.isNotEmpty()) recordTagDao.insertAll(refs)
    }

    suspend fun deleteRecord(recordId: Long) {
        database.withTransaction {
            recordTagDao.deleteForRecord(recordId)
            recordDao.deleteById(recordId)
        }
        WidgetUpdater.requestUpdate(context)
    }

    suspend fun saveBudget(bookId: Long, categoryId: Long, monthKey: String, limitCents: Long) {
        require(limitCents >= 0) { "预算不能为负数" }
        val existing = budgetDao.get(bookId, categoryId, monthKey)
        if (limitCents == 0L) {
            if (existing != null) budgetDao.delete(existing.id)
        } else {
            budgetDao.upsert(
                BudgetEntity(
                    id = existing?.id ?: 0,
                    bookId = bookId,
                    categoryId = categoryId,
                    monthKey = monthKey,
                    limitCents = limitCents,
                ),
            )
        }
    }

    suspend fun saveRecurringRule(rule: RecurringRuleEntity): Long {
        require(rule.amountCents > 0) { "金额必须大于 0" }
        require(rule.dayOfMonth in 1..31) { "日期需在 1 到 31 之间" }
        return if (rule.id == 0L) {
            ruleDao.insert(rule.copy(createdEpochDay = DateUtils.todayEpochDay()))
        } else {
            ruleDao.update(rule)
            rule.id
        }
    }

    suspend fun setRecurringEnabled(rule: RecurringRuleEntity, enabled: Boolean) {
        ruleDao.update(rule.copy(enabled = enabled))
    }

    suspend fun deleteRecurringRule(id: Long) {
        ruleDao.delete(id)
    }

    suspend fun generateDueRecurring(today: LocalDate = LocalDate.now()): Int {
        var generated = 0
        database.withTransaction {
            val activeBooks = bookDao.getActive().mapTo(hashSetOf()) { it.id }
            ruleDao.getAll().filter { it.enabled && it.bookId in activeBooks }.forEach { rule ->
                val startMonth = YearMonth.from(LocalDate.ofEpochDay(rule.createdEpochDay))
                    .coerceAtLeast(YearMonth.from(today).minusMonths(35))
                val endMonth = YearMonth.from(today)
                var month = startMonth
                while (!month.isAfter(endMonth)) {
                    val targetEpochDay = DateUtils.epochDayInMonth(month.toString(), rule.dayOfMonth)
                    if (targetEpochDay >= rule.createdEpochDay && targetEpochDay <= today.toEpochDay()) {
                        val periodKey = month.toString()
                        if (occurrenceDao.find(rule.id, periodKey) == null) {
                            val recordId = recordDao.insert(
                                RecordEntity(
                                    bookId = rule.bookId,
                                    categoryId = rule.categoryId,
                                    type = rule.type,
                                    amountCents = rule.amountCents,
                                    dateEpochDay = targetEpochDay,
                                    note = rule.note,
                                    recurringRuleId = rule.id,
                                ),
                            )
                            occurrenceDao.insert(
                                RecurringOccurrenceEntity(
                                    ruleId = rule.id,
                                    periodKey = periodKey,
                                    recordId = recordId,
                                ),
                            )
                            generated++
                        }
                    }
                    month = month.plusMonths(1)
                }
            }
        }
        if (generated > 0) WidgetUpdater.requestUpdate(context)
        return generated
    }

    suspend fun snapshotForBackup(): BackupSnapshot = database.withTransaction {
        BackupSnapshot(
            generatedAt = System.currentTimeMillis(),
            books = bookDao.getAll(),
            categories = categoryDao.getAll(),
            records = recordDao.getAllSnapshot(),
            tags = tagDao.getAll(),
            recordTags = recordTagDao.getAll(),
            budgets = budgetDao.getAll(),
            rules = ruleDao.getAll(),
            occurrences = occurrenceDao.getAll(),
        )
    }

    suspend fun clearAllData() {
        database.withTransaction {
            recordTagDao.clear()
            tagDao.clear()
            recordDao.clear()
            budgetDao.clear()
            occurrenceDao.clear()
            ruleDao.clear()
            bookDao.deleteAll()
            val bookId = bookDao.insert(BookEntity(name = "日常"))
            preferences.setDefaultBookId(bookId)
            preferences.setBackupResult("", "")
        }
        WidgetUpdater.requestUpdate(context)
    }

    suspend fun deleteAllTagsIfUnused() {
        tagDao.deleteUnused()
    }
}

data class BackupSnapshot(
    val generatedAt: Long,
    val books: List<BookEntity>,
    val categories: List<CategoryEntity>,
    val records: List<RecordEntity>,
    val tags: List<TagEntity>,
    val recordTags: List<RecordTagCrossRef>,
    val budgets: List<BudgetEntity>,
    val rules: List<RecurringRuleEntity>,
    val occurrences: List<RecurringOccurrenceEntity>,
)
