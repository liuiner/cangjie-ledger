package com.cangjie.ledger.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: String = "system",
    val dynamicColor: Boolean = true,
    val defaultBookId: Long = 0,
    val autoBackup: Boolean = false,
    val backupUri: String = "",
    val lastBackupDate: String = "",
    val lastBackupStatus: String = "",
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val defaultBookId = longPreferencesKey("default_book_id")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val backupUri = stringPreferencesKey("backup_uri")
        val lastBackupDate = stringPreferencesKey("last_backup_date")
        val lastBackupStatus = stringPreferencesKey("last_backup_status")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.themeMode] ?: "system",
            dynamicColor = prefs[Keys.dynamicColor] ?: true,
            defaultBookId = prefs[Keys.defaultBookId] ?: 0,
            autoBackup = prefs[Keys.autoBackup] ?: false,
            backupUri = prefs[Keys.backupUri] ?: "",
            lastBackupDate = prefs[Keys.lastBackupDate] ?: "",
            lastBackupStatus = prefs[Keys.lastBackupStatus] ?: "",
        )
    }

    suspend fun setThemeMode(value: String) = context.dataStore.edit { it[Keys.themeMode] = value }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.dynamicColor] = value }
    suspend fun setDefaultBookId(value: Long) = context.dataStore.edit { it[Keys.defaultBookId] = value }
    suspend fun setAutoBackup(value: Boolean) = context.dataStore.edit { it[Keys.autoBackup] = value }
    suspend fun setBackupUri(value: String) = context.dataStore.edit { it[Keys.backupUri] = value }
    suspend fun setBackupResult(date: String, status: String) = context.dataStore.edit {
        it[Keys.lastBackupDate] = date
        it[Keys.lastBackupStatus] = status
    }
    suspend fun setBackupStatus(status: String) = context.dataStore.edit {
        it[Keys.lastBackupStatus] = status
    }
}

