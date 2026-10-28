package com.cangjie.ledger.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkerScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<LedgerMaintenanceWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "cangjie-daily-maintenance",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
