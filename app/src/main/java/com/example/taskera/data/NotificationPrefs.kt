// app/src/main/java/com/example/taskera/data/NotificationPrefs.kt
package com.example.taskera.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ─── ➊ Public extension on Context for your Preferences DataStore ─────────────────────────
val Context.dataStore:
        androidx.datastore.core.DataStore<Preferences>
        by preferencesDataStore(name = "settings")

object NotificationPrefs {
    val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val DEFAULT_LEAD_MIN  = intPreferencesKey("default_lead_minutes")

    // ─── ➋ Flows for reading ─────────────────────────────────────────────────────────────────
    fun isEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[REMINDERS_ENABLED] ?: true }

    fun defaultLeadMin(context: Context): Flow<Int> =
        context.dataStore.data.map { it[DEFAULT_LEAD_MIN] ?: 30 }
}
