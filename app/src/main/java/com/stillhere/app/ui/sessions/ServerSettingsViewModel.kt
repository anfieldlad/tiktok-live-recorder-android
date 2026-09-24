package com.stillhere.app.ui.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillhere.app.StillHereApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The backend URL and API key, as the Server section of the sheet edits them. */
class ServerSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings get() = getApplication<StillHereApp>().flavor.settings

    val baseUrl: StateFlow<String> =
        settings.baseUrlFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val apiKey: StateFlow<String> =
        settings.apiKeyFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun save(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            settings.setBaseUrl(baseUrl)
            settings.setApiKey(apiKey)
        }
    }
}
