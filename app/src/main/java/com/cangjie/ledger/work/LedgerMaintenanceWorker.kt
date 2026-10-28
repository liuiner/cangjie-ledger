package com.cangjie.ledger.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cangjie.ledger.LedgerApplication
import com.cangjie.ledger.widget.WidgetUpdater

class LedgerMaintenanceWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LedgerApplication
        return runCatching {
            app.repository.initialize()
            app.repository.generateDueRecurring()
            app.backupManager.backupIfDue().getOrThrow()
            WidgetUpdater.updateNow(applicationContext)
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
