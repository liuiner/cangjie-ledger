package com.cangjie.ledger.ui.books

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.data.BookEntity
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.AppViewModel
import com.cangjie.ledger.ui.components.EmptyState
import com.cangjie.ledger.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    var editingBook by remember { mutableStateOf<BookEntity?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    val active = state.books.filterNot { it.archived }
    val archived = state.books.filter { it.archived }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账本管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("新建账本") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Text(
                    text = "使用中",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
            }
            if (active.isEmpty()) {
                item {
                    EmptyState(
                        title = "没有可用账本",
                        description = "新建一个账本继续记账。",
                    )
                }
            } else {
                item {
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            active.forEachIndexed { index, book ->
                                BookRow(
                                    book = book,
                                    isDefault = book.id == state.settings.defaultBookId,
                                    onEdit = { editingBook = book },
                                    onDefault = { viewModel.setDefaultBook(book.id) },
                                    onArchive = { viewModel.setBookArchived(book.id, true) },
                                )
                                if (index != active.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (archived.isNotEmpty()) {
                item {
                    Text(
                        text = "已归档",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
                    )
                }
                item {
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            archived.forEachIndexed { index, book ->
                                BookRow(
                                    book = book,
                                    isDefault = false,
                                    onEdit = { editingBook = book },
                                    onDefault = null,
                                    onArchive = { viewModel.setBookArchived(book.id, false) },
                                )
                                if (index != archived.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }

    if (showNewDialog) {
        BookDialog(
            book = null,
            onDismiss = { showNewDialog = false },
            onSave = { name ->
                viewModel.saveBook(BookEntity(name = name)) {
                    showNewDialog = false
                }
            },
        )
    }
    editingBook?.let { book ->
        BookDialog(
            book = book,
            onDismiss = { editingBook = null },
            onSave = { name ->
                viewModel.saveBook(book.copy(name = name)) {
                    editingBook = null
                }
            },
        )
    }
}

@Composable
private fun BookRow(
    book: BookEntity,
    isDefault: Boolean,
    onEdit: () -> Unit,
    onDefault: (() -> Unit)?,
    onArchive: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (isDefault) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = book.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        if (isDefault) {
            Text(
                text = "默认",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "账本操作")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("重命名") },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
            if (onDefault != null && !isDefault) {
                DropdownMenuItem(
                    text = { Text("设为默认") },
                    onClick = {
                        menuOpen = false
                        onDefault()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(if (book.archived) "恢复账本" else "归档账本") },
                onClick = {
                    menuOpen = false
                    onArchive()
                },
            )
        }
    }
}

@Composable
private fun BookDialog(
    book: BookEntity?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable(book?.id) { mutableStateOf(book?.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (book == null) "新建账本" else "账本名称") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(16) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("名称") },
                placeholder = { Text("例如：旅行、装修") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
