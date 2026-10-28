package com.cangjie.ledger.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.AppViewModel
import com.cangjie.ledger.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onOpenBooks: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenData: () -> Unit,
) {
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::saveBackupFolder) }
    val activeBooks = state.books.filterNot { it.archived }
    var bookMenuOpen by remember { mutableStateOf(false) }
    val currentBook = activeBooks.firstOrNull { it.id == state.settings.defaultBookId }
        ?: activeBooks.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("设置") })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SettingsSectionTitle("外观")
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingIconTitle(Icons.Rounded.Palette, "主题")
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "system" to "跟随系统",
                            "light" to "浅色",
                            "dark" to "深色",
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = state.settings.themeMode == value,
                                onClick = { viewModel.setThemeMode(value) },
                                label = { Text(label) },
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("动态取色", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Android 12 及以上用壁纸颜色",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.settings.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                        )
                    }
                }
            }

            SettingsSectionTitle("记账")
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Box {
                        SettingsRow(
                            icon = Icons.Rounded.Dataset,
                            title = "默认账本",
                            subtitle = currentBook?.name ?: "日常",
                            onClick = { bookMenuOpen = true },
                        )
                        DropdownMenu(
                            expanded = bookMenuOpen,
                            onDismissRequest = { bookMenuOpen = false },
                        ) {
                            activeBooks.forEach { book ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            book.name + if (book.id == currentBook?.id) "  ✓" else "",
                                        )
                                    },
                                    onClick = {
                                        bookMenuOpen = false
                                        viewModel.setDefaultBook(book.id)
                                    },
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Folder,
                        title = "账本管理",
                        subtitle = "${activeBooks.size} 个使用中",
                        onClick = onOpenBooks,
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Repeat,
                        title = "周期记账",
                        subtitle = "${state.recurringRules.count { it.enabled }} 条规则已启用",
                        onClick = onOpenRecurring,
                    )
                }
            }

            SettingsSectionTitle("备份与数据")
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SettingIcon(Icons.Rounded.Cloud)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        ) {
                            Text("自动备份", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = state.settings.lastBackupStatus.ifBlank {
                                    if (state.settings.backupUri.isBlank()) {
                                        "选择目录后可每日自动备份"
                                    } else {
                                        "启动时会检查并补齐当天备份"
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.settings.autoBackup,
                            onCheckedChange = { checked ->
                                if (checked && state.settings.backupUri.isBlank()) {
                                    folderLauncher.launch(null)
                                } else {
                                    viewModel.setAutoBackup(checked)
                                }
                            },
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Folder,
                        title = "备份目录",
                        subtitle = if (state.settings.backupUri.isBlank()) "未选择" else "已授权",
                        onClick = { folderLauncher.launch(null) },
                        trailingText = "选择",
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Backup,
                        title = "立即备份",
                        subtitle = state.settings.lastBackupDate.ifBlank { "尚无备份" },
                        onClick = viewModel::backupNow,
                        trailingText = "执行",
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Dataset,
                        title = "导入、导出与清空",
                        subtitle = "CSV 导入导出、备份和危险操作",
                        onClick = onOpenData,
                    )
                }
            }

            SettingsSectionTitle("关于")
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "仓颉记账 1.0.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "本地优先 · 无账号 · 单用户",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingIconTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingIcon(icon)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailingText != null) {
            TextButton(onClick = onClick) { Text(trailingText) }
        } else {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
