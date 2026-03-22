package com.onyx.cashflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onyx.cashflow.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsDataStore = SettingsDataStore(app)

    val smsAlertsEnabled: StateFlow<Boolean> = settingsDataStore.smsAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showEarnedData: StateFlow<Boolean> = settingsDataStore.showEarnedData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleSmsAlerts(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSmsAlertsEnabled(enabled)
        }
    }

    fun toggleShowEarnedData(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowEarnedData(enabled)
        }
    }
}
