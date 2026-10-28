package com.cangjie.ledger.ui

import com.cangjie.ledger.data.CategoryAmount
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.data.MonthAmount
import com.cangjie.ledger.data.MonthlyTotals
import com.cangjie.ledger.data.RecordItem
import com.cangjie.ledger.util.DateUtils

object LedgerAnalytics {
    fun totals(items: List<RecordItem>): MonthlyTotals = MonthlyTotals(
        incomeCents = items.filter { it.record.type == EntryType.INCOME }.sumOf { it.record.amountCents },
        expenseCents = items.filter { it.record.type == EntryType.EXPENSE }.sumOf { it.record.amountCents },
    )

    fun inMonth(items: List<RecordItem>, monthKey: String): List<RecordItem> =
        items.filter { DateUtils.monthKey(it.record.dateEpochDay) == monthKey }

    fun categoryExpenses(items: List<RecordItem>): List<CategoryAmount> =
        items.asSequence()
            .filter { it.record.type == EntryType.EXPENSE }
            .filter { it.category != null }
            .groupBy { it.category!!.id }
            .mapNotNull { (_, rows) ->
                val category = rows.first().category ?: return@mapNotNull null
                category to rows.sumOf { it.record.amountCents }
            }
            .map { CategoryAmount(it.first, it.second) }
            .sortedByDescending { it.amountCents }

    fun sixMonthTrend(items: List<RecordItem>, endMonth: String): List<MonthAmount> =
        DateUtils.lastNMonths(endMonth, 6).map { month ->
            val totals = totals(inMonth(items, month))
            MonthAmount(month, totals.incomeCents, totals.expenseCents)
        }

    fun filterRecords(
        items: List<RecordItem>,
        keyword: String = "",
        type: String? = null,
        categoryIds: Set<Long> = emptySet(),
        tagIds: Set<Long> = emptySet(),
        startEpochDay: Long? = null,
        endEpochDay: Long? = null,
    ): List<RecordItem> {
        val cleanKeyword = keyword.trim()
        return items.filter { item ->
            (type == null || item.record.type == type) &&
                (cleanKeyword.isEmpty() || item.record.note.contains(cleanKeyword, ignoreCase = true)) &&
                (categoryIds.isEmpty() || item.record.categoryId in categoryIds) &&
                (tagIds.isEmpty() || item.tags.any { it.id in tagIds }) &&
                (startEpochDay == null || item.record.dateEpochDay >= startEpochDay) &&
                (endEpochDay == null || item.record.dateEpochDay <= endEpochDay)
        }
    }
}

