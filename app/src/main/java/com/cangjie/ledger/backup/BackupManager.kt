package com.cangjie.ledger.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.cangjie.ledger.data.BackupSnapshot
import com.cangjie.ledger.data.LedgerRepository
import com.cangjie.ledger.util.DateUtils
import java.io.OutputStreamWriter
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val context: Context,
    private val repository: LedgerRepository,
) {
    suspend fun backupIfDue(): Result<Unit> {
        val settings = repository.settings.first()
        if (!settings.autoBackup || settings.backupUri.isBlank()) {
            return Result.success(Unit)
        }
        val today = LocalDate.now().toString()
        if (settings.lastBackupDate == today) return Result.success(Unit)
        return backupNow()
    }

    suspend fun backupNow(): Result<Unit> = runCatching {
        val settings = repository.settings.first()
        require(settings.backupUri.isNotBlank()) { "尚未选择备份目录" }
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(settings.backupUri))
            ?: error("备份目录不可访问")
        require(tree.canWrite()) { "备份目录没有写入权限，请重新选择" }

        val snapshot = repository.snapshotForBackup()
        val date = LocalDate.now().toString()
        val fileName = "cangjie-backup-$date.json"
        val existing = tree.findFile(fileName)
        val file = existing ?: tree.createFile("application/json", fileName)
            ?: error("无法创建备份文件")
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                writer.write(snapshotToJson(snapshot).toString())
            }
        } ?: error("无法写入备份文件")

        repository.preferences.setBackupResult(date, "备份完成")
        Unit
    }.onFailure { error ->
        repository.preferences.setBackupStatus("备份失败：${error.message ?: "未知错误"}")
    }

    private fun snapshotToJson(snapshot: BackupSnapshot): JSONObject = JSONObject().apply {
        put("format", "cangjie-ledger-backup")
        put("version", 1)
        put("generatedAt", snapshot.generatedAt)
        put("books", JSONArray().apply {
            snapshot.books.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("color", item.color)
                    put("archived", item.archived)
                    put("createdAt", item.createdAt)
                })
            }
        })
        put("categories", JSONArray().apply {
            snapshot.categories.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("type", item.type)
                    put("emoji", item.emoji)
                    put("color", item.color)
                    put("sortOrder", item.sortOrder)
                })
            }
        })
        put("records", JSONArray().apply {
            snapshot.records.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("bookId", item.bookId)
                    put("categoryId", item.categoryId)
                    put("type", item.type)
                    put("amountCents", item.amountCents)
                    put("dateEpochDay", item.dateEpochDay)
                    put("note", item.note)
                    put("recurringRuleId", item.recurringRuleId ?: JSONObject.NULL)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        })
        put("tags", JSONArray().apply {
            snapshot.tags.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("normalizedName", item.normalizedName)
                })
            }
        })
        put("recordTags", JSONArray().apply {
            snapshot.recordTags.forEach { item ->
                put(JSONObject().apply {
                    put("recordId", item.recordId)
                    put("tagId", item.tagId)
                })
            }
        })
        put("budgets", JSONArray().apply {
            snapshot.budgets.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("bookId", item.bookId)
                    put("categoryId", item.categoryId)
                    put("monthKey", item.monthKey)
                    put("limitCents", item.limitCents)
                })
            }
        })
        put("rules", JSONArray().apply {
            snapshot.rules.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("bookId", item.bookId)
                    put("categoryId", item.categoryId)
                    put("type", item.type)
                    put("amountCents", item.amountCents)
                    put("dayOfMonth", item.dayOfMonth)
                    put("note", item.note)
                    put("enabled", item.enabled)
                    put("createdEpochDay", item.createdEpochDay)
                })
            }
        })
        put("occurrences", JSONArray().apply {
            snapshot.occurrences.forEach { item ->
                put(JSONObject().apply {
                    put("ruleId", item.ruleId)
                    put("periodKey", item.periodKey)
                    put("recordId", item.recordId)
                    put("generatedAt", item.generatedAt)
                })
            }
        })
    }
}
