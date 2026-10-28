package com.cangjie.ledger.data

data class RecordItem(
    val record: RecordEntity,
    val category: CategoryEntity?,
    val book: BookEntity?,
    val tags: List<TagEntity>,
)

data class RecordDraft(
    val id: Long = 0,
    val bookId: Long,
    val categoryId: Long,
    val type: String,
    val amountCents: Long,
    val dateEpochDay: Long,
    val note: String,
    val tags: List<String>,
)

data class BudgetProgress(
    val category: CategoryEntity,
    val budget: BudgetEntity?,
    val spentCents: Long,
) {
    val limitCents: Long get() = budget?.limitCents ?: 0L
    val remainingCents: Long get() = limitCents - spentCents
    val ratio: Float
        get() = if (limitCents <= 0L) 0f else (spentCents.toFloat() / limitCents).coerceAtLeast(0f)
    val overspent: Boolean get() = limitCents > 0L && spentCents > limitCents
}

data class MonthlyTotals(
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

data class CategoryAmount(
    val category: CategoryEntity,
    val amountCents: Long,
)

data class MonthAmount(
    val monthKey: String,
    val incomeCents: Long,
    val expenseCents: Long,
)

