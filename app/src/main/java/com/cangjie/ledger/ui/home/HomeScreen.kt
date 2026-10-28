package com.cangjie.ledger.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.data.RecordItem
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.LedgerAnalytics
import com.cangjie.ledger.ui.components.DayHeader
import com.cangjie.ledger.ui.components.EmptyState
import com.cangjie.ledger.ui.components.MonthSwitcher
import com.cangjie.ledger.ui.components.RecordRow
import com.cangjie.ledger.ui.components.SectionCard
import com.cangjie.ledger.ui.theme.ExpenseRed
import com.cangjie.ledger.ui.theme.IncomeGreen
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AppUiState,
    onOpenRecord: (Long) -> Unit,
    onSearch: () -> Unit,
    onManageBooks: () -> Unit,
    onSelectBook: (Long) -> Unit,
) {
    val activeBooks = state.books.filterNot { it.archived }
    val currentBook = activeBooks.firstOrNull { it.id == state.settings.defaultBookId }
        ?: activeBooks.firstOrNull()
    var monthKey by rememberSaveable { mutableStateOf(DateUtils.monthKey()) }
    var typeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var bookMenuOpen by remember { mutableStateOf(false) }
    var tagMenuOpen by remember { mutableStateOf(false) }

    val bookRecords = remember(state.records, currentBook?.id) {
        state.records.filter { it.record.bookId == currentBook?.id }
    }
    val monthRecords = remember(bookRecords, monthKey) {
        LedgerAnalytics.inMonth(bookRecords, monthKey)
    }
    val totals = remember(monthRecords) { LedgerAnalytics.totals(monthRecords) }
    val visibleRecords = remember(monthRecords, typeFilter, selectedTagIds) {
        monthRecords.filter { item ->
            (typeFilter == null || item.record.type == typeFilter) &&
                (selectedTagIds.isEmpty() || item.tags.any { it.id in selectedTagIds })
        }
    }
    val groups = remember(visibleRecords) {
        visibleRecords.groupBy { it.record.dateEpochDay }.toSortedMap(compareByDescending { it })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("仓颉记账", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable { bookMenuOpen = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currentBook?.name ?: "日常",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = "切换账本",
                        )
                    }
                }
                DropdownMenu(
                    expanded = bookMenuOpen,
                    onDismissRequest = { bookMenuOpen = false },
                ) {
                    activeBooks.forEach { book ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (book.id == currentBook?.id) "${book.name}  ✓" else book.name,
                                )
                            },
                            onClick = {
                                bookMenuOpen = false
                                onSelectBook(book.id)
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("管理账本") },
                        onClick = {
                            bookMenuOpen = false
                            onManageBooks()
                        },
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "搜索账单")
                }
            },
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SectionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        MonthSwitcher(
                            monthKey = monthKey,
                            onPrevious = { monthKey = DateUtils.shiftMonth(monthKey, -1) },
                            onNext = { monthKey = DateUtils.shiftMonth(monthKey, 1) },
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SummaryValue("收入", Money.format(totals.incomeCents), IncomeGreen)
                            SummaryValue("支出", Money.format(totals.expenseCents), ExpenseRed)
                            SummaryValue(
                                "结余",
                                Money.format(totals.balanceCents),
                                MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = typeFilter == null,
                        onClick = { typeFilter = null },
                        label = { Text("全部") },
                    )
                    FilterChip(
                        selected = typeFilter == EntryType.EXPENSE,
                        onClick = { typeFilter = EntryType.EXPENSE },
                        label = { Text("支出") },
                    )
                    FilterChip(
                        selected = typeFilter == EntryType.INCOME,
                        onClick = { typeFilter = EntryType.INCOME },
                        label = { Text("收入") },
                    )
                    if (state.tags.isNotEmpty()) {
                        Row {
                            FilterChip(
                                selected = selectedTagIds.isNotEmpty(),
                                onClick = { tagMenuOpen = true },
                                label = {
                                    Text(
                                        if (selectedTagIds.isEmpty()) "标签"
                                        else "标签 ${selectedTagIds.size}",
                                    )
                                },
                            )
                            DropdownMenu(
                                expanded = tagMenuOpen,
                                onDismissRequest = { tagMenuOpen = false },
                            ) {
                                state.tags.forEach { tag ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (tag.id in selectedTagIds) "${tag.name}  ✓" else tag.name,
                                            )
                                        },
                                        onClick = {
                                            selectedTagIds = if (tag.id in selectedTagIds) {
                                                selectedTagIds - tag.id
                                            } else {
                                                selectedTagIds + tag.id
                                            }
                                        },
                                    )
                                }
                                if (selectedTagIds.isNotEmpty()) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("清除标签筛选") },
                                        onClick = {
                                            selectedTagIds = emptySet()
                                            tagMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (selectedTagIds.isNotEmpty()) {
                        TextButton(onClick = { selectedTagIds = emptySet() }) {
                            Text("清除")
                        }
                    }
                }
            }
            if (visibleRecords.isEmpty()) {
                item {
                    EmptyState(
                        title = "这个月还没有记录",
                        description = "点右下角“记一笔”开始。",
                    )
                }
            }
            groups.forEach { (epochDay, records) ->
                item(key = "day-$epochDay") {
                    DayHeader(
                        epochDay = epochDay,
                        dayExpenseCents = records.filter { it.record.type == EntryType.EXPENSE }
                            .sumOf { it.record.amountCents },
                        dayIncomeCents = records.filter { it.record.type == EntryType.INCOME }
                            .sumOf { it.record.amountCents },
                    )
                }
                items(records, key = { "record-${it.record.id}" }) { item ->
                    RecordRow(item = item, onClick = { onOpenRecord(item.record.id) })
                }
                item(key = "divider-$epochDay") {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    )
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun SummaryValue(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
