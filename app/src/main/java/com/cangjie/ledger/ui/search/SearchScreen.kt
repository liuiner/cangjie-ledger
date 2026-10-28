package com.cangjie.ledger.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.LedgerAnalytics
import com.cangjie.ledger.ui.components.EmptyState
import com.cangjie.ledger.ui.components.RecordRow
import com.cangjie.ledger.util.DateUtils
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onOpenRecord: (Long) -> Unit,
) {
    var keyword by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryIds by remember { mutableStateOf(setOf<Long>()) }
    var tagIds by remember { mutableStateOf(setOf<Long>()) }
    var startDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var endDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    var tagMenu by remember { mutableStateOf(false) }
    var showDateRange by remember { mutableStateOf(false) }

    val filtered = remember(
        state.records,
        keyword,
        type,
        categoryIds,
        tagIds,
        startDay,
        endDay,
    ) {
        LedgerAnalytics.filterRecords(
            items = state.records,
            keyword = keyword,
            type = type,
            categoryIds = categoryIds,
            tagIds = tagIds,
            startEpochDay = startDay,
            endEpochDay = endDay,
        )
    }
    val hasFilters = keyword.isNotBlank() || type != null || categoryIds.isNotEmpty() ||
        tagIds.isNotEmpty() || startDay != null || endDay != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索账单") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it.take(100) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(onClick = { keyword = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "清除关键词")
                        }
                    }
                },
                label = { Text("搜索备注") },
                placeholder = { Text("输入备注关键词") },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = type == EntryType.EXPENSE,
                    onClick = { type = if (type == EntryType.EXPENSE) null else EntryType.EXPENSE },
                    label = { Text("支出") },
                )
                FilterChip(
                    selected = type == EntryType.INCOME,
                    onClick = { type = if (type == EntryType.INCOME) null else EntryType.INCOME },
                    label = { Text("收入") },
                )
                Box {
                    FilterChip(
                        selected = categoryIds.isNotEmpty(),
                        onClick = { categoryMenu = true },
                        label = {
                            Text(
                                if (categoryIds.isEmpty()) "分类" else "分类 ${categoryIds.size}",
                            )
                        },
                    )
                    DropdownMenu(
                        expanded = categoryMenu,
                        onDismissRequest = { categoryMenu = false },
                    ) {
                        state.categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${category.emoji} ${category.name}" +
                                            if (category.id in categoryIds) "  ✓" else "",
                                    )
                                },
                                onClick = {
                                    categoryIds = if (category.id in categoryIds) {
                                        categoryIds - category.id
                                    } else {
                                        categoryIds + category.id
                                    }
                                },
                            )
                        }
                    }
                }
                Box {
                    FilterChip(
                        selected = tagIds.isNotEmpty(),
                        onClick = { tagMenu = true },
                        label = { Text(if (tagIds.isEmpty()) "标签" else "标签 ${tagIds.size}") },
                    )
                    DropdownMenu(
                        expanded = tagMenu,
                        onDismissRequest = { tagMenu = false },
                    ) {
                        if (state.tags.isEmpty()) {
                            DropdownMenuItem(text = { Text("暂无标签") }, onClick = {})
                        }
                        state.tags.forEach { tag ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "#${tag.name}" + if (tag.id in tagIds) "  ✓" else "",
                                    )
                                },
                                onClick = {
                                    tagIds = if (tag.id in tagIds) {
                                        tagIds - tag.id
                                    } else {
                                        tagIds + tag.id
                                    }
                                },
                            )
                        }
                    }
                }
                FilterChip(
                    selected = startDay != null || endDay != null,
                    onClick = { showDateRange = true },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp),
                        )
                    },
                    label = {
                        Text(
                            when {
                                startDay != null && endDay != null ->
                                    "${DateUtils.dateLabel(startDay!!)}–${DateUtils.dateLabel(endDay!!)}"
                                startDay != null -> "${DateUtils.dateLabel(startDay!!)} 起"
                                endDay != null -> "${DateUtils.dateLabel(endDay!!)} 止"
                                else -> "日期"
                            },
                        )
                    },
                )
                if (hasFilters) {
                    TextButton(
                        onClick = {
                            keyword = ""
                            type = null
                            categoryIds = emptySet()
                            tagIds = emptySet()
                            startDay = null
                            endDay = null
                        },
                    ) { Text("清除") }
                }
            }

            Text(
                text = "找到 ${filtered.size} 条记录",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )
            if (filtered.isEmpty()) {
                EmptyState(
                    title = "没有匹配的账单",
                    description = "试试减少筛选条件或换一个关键词。",
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.record.id }) { item ->
                        Column {
                            RecordRow(
                                item = item,
                                onClick = { onOpenRecord(item.record.id) },
                                showBook = true,
                            )
                            Text(
                                text = DateUtils.dateLabel(item.record.dateEpochDay),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 70.dp, bottom = 4.dp),
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 70.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f),
                            )
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showDateRange) {
        val rangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDay?.let {
                Instant.ofEpochSecond(it * 86_400).toEpochMilli()
            },
            initialSelectedEndDateMillis = endDay?.let {
                Instant.ofEpochSecond(it * 86_400).toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDateRange = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDay = rangeState.selectedStartDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        }
                        endDay = rangeState.selectedEndDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        }
                        showDateRange = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            startDay = null
                            endDay = null
                            showDateRange = false
                        },
                    ) { Text("清除") }
                    TextButton(onClick = { showDateRange = false }) { Text("取消") }
                }
            },
        ) {
            DateRangePicker(state = rangeState)
        }
    }
}
