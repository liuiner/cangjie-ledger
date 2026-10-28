package com.cangjie.ledger.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.data.BookEntity
import com.cangjie.ledger.data.CategoryEntity
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.data.RecordDraft
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.AppViewModel
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordEditorScreen(
    state: AppUiState,
    recordId: Long?,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val existing = state.records.firstOrNull { it.record.id == recordId }
    val activeBooks = state.books.filterNot { it.archived }
    var initialized by rememberSaveable(recordId) { mutableStateOf(false) }
    var expression by rememberSaveable(recordId) { mutableStateOf("") }
    var type by rememberSaveable(recordId) { mutableStateOf(EntryType.EXPENSE) }
    var bookId by rememberSaveable(recordId) { mutableStateOf(0L) }
    var categoryId by rememberSaveable(recordId) { mutableStateOf(0L) }
    var dateEpochDay by rememberSaveable(recordId) { mutableStateOf(DateUtils.todayEpochDay()) }
    var note by rememberSaveable(recordId) { mutableStateOf("") }
    var tagsText by rememberSaveable(recordId) { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBookMenu by remember { mutableStateOf(false) }

    val currentCategories = state.categories.filter { it.type == type }
    val selectedBook = activeBooks.firstOrNull { it.id == bookId }
    val amountCents = remember(expression) {
        runCatching { Money.expressionToCents(expression) }.getOrNull()
    }
    val expressionError = remember(expression) {
        if (expression.isBlank()) "请输入金额" else runCatching {
            Money.expressionToCents(expression)
        }.exceptionOrNull()?.message
    }

    LaunchedEffect(recordId, existing?.record?.id, activeBooks.size, state.categories.size) {
        if (initialized) return@LaunchedEffect
        if (recordId == null) {
            if (activeBooks.isNotEmpty() && state.categories.isNotEmpty()) {
                bookId = activeBooks.firstOrNull { it.id == state.settings.defaultBookId }?.id
                    ?: activeBooks.first().id
                categoryId = state.categories.firstOrNull { it.type == EntryType.EXPENSE }?.id ?: 0L
                initialized = true
            }
        } else if (existing != null) {
            expression = Money.plain(existing.record.amountCents)
            type = existing.record.type
            bookId = existing.record.bookId
            categoryId = existing.record.categoryId
            dateEpochDay = existing.record.dateEpochDay
            note = existing.record.note
            tagsText = existing.tags.joinToString("，") { it.name }
            initialized = true
        }
    }

    LaunchedEffect(type, state.categories.size) {
        if (state.categories.none { it.id == categoryId }) {
            categoryId = state.categories.firstOrNull { it.type == type }?.id ?: 0L
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recordId == null) "记一笔" else "编辑记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (recordId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Button(
                    onClick = {
                        val cents = amountCents ?: return@Button
                        viewModel.saveRecord(
                            RecordDraft(
                                id = recordId ?: 0L,
                                bookId = bookId,
                                categoryId = categoryId,
                                type = type,
                                amountCents = cents,
                                dateEpochDay = dateEpochDay,
                                note = note,
                                tags = parseTagInput(tagsText),
                            ),
                            onSuccess = onBack,
                        )
                    },
                    enabled = amountCents != null && bookId > 0 && categoryId > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(50.dp),
                ) {
                    Text(if (recordId == null) "保存记录" else "保存修改")
                }
            }
        },
    ) { padding ->
        if (recordId != null && !initialized) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("记录不存在或已删除")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                onValueChange = { expression = it.take(48) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                textStyle = MaterialTheme.typography.headlineMedium,
                label = { Text("金额算式") },
                prefix = { Text("¥") },
                placeholder = { Text("12.5+8*0.5") },
                singleLine = true,
                isError = expression.isNotBlank() && amountCents == null,
                supportingText = {
                    Text(
                        when {
                            amountCents != null -> "= ${Money.format(amountCents)}"
                            else -> expressionError.orEmpty()
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            CalculatorPad(
                onInput = { token ->
                    expression = when (token) {
                        "⌫" -> expression.dropLast(1)
                        "C" -> ""
                        else -> expression + token
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("账本", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { showBookMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text(selectedBook?.name ?: "选择账本", modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                DropdownMenu(
                    expanded = showBookMenu,
                    onDismissRequest = { showBookMenu = false },
                ) {
                    activeBooks.forEach { book: BookEntity ->
                        DropdownMenuItem(
                            text = { Text(book.name) },
                            onClick = {
                                bookId = book.id
                                showBookMenu = false
                            },
                        )
                    }
                }
            }

            Text(
                "分类",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                currentCategories.forEach { category: CategoryEntity ->
                    FilterChip(
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id },
                        label = { Text("${category.emoji} ${category.name}") },
                    )
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Text(
                    text = "  ${DateUtils.dateLabel(dateEpochDay)}  ${DateUtils.weekdayLabel(dateEpochDay)}",
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(160) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = { Text("备注") },
                placeholder = { Text("可选，例如：和朋友吃午饭") },
                minLines = 2,
                maxLines = 4,
            )

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it.take(96) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = { Text("标签") },
                placeholder = { Text("用逗号分隔，例如：报销，聚餐") },
                singleLine = true,
            )
            if (state.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.tags.take(16).forEach { tag ->
                        val selected = parseTagInput(tagsText).any {
                            it.equals(tag.name, ignoreCase = true)
                        }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val names = parseTagInput(tagsText).toMutableList()
                                if (selected) {
                                    names.removeAll { it.equals(tag.name, ignoreCase = true) }
                                } else {
                                    names += tag.name
                                }
                                tagsText = names.distinct().joinToString("，")
                            },
                            label = { Text("#${tag.name}") },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(dateEpochDay)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dateEpochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这条记录？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        recordId?.let { id ->
                            viewModel.deleteRecord(id, onSuccess = onBack)
                        }
                    },
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CalculatorPad(onInput: (String) -> Unit) {
    val keys = listOf("7", "8", "9", "÷", "4", "5", "6", "×", "1", "2", "3", "-", "0", ".", "⌫", "+")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keys.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = { onInput(key) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        if (key == "⌫") {
                            Icon(
                                Icons.Rounded.Backspace,
                                contentDescription = "退格",
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(key, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("(", ")", "C").forEach { key ->
                OutlinedButton(
                    onClick = { onInput(key) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text(key) }
            }
        }
    }
}

private fun parseTagInput(value: String): List<String> =
    value.split(',', '，', '|')
        .map { it.trim().removePrefix("#") }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .take(12)
