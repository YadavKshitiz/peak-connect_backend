package com.peakconnect.app.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationPreferences {
    val masterEnabled: Flow<Boolean>
    val remindersEnabled: Flow<Boolean>
    val lastSyncTimestamp: Flow<Long?>

    suspend fun setMasterEnabled(enabled: Boolean)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setLastSyncTimestamp(timestamp: Long)
}

@Singleton
class DataStoreNotificationPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : NotificationPreferences {

    companion object {
        val KEY_MASTER_ENABLED = booleanPreferencesKey("notif_master_enabled")
        val KEY_REMINDERS_ENABLED = booleanPreferencesKey("notif_reminders_enabled")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("notif_last_sync_timestamp")
    }

    override val masterEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_MASTER_ENABLED] ?: true
    }

    override val remindersEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_REMINDERS_ENABLED] ?: true
    }

    override val lastSyncTimestamp: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIMESTAMP]
    }

    override suspend fun setMasterEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_MASTER_ENABLED] = enabled
        }
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_REMINDERS_ENABLED] = enabled
        }
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }
}
