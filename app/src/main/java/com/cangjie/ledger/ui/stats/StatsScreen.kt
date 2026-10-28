package com.cangjie.ledger.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cangjie.ledger.data.CategoryAmount
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.ui.AppUiState
import com.cangjie.ledger.ui.LedgerAnalytics
import com.cangjie.ledger.ui.components.EmptyState
import com.cangjie.ledger.ui.components.MonthSwitcher
import com.cangjie.ledger.ui.components.SectionCard
import com.cangjie.ledger.ui.theme.ExpenseRed
import com.cangjie.ledger.ui.theme.IncomeGreen
import com.cangjie.ledger.ui.theme.MutedInk
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.util.Money
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: AppUiState,
    onOpenRecord: (Long) -> Unit,
) {
    val activeBooks = state.books.filterNot { it.archived }
    val bookId = activeBooks.firstOrNull { it.id == state.settings.defaultBookId }?.id
        ?: activeBooks.firstOrNull()?.id
    var monthKey by rememberSaveable { mutableStateOf(DateUtils.monthKey()) }
    val bookRecords = remember(state.records, bookId) {
        state.records.filter { it.record.bookId == bookId }
    }
    val monthRecords = remember(bookRecords, monthKey) {
        LedgerAnalytics.inMonth(bookRecords, monthKey)
    }
    val totals = remember(monthRecords) { LedgerAnalytics.totals(monthRecords) }
    val categoryExpenses = remember(monthRecords) {
        LedgerAnalytics.categoryExpenses(monthRecords)
    }
    val trend = remember(bookRecords, monthKey) {
        LedgerAnalytics.sixMonthTrend(bookRecords, monthKey)
    }
    val maxCategory = categoryExpenses.maxOfOrNull { it.amountCents } ?: 1L

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("统计") })
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatSummary(
                    label = "收入",
                    value = Money.format(totals.incomeCents),
                    color = IncomeGreen,
                    modifier = Modifier.weight(1f),
                )
                StatSummary(
                    label = "支出",
                    value = Money.format(totals.expenseCents),
                    color = ExpenseRed,
                    modifier = Modifier.weight(1f),
                )
                StatSummary(
                    label = "结余",
                    value = Money.format(totals.balanceCents),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionTitle("本月支出构成")
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                if (categoryExpenses.isEmpty()) {
                    EmptyState(
                        title = "还没有支出",
                        description = "记录支出后，这里会显示分类占比。",
                    )
                } else {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DonutChart(
                            data = categoryExpenses,
                            totalCents = totals.expenseCents,
                            modifier = Modifier.size(150.dp),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            categoryExpenses.take(5).forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(9.dp)
                                            .background(Color(item.category.color), CircleShape),
                                    )
                                    Text(
                                        text = "  ${item.category.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "${(item.amountCents * 100.0 / max(1L, totals.expenseCents)).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SectionTitle("近 6 个月收支趋势")
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegendDot("收入", IncomeGreen)
                        LegendDot("支出", ExpenseRed)
                    }
                    Spacer(Modifier.height(14.dp))
                    TrendBars(
                        income = trend.map { it.incomeCents },
                        expense = trend.map { it.expenseCents },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        trend.forEach { month ->
                            Text(
                                text = DateUtils.shortMonthLabel(month.monthKey),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }

            SectionTitle("分类支出排行")
            if (categoryExpenses.isEmpty()) {
                EmptyState(
                    title = "暂无排行",
                    description = "本月还没有支出记录。",
                )
            } else {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        categoryExpenses.forEachIndexed { index, item ->
                            RankingRow(
                                rank = index + 1,
                                item = item,
                                maxAmount = maxCategory,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatSummary(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun DonutChart(
    data: List<CategoryAmount>,
    totalCents: Long,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.18f
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var start = -90f
            data.forEach { item ->
                val sweep = if (totalCents <= 0L) 0f else {
                    item.amountCents.toFloat() / totalCents * 360f
                }
                drawArc(
                    color = Color(item.category.color),
                    startAngle = start,
                    sweepAngle = (sweep - 1.2f).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                start += sweep
            }
            drawCircle(
                color = surface,
                radius = diameter / 2f - stroke / 2f,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "本月支出",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = Money.format(totalCents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TrendBars(
    income: List<Long>,
    expense: List<Long>,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    Canvas(modifier = modifier) {
        val maxValue = max(1L, (income + expense).maxOrNull() ?: 1L)
        val groupWidth = size.width / income.size.coerceAtLeast(1)
        val barWidth = (groupWidth * 0.2f).coerceAtMost(18.dp.toPx())
        repeat(4) { line ->
            val y = size.height * line / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        income.indices.forEach { index ->
            val center = groupWidth * index + groupWidth / 2f
            val inHeight = size.height * (income.getOrElse(index) { 0L }.toFloat() / maxValue)
            val exHeight = size.height * (expense.getOrElse(index) { 0L }.toFloat() / maxValue)
            drawRoundRect(
                color = IncomeGreen,
                topLeft = Offset(center - barWidth - 2.dp.toPx(), size.height - inHeight),
                size = Size(barWidth, inHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
            drawRoundRect(
                color = ExpenseRed,
                topLeft = Offset(center + 2.dp.toPx(), size.height - exHeight),
                size = Size(barWidth, exHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            text = "  $label",
            style = MaterialTheme.typography.labelMedium,
            color = MutedInk,
        )
    }
}

@Composable
private fun RankingRow(
    rank: Int,
    item: CategoryAmount,
    maxAmount: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Text(text = item.category.emoji, style = MaterialTheme.typography.titleLarge)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Row {
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Money.format(item.amountCents),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { (item.amountCents.toFloat() / maxAmount).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = Color(item.category.color),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
