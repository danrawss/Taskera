package com.example.taskera.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.taskera.data.NotificationPrefs
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch
import com.example.taskera.data.dataStore

class SettingsViewModel(private val context: Context) : ViewModel() {
    // LiveData for UI
    val remindersEnabled = NotificationPrefs.isEnabled(context).asLiveData()
    val defaultLeadMin   = NotificationPrefs.defaultLeadMin(context).asLiveData()

    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[NotificationPrefs.REMINDERS_ENABLED] = enabled
        }
    }

    fun setDefaultLeadMin(min: Int) = viewModelScope.launch {
        context.dataStore.edit { prefs: MutablePreferences ->
            prefs[NotificationPrefs.DEFAULT_LEAD_MIN] = min
        }
    }
}
