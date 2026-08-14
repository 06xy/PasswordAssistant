package com.passwordassistant.app

import android.app.Application
import android.content.Context
import com.passwordassistant.app.data.AppDatabase
import com.passwordassistant.app.data.BackupManager
import com.passwordassistant.app.data.SeedData
import com.passwordassistant.app.data.SettingsRepository
import com.passwordassistant.app.data.SyncRepository
import com.passwordassistant.app.data.VaultManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val settingsRepository = SettingsRepository(context)
    val vaultManager = VaultManager(context, database)
    val backupManager = BackupManager(context, database, settingsRepository, vaultManager)
    val syncRepository = SyncRepository(context, backupManager, vaultManager)
}

class PasswordApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            if (container.database.groupDao().count() == 0) {
                SeedData.defaultGroups().forEach { container.database.groupDao().insert(it) }
            }
        }
    }
}
