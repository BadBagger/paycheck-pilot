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
    private val mockPremiumState = MutableStateFlow(prefs.getBoolean(KEY_MOCK_PREMIUM, false))

    val backendUrl: StateFlow<String> = backendUrlState
    val mockPremiumEnabled: StateFlow<Boolean> = mockPremiumState

    fun currentBackendUrl(): String = backendUrlState.value
    fun hasMockPremium(): Boolean = mockPremiumState.value

    fun updateBackendUrl(value: String) {
        val normalized = value.trim()
        prefs.edit().putString(KEY_BACKEND_URL, normalized).apply()
        backendUrlState.value = normalized
    }

    fun updateMockPremium(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MOCK_PREMIUM, enabled).apply()
        mockPremiumState.value = enabled
    }

    companion object {
        private const val KEY_BACKEND_URL = "bank_backend_url"
        private const val KEY_MOCK_PREMIUM = "mock_premium_enabled"
    }
}
