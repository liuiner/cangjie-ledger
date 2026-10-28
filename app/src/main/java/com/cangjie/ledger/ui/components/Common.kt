package com.cangjie.ledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.data.RecordItem
import com.cangjie.ledger.ui.theme.ExpenseRed
import com.cangjie.ledger.ui.theme.IncomeGreen
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.util.Money

@Composable
fun MonthSwitcher(
    monthKey: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    allowFuture: Boolean = false,
    currentMonth: String = DateUtils.monthKey(),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "上个月")
        }
        Text(
            text = DateUtils.monthLabel(monthKey),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        IconButton(onClick = onNext, enabled = allowFuture || monthKey < currentMonth) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "下个月")
        }
    }
}

@Composable
fun CategoryAvatar(
    emoji: String,
    color: Long,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(color).copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun RecordRow(
    item: RecordItem,
    onClick: () -> Unit,
    showBook: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            emoji = item.category?.emoji ?: "🧾",
            color = item.category?.color ?: 0xFF8AA39B,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.category?.name ?: "未分类",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showBook) {
                    Text(
                        text = " · ${item.book?.name.orEmpty()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (item.record.note.isNotBlank()) {
                Text(
                    text = item.record.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.tags.isNotEmpty()) {
                Text(
                    text = item.tags.joinToString("  ") { "#${it.name}" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = (if (item.record.type == EntryType.INCOME) "+" else "-") +
                Money.format(item.record.amountCents),
            style = MaterialTheme.typography.titleMedium,
            color = if (item.record.type == EntryType.INCOME) IncomeGreen else ExpenseRed,
        )
    }
}

@Composable
fun DayHeader(
    epochDay: Long,
    dayExpenseCents: Long,
    dayIncomeCents: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DateUtils.dateLabel(epochDay),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "  ${DateUtils.weekdayLabel(epochDay)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        val text = buildString {
            if (dayIncomeCents > 0) append("收入 ${Money.format(dayIncomeCents)}")
            if (dayIncomeCents > 0 && dayExpenseCents > 0) append(" · ")
            if (dayExpenseCents > 0) append("支出 ${Money.format(dayExpenseCents)}")
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        content = content,
    )
}
