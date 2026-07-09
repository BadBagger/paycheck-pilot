package com.paycheckpilot.data

import android.content.Context
import java.util.UUID

object AppInstallId {
    private const val PREFS = "paycheck_pilot_install"
    private const val KEY_ID = "install_id"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_ID, null)?.let { return it }
        val value = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_ID, value).apply()
        return value
    }
}
