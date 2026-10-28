package com.cangjie.ledger.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cangjie.ledger.LedgerApplication
import com.cangjie.ledger.backup.CsvTransferManager
import com.cangjie.ledger.data.AppSettings
import com.cangjie.ledger.data.BookEntity
import com.cangjie.ledger.data.BudgetEntity
import com.cangjie.ledger.data.CategoryEntity
import com.cangjie.ledger.data.RecordDraft
import com.cangjie.ledger.data.RecordItem
import com.cangjie.ledger.data.RecordTagCrossRef
import com.cangjie.ledger.data.RecurringRuleEntity
import com.cangjie.ledger.data.TagEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val records: List<RecordItem> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val initialized: Boolean = false,
)

private data class CoreData(
    val books: List<BookEntity>,
    val categories: List<CategoryEntity>,
    val records: List<RecordItem>,
    val tags: List<TagEntity>,
    val budgets: List<BudgetEntity>,
)

class AppViewModel(
    private val application: LedgerApplication,
) : ViewModel() {
    private val repository = application.repository
    private val repositoryRecords = repository.records
    private val repositoryTags = repository.tags
    private val repositoryRefs = repository.recordTags

    private val mutableBusy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = mutableBusy
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = mutableMessage

    private val core: kotlinx.coroutines.flow.Flow<CoreData> = combine(
        repository.books,
        repository.categories,
        repositoryRecords,
        repositoryTags,
        repositoryRefs,
    ) { books, categories, records, tags, refs ->
        val bookMap = books.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }
        val tagMap = tags.associateBy { it.id }
        val tagsByRecord = refs.groupBy { it.recordId }
        CoreData(
            books = books,
            categories = categories,
            records = records.map { record ->
                RecordItem(
                    record = record,
                    category = categoryMap[record.categoryId],
                    book = bookMap[record.bookId],
                    tags = tagsByRecord[record.id].orEmpty().mapNotNull { tagMap[it.tagId] }
                        .sortedBy { it.name },
                )
            },
            tags = tags,
            budgets = emptyList(),
        )
    }

    val state: StateFlow<AppUiState> = combine(
        core,
        repository.budgets,
        repository.recurringRules,
        repository.settings,
    ) { data, budgets, rules, settings ->
        AppUiState(
            records = data.records,
            books = data.books,
            categories = data.categories,
            tags = data.tags,
            budgets = budgets,
            recurringRules = rules,
            settings = settings,
            initialized = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch {
            repository.initialize()
            runCatching { repository.generateDueRecurring() }
        }
    }

    fun saveRecord(draft: RecordDraft, onSuccess: () -> Unit) = runAction {
        repository.saveRecord(draft)
        onSuccess()
    }

    fun deleteRecord(recordId: Long, onSuccess: () -> Unit) = runAction("记录已删除") {
        repository.deleteRecord(recordId)
        repository.deleteAllTagsIfUnused()
        onSuccess()
    }

    fun saveBook(book: BookEntity, onSuccess: () -> Unit = {}) = runAction("账本已保存") {
        repository.saveBook(book)
        onSuccess()
    }

    fun setBookArchived(bookId: Long, archived: Boolean) = runAction(
        if (archived) "账本已归档" else "账本已恢复",
    ) {
        repository.archiveBook(bookId, archived)
    }

    fun setDefaultBook(bookId: Long) = runAction {
        repository.preferences.setDefaultBookId(bookId)
    }

    fun saveBudget(
        bookId: Long,
        categoryId: Long,
        monthKey: String,
        limitCents: Long,
        onSuccess: () -> Unit = {},
    ) = runAction(if (limitCents == 0L) "预算已清除" else "预算已保存") {
        repository.saveBudget(bookId, categoryId, monthKey, limitCents)
        onSuccess()
    }

    fun saveRecurringRule(rule: RecurringRuleEntity, onSuccess: () -> Unit = {}) =
        runAction("周期规则已保存") {
            repository.saveRecurringRule(rule)
            onSuccess()
        }

    fun setRecurringEnabled(rule: RecurringRuleEntity, enabled: Boolean) = runAction {
        repository.setRecurringEnabled(rule, enabled)
    }

    fun deleteRecurringRule(id: Long) = runAction("周期规则已删除") {
        repository.deleteRecurringRule(id)
    }

    fun generateRecurringNow() = runAction("周期账单已检查") {
        repository.generateDueRecurring()
    }

    fun setThemeMode(value: String) = runAction {
        repository.preferences.setThemeMode(value)
    }

    fun setDynamicColor(value: Boolean) = runAction {
        repository.preferences.setDynamicColor(value)
    }

    fun setAutoBackup(value: Boolean) = runAction {
        repository.preferences.setAutoBackup(value)
    }

    fun saveBackupFolder(uri: Uri) = runAction("备份目录已设置") {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        repository.preferences.setBackupUri(uri.toString())
        repository.preferences.setAutoBackup(true)
    }

    fun backupNow() = runAction("备份完成") {
        application.backupManager.backupNow().getOrThrow()
    }

    fun exportCsv(uri: Uri) = runAction {
        val count = CsvTransferManager(application, repository).exportTo(uri)
        mutableMessage.value = "已导出 $count 条记录"
    }

    fun importCsv(uri: Uri) = runAction {
        val result = CsvTransferManager(application, repository).importFrom(uri)
        val details = buildString {
            append("导入 ${result.imported} 条")
            if (result.duplicates > 0) append("，跳过重复 ${result.duplicates} 条")
            if (result.badRows > 0) append("，坏行 ${result.badRows} 条")
            if (result.errors.isNotEmpty()) append("。${result.errors.first()}")
        }
        mutableMessage.value = details
    }

    fun clearAllData() = runAction("数据已清空") {
        repository.clearAllData()
    }

    fun clearMessage() {
        mutableMessage.value = null
    }

    private fun runAction(
        successMessage: String? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            if (mutableBusy.value) return@launch
            mutableBusy.value = true
            runCatching { block() }
                .onSuccess {
                    if (successMessage != null) mutableMessage.value = successMessage
                }
                .onFailure { error ->
                    mutableMessage.value = "操作失败：${error.message ?: "未知错误"}"
                }
            mutableBusy.value = false
        }
    }

    class Factory(private val application: LedgerApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(application) as T
    }
}
