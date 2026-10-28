package com.cangjie.ledger.backup

import android.content.Context
import android.net.Uri
import com.cangjie.ledger.data.CategoryEntity
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.data.LedgerRepository
import com.cangjie.ledger.data.RecordItem
import com.cangjie.ledger.data.RecordTagCrossRef
import com.cangjie.ledger.data.TagEntity
import com.cangjie.ledger.util.Csv
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.util.Money
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.flow.first

class CsvTransferManager(
    private val context: Context,
    private val repository: LedgerRepository,
) {
    suspend fun exportTo(uri: Uri): Int {
        val snapshot = repository.snapshotForBackup()
        val categoryMap = snapshot.categories.associateBy { it.id }
        val bookMap = snapshot.books.associateBy { it.id }
        val tagMap = snapshot.tags.associateBy { it.id }
        val tagsByRecord = snapshot.recordTags.groupBy { it.recordId }
        val resolver = context.contentResolver
        resolver.openOutputStream(uri, "wt")?.use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                Csv.writeRow(
                    writer,
                    listOf("日期", "类型", "金额", "账本", "分类", "备注", "标签"),
                )
                snapshot.records.forEach { record ->
                    val tags = tagsByRecord[record.id].orEmpty()
                        .mapNotNull { tagMap[it.tagId]?.name }
                        .joinToString("|")
                    Csv.writeRow(
                        writer,
                        listOf(
                            LocalDate.ofEpochDay(record.dateEpochDay).toString(),
                            if (record.type == EntryType.INCOME) "收入" else "支出",
                            Money.plain(record.amountCents),
                            bookMap[record.bookId]?.name ?: "日常",
                            categoryMap[record.categoryId]?.name ?: "其他",
                            record.note,
                            tags,
                        ),
                    )
                }
            }
        } ?: error("无法写入 CSV 文件")
        return snapshot.records.size
    }

    suspend fun importFrom(uri: Uri): ImportResult {
        val rows = context.contentResolver.openInputStream(uri)?.use { input ->
            Csv.readRows(InputStreamReader(input, Charsets.UTF_8))
        } ?: error("无法读取 CSV 文件")
        if (rows.isEmpty()) return ImportResult(0, 0, 0, listOf("文件为空"))

        val snapshot = repository.snapshotForBackup()
        val bookIdByName = snapshot.books.associate { it.name.lowercase(Locale.ROOT) to it.id }
            .toMutableMap()
        val categoryIdByKey = snapshot.categories.associate {
            "${it.type}|${it.name.lowercase(Locale.ROOT)}" to it.id
        }.toMutableMap()
        val tagsById = snapshot.tags.associateBy { it.id }
        val tagNamesByRecord = snapshot.recordTags.groupBy { it.recordId }
            .mapValues { (_, refs) -> refs.mapNotNull { tagsById[it.tagId]?.name }.sorted() }
        val categoryNames = snapshot.categories.associate { it.id to it.name }
        val bookNames = snapshot.books.associate { it.id to it.name }
        val existingKeys = snapshot.records.mapTo(hashSetOf()) { record ->
            fingerprint(
                record.dateEpochDay.toString(),
                record.type,
                record.amountCents,
                bookNames[record.bookId] ?: "",
                categoryNames[record.categoryId] ?: "",
                record.note,
                tagNamesByRecord[record.id].orEmpty(),
            )
        }

        val headerOffset = if (rows.first().values.firstOrNull()?.removePrefix("\uFEFF")
                ?.contains("日期") == true
        ) 1 else 0
        var imported = 0
        var duplicates = 0
        var badRows = 0
        val errors = mutableListOf<String>()

        rows.drop(headerOffset).forEach { row ->
            val values = row.values.map { it.trim() }
            if (values.size < 6) {
                badRows++
                if (errors.size < 6) errors += "第 ${row.lineNumber} 行：列数不足"
                return@forEach
            }
            try {
                val date = LocalDate.parse(values[0])
                val type = when (values[1].lowercase(Locale.ROOT)) {
                    "收入", "income", "in" -> EntryType.INCOME
                    "支出", "expense", "out" -> EntryType.EXPENSE
                    else -> error("未知类型 ${values[1]}")
                }
                val cents = Money.expressionToCents(values[2].removePrefix("¥").replace(",", ""))
                val bookName = values[3].ifBlank { "日常" }
                val categoryName = values[4].ifBlank {
                    if (type == EntryType.INCOME) "其他收入" else "其他"
                }
                val note = values[5]
                val tagNames = values.getOrNull(6).orEmpty()
                    .split('|', ',', '，')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()

                val bookId = bookIdByName[bookName.lowercase(Locale.ROOT)] ?: run {
                    val created = repository.saveBook(com.cangjie.ledger.data.BookEntity(name = bookName))
                    bookIdByName[bookName.lowercase(Locale.ROOT)] = created.id
                    created.id
                }
                val fallbackName = if (type == EntryType.INCOME) "其他收入" else "其他"
                val categoryId = categoryIdByKey["${type}|${categoryName.lowercase(Locale.ROOT)}"]
                    ?: categoryIdByKey["${type}|${fallbackName.lowercase(Locale.ROOT)}"]
                    ?: error("找不到分类 $categoryName")
                val key = fingerprint(
                    date.toEpochDay().toString(),
                    type,
                    cents,
                    bookName,
                    categoryName,
                    note,
                    tagNames,
                )
                if (!existingKeys.add(key)) {
                    duplicates++
                    return@forEach
                }
                repository.saveImportedRecord(
                    bookId = bookId,
                    categoryId = categoryId,
                    type = type,
                    amountCents = cents,
                    dateEpochDay = date.toEpochDay(),
                    note = note,
                    tags = tagNames,
                )
                imported++
            } catch (error: DateTimeParseException) {
                badRows++
                if (errors.size < 6) errors += "第 ${row.lineNumber} 行：日期格式应为 yyyy-MM-dd"
            } catch (error: Exception) {
                badRows++
                if (errors.size < 6) errors += "第 ${row.lineNumber} 行：${error.message ?: "无效数据"}"
            }
        }
        return ImportResult(imported, duplicates, badRows, errors)
    }

    private fun fingerprint(
        date: String,
        type: String,
        cents: Long,
        book: String,
        category: String,
        note: String,
        tags: List<String>,
    ): String = listOf(
        date,
        type,
        cents.toString(),
        book.lowercase(Locale.ROOT),
        category.lowercase(Locale.ROOT),
        note.trim().lowercase(Locale.ROOT),
        tags.joinToString("|") { it.lowercase(Locale.ROOT) },
    ).joinToString("\u001F")
}

data class ImportResult(
    val imported: Int,
    val duplicates: Int,
    val badRows: Int,
    val errors: List<String>,
)
