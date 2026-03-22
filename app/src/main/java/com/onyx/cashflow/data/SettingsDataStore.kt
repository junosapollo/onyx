package com.onyx.cashflow.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val SMS_ALERTS_ENABLED = booleanPreferencesKey("sms_alerts_enabled")
        private val SHOW_EARNED_DATA = booleanPreferencesKey("show_earned_data")
    }

    val smsAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SMS_ALERTS_ENABLED] ?: true
    }

    val showEarnedData: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_EARNED_DATA] ?: true
    }

    suspend fun setSmsAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SMS_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setShowEarnedData(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHOW_EARNED_DATA] = enabled
        }
    }
}
