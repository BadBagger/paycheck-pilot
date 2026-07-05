package com.paycheckpilot.widget

import android.content.Context
import com.paycheckpilot.ui.PaycheckPilotUiState

data class WidgetSnapshot(
    val hasSetup: Boolean,
    val currentBalance: String,
    val nextPayday: String,
    val daysUntilPayday: String,
    val billsBeforePayday: String,
    val leftAfterBills: String,
    val safeToSpend: String,
    val status: String,
    val upcomingBills: String,
)

object WidgetSnapshotStore {
    private const val PREFS = "paycheck_pilot_widgets"
    private const val HAS_SETUP = "has_setup"
    private const val CURRENT_BALANCE = "current_balance"
    private const val NEXT_PAYDAY = "next_payday"
    private const val DAYS_UNTIL_PAYDAY = "days_until_payday"
    private const val BILLS_BEFORE_PAYDAY = "bills_before_payday"
    private const val LEFT_AFTER_BILLS = "left_after_bills"
    private const val SAFE_TO_SPEND = "safe_to_spend"
    private const val STATUS = "status"
    private const val UPCOMING_BILLS = "upcoming_bills"

    fun save(context: Context, state: PaycheckPilotUiState) {
        val settings = state.settings
        val dashboard = state.dashboard
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (settings == null || dashboard == null) {
            prefs.edit()
                .putBoolean(HAS_SETUP, false)
                .putString(STATUS, "Open Paycheck Pilot to finish setup")
                .apply()
            return
        }

        val status = when {
            dashboard.mayRunShort -> "You may run short"
            dashboard.safeToSpendIsLow -> "Safe-to-spend is low"
            else -> "Plan looks okay"
        }
        val billLines = dashboard.upcomingBills.take(3).joinToString("\n") {
            "${it.name}: ${it.amountInCents.moneyLabel()} due ${it.dueDate.monthValue}/${it.dueDate.dayOfMonth}"
        }.ifBlank { "No unpaid bills before payday" }
        val remaining = dashboard.upcomingBills.size - 3
        val upcomingBills = if (remaining > 0) "$billLines\n+$remaining more" else billLines

        prefs.edit()
            .putBoolean(HAS_SETUP, true)
            .putString(CURRENT_BALANCE, settings.currentBalanceInCents.moneyLabel())
            .putString(NEXT_PAYDAY, settings.nextPayday.toString())
            .putString(DAYS_UNTIL_PAYDAY, "${dashboard.daysUntilPayday} days")
            .putString(BILLS_BEFORE_PAYDAY, dashboard.billsDueBeforePaydayInCents.moneyLabel())
            .putString(LEFT_AFTER_BILLS, dashboard.projectedLeftoverInCents.moneyLabel())
            .putString(SAFE_TO_SPEND, dashboard.safeToSpendInCents.moneyLabel())
            .putString(STATUS, status)
            .putString(UPCOMING_BILLS, upcomingBills)
            .apply()
    }

    fun load(context: Context): WidgetSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetSnapshot(
            hasSetup = prefs.getBoolean(HAS_SETUP, false),
            currentBalance = prefs.getString(CURRENT_BALANCE, "--") ?: "--",
            nextPayday = prefs.getString(NEXT_PAYDAY, "--") ?: "--",
            daysUntilPayday = prefs.getString(DAYS_UNTIL_PAYDAY, "--") ?: "--",
            billsBeforePayday = prefs.getString(BILLS_BEFORE_PAYDAY, "--") ?: "--",
            leftAfterBills = prefs.getString(LEFT_AFTER_BILLS, "--") ?: "--",
            safeToSpend = prefs.getString(SAFE_TO_SPEND, "--") ?: "--",
            status = prefs.getString(STATUS, "Open Paycheck Pilot to finish setup") ?: "Open Paycheck Pilot to finish setup",
            upcomingBills = prefs.getString(UPCOMING_BILLS, "No bills loaded yet") ?: "No bills loaded yet",
        )
    }

    private fun Long.moneyLabel(): String = com.paycheckpilot.domain.BudgetCalculator.formatMoney(this)
}
