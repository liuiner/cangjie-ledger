package com.cangjie.ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cangjie.ledger.LedgerApplication
import com.cangjie.ledger.MainActivity
import com.cangjie.ledger.R
import com.cangjie.ledger.data.EntryType
import com.cangjie.ledger.util.DateUtils
import com.cangjie.ledger.util.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun requestUpdate(context: Context) {
        val app = context.applicationContext as? LedgerApplication ?: return
        app.appScope.launch {
            runCatching { updateNow(context) }
        }
    }

    suspend fun updateNow(context: Context) {
        val app = context.applicationContext as? LedgerApplication ?: return
        app.repository.initialize()
        val settings = app.repository.settings.first()
        val bookId = app.repository.ensureValidDefaultBook(settings.defaultBookId)
        val records = app.repository.records.first().filter { it.bookId == bookId }
        val todayEpoch = DateUtils.todayEpochDay()
        val monthKey = DateUtils.monthKey()
        val todaySpending = records
            .filter { it.type == EntryType.EXPENSE && it.dateEpochDay == todayEpoch }
            .sumOf { it.amountCents }
        val monthRecords = records.filter { DateUtils.monthKey(it.dateEpochDay) == monthKey }
        val income = monthRecords.filter { it.type == EntryType.INCOME }.sumOf { it.amountCents }
        val expense = monthRecords.filter { it.type == EntryType.EXPENSE }.sumOf { it.amountCents }

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, LedgerWidgetProvider::class.java))
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_ledger).apply {
                setTextViewText(R.id.widget_today, Money.format(todaySpending))
                setTextViewText(R.id.widget_balance, Money.format(income - expense))
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        id,
                        Intent(context, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_OPEN_ADD, true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
            manager.updateAppWidget(id, views)
        }
    }
}

