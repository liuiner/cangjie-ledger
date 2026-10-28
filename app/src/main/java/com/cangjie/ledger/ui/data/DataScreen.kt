package com.cangjie.ledger.ui.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.AppViewModel
import com.cangjie.ledger.ui.components.SectionCard
import com.cangjie.ledger.ui.theme.ExpenseRed
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::saveBackupFolder) }
    val createCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(viewModel::exportCsv) }
    val openCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importCsv) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入与导出") },
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "自动备份",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    DataRow(
                        icon = Icons.Rounded.Folder,
                        title = "备份目录",
                        subtitle = if (state.settings.backupUri.isBlank()) "未选择目录" else "已授权，可写入",
                        action = "选择",
                        onClick = { folderLauncher.launch(null) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    DataRow(
                        icon = Icons.Rounded.Backup,
                        title = "立即备份",
                        subtitle = state.settings.lastBackupStatus.ifBlank { "备份为 JSON 文件，包含全部账本数据" },
                        action = "执行",
                        onClick = viewModel::backupNow,
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DataIcon(Icons.Rounded.Backup)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        ) {
                            Text("自动备份", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "每天首次启动时检查；当天未备份会自动补备份",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        androidx.compose.material3.Switch(
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
                }
            }

            Text(
                text = "CSV",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    DataRow(
                        icon = Icons.Rounded.Download,
                        title = "导出 CSV",
                        subtitle = "兼容 Excel，金额、分类、账本和标签完整导出",
                        action = "导出",
                        onClick = {
                            createCsvLauncher.launch("cangjie-${LocalDate.now()}.csv")
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    DataRow(
                        icon = Icons.Rounded.Upload,
                        title = "导入 CSV",
                        subtitle = "自动跳过重复记录和格式错误行",
                        action = "导入",
                        onClick = {
                            openCsvLauncher.launch(
                                arrayOf("text/csv", "text/comma-separated-values", "text/plain"),
                            )
                        },
                    )
                }
            }

            Text(
                text = "危险操作",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                DataRow(
                    icon = Icons.Rounded.DeleteForever,
                    title = "清空全部数据",
                    subtitle = "删除账单、账本、预算、标签和周期规则",
                    action = "清空",
                    destructive = true,
                    onClick = { showClearDialog = true },
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空全部数据？") },
            text = { Text("此操作不可恢复。建议先执行一次备份或导出 CSV。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllData()
                    },
                ) { Text("确认清空", color = ExpenseRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DataRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DataIcon(icon, destructive)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) ExpenseRed else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick) {
            Text(action, color = if (destructive) ExpenseRed else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DataIcon(icon: ImageVector, destructive: Boolean = false) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
        color = if (destructive) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(8.dp),
        )
    }
}
