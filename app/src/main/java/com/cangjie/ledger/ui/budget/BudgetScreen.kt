package com.cangjie.ledger.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.data.BudgetProgress
import com.cangjie.ledger.data.CategoryEntity
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.LedgerAnalytics
import com.cangjie.ledger.ui.components.CategoryAvatar
import com.cangjie.ledger.ui.components.MonthSwitcher
import com.cangjie.ledger.ui.components.SectionCard
import com.cangjie.ledger.ui.theme.ExpenseRed
import com.cangjie.ledger.ui.theme.MutedInk
import com.cangjie.ledger.ui.theme.WarmAmber
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    state: AppUiState,
    onSaveBudget: (Long, Long, String, Long, () -> Unit) -> Unit,
) {
    val activeBooks = state.books.filterNot { it.archived }
    val bookId = activeBooks.firstOrNull { it.id == state.settings.defaultBookId }?.id
        ?: activeBooks.firstOrNull()?.id ?: 0L
    var monthKey by rememberSaveable { mutableStateOf(DateUtils.monthKey()) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val expenseCategories = state.categories.filter { it.type == EntryType.EXPENSE }
    val monthRecords = remember(state.records, bookId, monthKey) {
        LedgerAnalytics.inMonth(
            state.records.filter { it.record.bookId == bookId },
            monthKey,
        )
    }
    val progress = remember(
        expenseCategories,
        state.budgets,
        monthRecords,
        bookId,
        monthKey,
    ) {
        expenseCategories.map { category ->
            BudgetProgress(
                category = category,
                budget = state.budgets.firstOrNull {
                    it.bookId == bookId && it.categoryId == category.id && it.monthKey == monthKey
                },
                spentCents = monthRecords
                    .filter { it.record.categoryId == category.id }
                    .sumOf { it.record.amountCents },
            )
        }
    }
    val totalBudget = progress.sumOf { it.limitCents }
    val totalSpent = progress.filter { it.limitCents > 0 }.sumOf { it.spentCents }
    val totalRemaining = totalBudget - totalSpent

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("预算") })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            MonthSwitcher(
                monthKey = monthKey,
                onPrevious = { monthKey = DateUtils.shiftMonth(monthKey, -1) },
                onNext = { monthKey = DateUtils.shiftMonth(monthKey, 1) },
            )
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "本月预算",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = Money.format(totalSpent),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = " / ${Money.format(totalBudget)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    if (totalBudget > 0) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = {
                                (totalSpent.toFloat() / totalBudget).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp),
                            color = if (totalRemaining < 0) ExpenseRed else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            text = if (totalRemaining >= 0) {
                                "剩余 ${Money.format(totalRemaining)}"
                            } else {
                                "已超支 ${Money.format(-totalRemaining)}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (totalRemaining < 0) ExpenseRed else MutedInk,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        Text(
                            text = "点下方分类设置本月预算",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            Text(
                text = "分类预算",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    progress.forEachIndexed { index, item ->
                        BudgetRow(
                            progress = item,
                            onClick = { editingCategory = item.category },
                        )
                        if (index != progress.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp, end = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    editingCategory?.let { category ->
        val existing = state.budgets.firstOrNull {
            it.bookId == bookId && it.categoryId == category.id && it.monthKey == monthKey
        }
        BudgetDialog(
            category = category,
            initialExpression = existing?.let { Money.plain(it.limitCents) }.orEmpty(),
            onDismiss = { editingCategory = null },
            onSave = { cents ->
                onSaveBudget(bookId, category.id, monthKey, cents) {
                    editingCategory = null
                }
            },
        )
    }
}

@Composable
private fun BudgetRow(
    progress: BudgetProgress,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            emoji = progress.category.emoji,
            color = progress.category.color,
            modifier = Modifier.size(40.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = progress.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (progress.limitCents == 0L) {
                    Text(
                        text = "设置预算",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = if (progress.overspent) "超支 ${Money.format(-progress.remainingCents)}"
                        else "剩余 ${Money.format(progress.remainingCents)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (progress.overspent) ExpenseRed else MutedInk,
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            if (progress.limitCents > 0L) {
                LinearProgressIndicator(
                    progress = { progress.ratio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when {
                        progress.overspent -> ExpenseRed
                        progress.ratio >= 0.8f -> WarmAmber
                        else -> Color(progress.category.color)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${Money.format(progress.spentCents)} / ${Money.format(progress.limitCents)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (progress.overspent) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp),
                )
            } else {
                Text(
                    text = "已支出 ${Money.format(progress.spentCents)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        androidx.compose.material3.Icon(
            Icons.Rounded.Edit,
            contentDescription = "编辑预算",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun BudgetDialog(
    category: CategoryEntity,
    initialExpression: String,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var expression by rememberSaveable(category.id) { mutableStateOf(initialExpression) }
    val cents = remember(expression) {
        if (expression.isBlank()) 0L else runCatching { Money.expressionToCents(expression) }.getOrNull()
    }
    val invalid = expression.isNotBlank() && cents == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${category.emoji} ${category.name}预算") },
        text = {
            Column {
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it.take(32) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("月度预算") },
                    prefix = { Text("¥") },
                    placeholder = { Text("例如 1500 或 1200+300") },
                    isError = invalid,
                    supportingText = {
                        Text(
                            when {
                                invalid -> "请输入有效金额"
                                cents != null && cents > 0 -> "每月 ${Money.format(cents)}"
                                else -> "留空或输入 0 可清除预算"
                            },
                        )
                    },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = if (expression.isBlank()) 0L else cents ?: return@TextButton
                    onSave(amount)
                },
                enabled = !invalid,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
