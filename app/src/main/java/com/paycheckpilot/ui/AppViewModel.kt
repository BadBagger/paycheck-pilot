package com.paycheckpilot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paycheckpilot.data.AppDatabase
import com.paycheckpilot.data.Bill
import com.paycheckpilot.data.BudgetRepository
import com.paycheckpilot.data.PayFrequency
import com.paycheckpilot.data.Paycheck
import com.paycheckpilot.data.RepeatType
import com.paycheckpilot.data.UserBudgetSettings
import com.paycheckpilot.domain.BudgetCalculator
import com.paycheckpilot.domain.DashboardProjection
import com.paycheckpilot.domain.TimelineEvent
import com.paycheckpilot.widget.WidgetSnapshotStore
import com.paycheckpilot.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PaycheckPilotUiState(
    val settings: UserBudgetSettings? = null,
    val bills: List<Bill> = emptyList(),
    val paychecks: List<Paycheck> = emptyList(),
    val dashboard: DashboardProjection? = null,
    val timeline: List<TimelineEvent> = emptyList(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BudgetRepository

    val uiState: StateFlow<PaycheckPilotUiState>

    init {
        val db = AppDatabase.get(application)
        repository = BudgetRepository(db.settingsDao(), db.billDao(), db.paycheckDao())
        uiState = combine(repository.settings, repository.bills, repository.paychecks) { settings, bills, paychecks ->
            val today = LocalDate.now()
            val nextPaycheck = paychecks.firstOrNull { !it.date.isBefore(today) }
            PaycheckPilotUiState(
                settings = settings,
                bills = bills,
                paychecks = paychecks,
                dashboard = settings?.let { BudgetCalculator.dashboard(it, bills, today) },
                timeline = settings?.let { BudgetCalculator.timeline(it, bills, nextPaycheck, today) }.orEmpty(),
            )
        }.onEach { state ->
            WidgetSnapshotStore.save(application, state)
            WidgetUpdater.updateAll(application)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaycheckPilotUiState())
    }

    fun saveSettings(
        currentBalance: Long,
        safetyBuffer: Long,
        payFrequency: PayFrequency,
        nextPayday: LocalDate,
        estimatedPaycheck: Long,
        hourlyRate: Long?,
        averageHours: Double?,
    ) = viewModelScope.launch {
        repository.saveSettings(
            UserBudgetSettings(
                currentBalanceInCents = currentBalance,
                safetyBufferInCents = safetyBuffer,
                payFrequency = payFrequency,
                nextPayday = nextPayday,
                estimatedPaycheckInCents = estimatedPaycheck,
                hourlyRateInCents = hourlyRate,
                averageHours = averageHours,
            ),
        )
        repository.savePaycheck(
            Paycheck(date = nextPayday, estimatedAmountInCents = estimatedPaycheck),
        )
    }

    fun saveBill(
        existing: Bill?,
        name: String,
        amount: Long,
        dueDate: LocalDate,
        repeatType: RepeatType,
        category: String,
        notes: String?,
    ) = viewModelScope.launch {
        repository.saveBill(
            Bill(
                id = existing?.id ?: 0,
                name = name.ifBlank { "Bill" },
                amountInCents = amount,
                dueDate = dueDate,
                repeatType = repeatType,
                category = category.ifBlank { "General" },
                isPaid = existing?.isPaid ?: false,
                notes = notes?.ifBlank { null },
            ),
        )
    }

    fun deleteBill(bill: Bill) = viewModelScope.launch { repository.deleteBill(bill) }
    fun setBillPaid(id: Long, isPaid: Boolean) = viewModelScope.launch { repository.setBillPaid(id, isPaid) }

    fun savePaycheck(
        existing: Paycheck?,
        date: LocalDate,
        estimatedAmount: Long,
        actualAmount: Long?,
        hoursWorked: Double?,
        notes: String?,
    ) = viewModelScope.launch {
        repository.savePaycheck(
            Paycheck(
                id = existing?.id ?: 0,
                date = date,
                estimatedAmountInCents = estimatedAmount,
                actualAmountInCents = actualAmount,
                hoursWorked = hoursWorked,
                notes = notes?.ifBlank { null },
            ),
        )
    }

    fun deletePaycheck(paycheck: Paycheck) = viewModelScope.launch { repository.deletePaycheck(paycheck) }

    fun addSampleData() = viewModelScope.launch { repository.addSampleData() }

    fun applyEarlyBillPayment(bill: Bill) = viewModelScope.launch {
        repository.setBillPaid(bill.id, true)
    }
}
