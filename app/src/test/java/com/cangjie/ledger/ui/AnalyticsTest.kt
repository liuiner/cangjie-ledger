package com.cangjie.ledger.ui

import com.cangjie.ledger.data.BookEntity
import com.cangjie.ledger.data.CategoryEntity
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.data.RecordEntity
import com.cangjie.ledger.data.RecordItem
import com.cangjie.ledger.data.TagEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsTest {
    private val food = CategoryEntity(id = 1, name = "餐饮", type = EntryType.EXPENSE, emoji = "🍜")
    private val salary = CategoryEntity(id = 2, name = "工资", type = EntryType.INCOME, emoji = "💼")
    private val lunchTag = TagEntity(id = 1, name = "工作餐", normalizedName = "工作餐")
    private val book = BookEntity(id = 1, name = "日常")

    @Test
    fun combinesKeywordCategoryTagAndDateFilters() {
        val items = listOf(
            item(id = 1, category = food, type = EntryType.EXPENSE, amount = 2_500, note = "公司午餐"),
            item(id = 2, category = food, type = EntryType.EXPENSE, amount = 3_000, note = "家庭晚餐"),
            item(id = 3, category = salary, type = EntryType.INCOME, amount = 100_000, note = "本月工资"),
        )
        val result = LedgerAnalytics.filterRecords(
            items = items,
            keyword = "午餐",
            type = EntryType.EXPENSE,
            categoryIds = setOf(food.id),
            tagIds = setOf(lunchTag.id),
        )
        assertEquals(listOf(1L), result.map { it.record.id })
    }

    @Test
    fun calculatesBalancesAndCategoryRanking() {
        val items = listOf(
            item(id = 1, category = food, type = EntryType.EXPENSE, amount = 2_500),
            item(id = 2, category = food, type = EntryType.EXPENSE, amount = 1_000),
            item(id = 3, category = salary, type = EntryType.INCOME, amount = 10_000),
        )
        val totals = LedgerAnalytics.totals(items)
        assertEquals(3_500L, totals.expenseCents)
        assertEquals(10_000L, totals.incomeCents)
        assertEquals(6_500L, totals.balanceCents)
        assertEquals(3_500L, LedgerAnalytics.categoryExpenses(items).first().amountCents)
    }

    private fun item(
        id: Long,
        category: CategoryEntity,
        type: String,
        amount: Long,
        note: String = "",
    ) = RecordItem(
        record = RecordEntity(
            id = id,
            bookId = book.id,
            categoryId = category.id,
            type = type,
            amountCents = amount,
            dateEpochDay = LocalDate.of(2026, 10, 28).toEpochDay(),
            note = note,
        ),
        category = category,
        book = book,
        tags = if (note == "公司午餐") listOf(lunchTag) else emptyList(),
    )
}
