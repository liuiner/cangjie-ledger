package com.cangjie.ledger.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.data.RecurringRuleEntity
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.AppViewModel
import com.cangjie.ledger.ui.components.CategoryAvatar
import com.cangjie.ledger.ui.components.EmptyState
import com.cangjie.ledger.ui.components.SectionCard
import com.cangjie.ledger.ui.theme.ExpenseRed
import com.cangjie.ledger.ui.theme.IncomeGreen
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    var editingRule by remember { mutableStateOf<RecurringRuleEntity?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var deletingRule by remember { mutableStateOf<RecurringRuleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("周期记账") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::generateRecurringNow) {
                        Text("立即检查")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("新建规则") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "每月到达指定日期后，会自动生成一条账单。短月份会使用当月最后一天。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            if (state.recurringRules.isEmpty()) {
                EmptyState(
                    title = "还没有周期规则",
                    description = "适合房租、工资、订阅等每月固定收支。",
                )
            } else {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.recurringRules.forEachIndexed { index, rule ->
                            RecurringRow(
                                rule = rule,
                                state = state,
                                onToggle = { enabled ->
                                    viewModel.setRecurringEnabled(rule, enabled)
                                },
                                onEdit = { editingRule = rule },
                                onDelete = { deletingRule = rule },
                            )
                            if (index != state.recurringRules.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }

    if (showNew || editingRule != null) {
        RuleDialog(
            state = state,
            rule = editingRule,
            onDismiss = {
                showNew = false
                editingRule = null
            },
            onSave = { rule ->
                viewModel.saveRecurringRule(rule) {
                    showNew = false
                    editingRule = null
                }
            },
        )
    }
    deletingRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { deletingRule = null },
            title = { Text("删除周期规则？") },
            text = { Text("已生成的账单不会删除，之后也不会再自动生成。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecurringRule(rule.id)
                        deletingRule = null
                    },
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingRule = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun RecurringRow(
    rule: RecurringRuleEntity,
    state: AppUiState,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = state.categories.firstOrNull { it.id == rule.categoryId }
    val book = state.books.firstOrNull { it.id == rule.bookId }
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            emoji = category?.emoji ?: "🔁",
            color = category?.color ?: 0xFF8AA39B,
            modifier = Modifier.size(40.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category?.name ?: "未分类",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = (if (rule.type == EntryType.INCOME) "+" else "-") +
                        Money.format(rule.amountCents),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (rule.type == EntryType.INCOME) IncomeGreen else ExpenseRed,
                )
            }
            Text(
                text = "每月 ${rule.dayOfMonth} 日 · ${book?.name.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rule.note.isNotBlank()) {
                Text(
                    text = rule.note,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Switch(checked = rule.enabled, onCheckedChange = onToggle)
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "规则操作")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("编辑规则") },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("删除规则") },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleDialog(
    state: AppUiState,
    rule: RecurringRuleEntity?,
    onDismiss: () -> Unit,
    onSave: (RecurringRuleEntity) -> Unit,
) {
    val activeBooks = state.books.filterNot { it.archived }
    var type by rememberSaveable(rule?.id) { mutableStateOf(rule?.type ?: EntryType.EXPENSE) }
    var expression by rememberSaveable(rule?.id) {
        mutableStateOf(rule?.let { Money.plain(it.amountCents) }.orEmpty())
    }
    var bookId by rememberSaveable(rule?.id) {
        mutableStateOf(
            rule?.bookId
                ?: activeBooks.firstOrNull { it.id == state.settings.defaultBookId }?.id
                ?: activeBooks.firstOrNull()?.id
                ?: 0L,
        )
    }
    var categoryId by rememberSaveable(rule?.id) {
        mutableStateOf(
            rule?.categoryId
                ?: state.categories.firstOrNull { it.type == EntryType.EXPENSE }?.id
                ?: 0L,
        )
    }
    var dayText by rememberSaveable(rule?.id) { mutableStateOf((rule?.dayOfMonth ?: 1).toString()) }
    var note by rememberSaveable(rule?.id) { mutableStateOf(rule?.note.orEmpty()) }
    var bookMenu by remember { mutableStateOf(false) }

    val categories = state.categories.filter { it.type == type }
    val cents = remember(expression) {
        runCatching { Money.expressionToCents(expression) }.getOrNull()
    }
    val day = dayText.toIntOrNull()
    val book = activeBooks.firstOrNull { it.id == bookId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule == null) "新建周期规则" else "编辑周期规则") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == EntryType.EXPENSE,
                        onClick = {
                            type = EntryType.EXPENSE
                            categoryId = state.categories.firstOrNull {
                                it.type == EntryType.EXPENSE
                            }?.id ?: 0L
                        },
                        label = { Text("支出") },
                    )
                    FilterChip(
                        selected = type == EntryType.INCOME,
                        onClick = {
                            type = EntryType.INCOME
                            categoryId = state.categories.firstOrNull {
                                it.type == EntryType.INCOME
                            }?.id ?: 0L
                        },
                        label = { Text("收入") },
                    )
                }
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it.take(36) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("金额算式") },
                    prefix = { Text("¥") },
                    placeholder = { Text("2500") },
                    singleLine = true,
                    isError = expression.isNotBlank() && cents == null,
                )
                OutlinedButton(
                    onClick = { bookMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(book?.name ?: "选择账本", modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    DropdownMenu(expanded = bookMenu, onDismissRequest = { bookMenu = false }) {
                        activeBooks.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    bookId = item.id
                                    bookMenu = false
                                },
                            )
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text("${category.emoji} ${category.name}") },
                        )
                    }
                }
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { value ->
                        dayText = value.filter(Char::isDigit).take(2)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("每月几号") },
                    suffix = { Text("日") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = dayText.isNotBlank() && day !in 1..31,
                    supportingText = { Text("29–31 号在短月份按最后一天生成") },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(100) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    label = { Text("备注") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = cents ?: return@TextButton
                    val safeDay = day ?: return@TextButton
                    onSave(
                        RecurringRuleEntity(
                            id = rule?.id ?: 0L,
                            bookId = bookId,
                            categoryId = categoryId,
                            type = type,
                            amountCents = amount,
                            dayOfMonth = safeDay,
                            note = note.trim(),
                            enabled = rule?.enabled ?: true,
                            createdEpochDay = rule?.createdEpochDay ?: DateUtils.todayEpochDay(),
                        ),
                    )
                },
                enabled = cents != null && day in 1..31 && categoryId > 0 && bookId > 0,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
