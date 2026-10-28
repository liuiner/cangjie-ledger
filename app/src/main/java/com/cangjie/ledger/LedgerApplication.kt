package com.cangjie.ledger

import android.app.Application
import androidx.work.Configuration
import com.cangjie.ledger.backup.BackupManager
import com.cangjie.ledger.data.LedgerDatabase
import com.cangjie.ledger.data.LedgerRepository
import com.cangjie.ledger.data.PreferencesRepository
import com.cangjie.ledger.widget.WidgetUpdater
import com.cangjie.ledger.work.WorkerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LedgerApplication : Application(), Configuration.Provider {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { LedgerDatabase.get(this) }
    val preferences by lazy { PreferencesRepository(this) }
    val repository by lazy { LedgerRepository(this, database, preferences) }
    val backupManager by lazy { BackupManager(this, repository) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        WorkerScheduler.schedule(this)
        appScope.launch {
            repository.initialize()
            runCatching { repository.generateDueRecurring() }
            runCatching { backupManager.backupIfDue() }
            WidgetUpdater.updateNow(this@LedgerApplication)
        }
    }
}
