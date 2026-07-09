package com.paycheckpilot.data

import android.content.Context
import com.paycheckpilot.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BankSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("paycheck_pilot_bank_settings", Context.MODE_PRIVATE)
    private val backendUrlState = MutableStateFlow(
        prefs.getString(KEY_BACKEND_URL, null)
            ?.takeIf { it.isNotBlank() }
            ?: BuildConfig.PAYCHECK_BACKEND_URL,
    )

    val backendUrl: StateFlow<String> = backendUrlState

    fun currentBackendUrl(): String = backendUrlState.value

    fun updateBackendUrl(value: String) {
        val normalized = value.trim()
        prefs.edit().putString(KEY_BACKEND_URL, normalized).apply()
        backendUrlState.value = normalized
    }

    companion object {
        private const val KEY_BACKEND_URL = "bank_backend_url"
    }
}
