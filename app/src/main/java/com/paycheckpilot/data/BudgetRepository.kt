package com.paycheckpilot.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class BudgetRepository(
    private val settingsDao: UserBudgetSettingsDao,
    private val billDao: BillDao,
    private val paycheckDao: PaycheckDao,
) {
    val settings: Flow<UserBudgetSettings?> = settingsDao.observeSettings()
    val bills: Flow<List<Bill>> = billDao.observeBills()
    val paychecks: Flow<List<Paycheck>> = paycheckDao.observePaychecks()

    fun nextPaycheck(today: LocalDate): Flow<Paycheck?> = paycheckDao.observeNextPaycheck(today)

    suspend fun saveSettings(settings: UserBudgetSettings) = settingsDao.upsert(settings)
    suspend fun saveBill(bill: Bill) = billDao.upsert(bill)
    suspend fun deleteBill(bill: Bill) = billDao.delete(bill)
    suspend fun setBillPaid(id: Long, isPaid: Boolean) = billDao.setPaid(id, isPaid)
    suspend fun savePaycheck(paycheck: Paycheck) = paycheckDao.upsert(paycheck)
    suspend fun deletePaycheck(paycheck: Paycheck) = paycheckDao.delete(paycheck)

    suspend fun addSampleData(today: LocalDate = LocalDate.now()) {
        saveSettings(
            UserBudgetSettings(
                currentBalanceInCents = 124_500,
                safetyBufferInCents = 20_000,
                payFrequency = PayFrequency.Biweekly,
                nextPayday = today.plusDays(9),
                estimatedPaycheckInCents = 165_000,
                hourlyRateInCents = 2_250,
                averageHours = 72.0,
            ),
        )
        listOf(
            Bill(name = "Rent", amountInCents = 82_500, dueDate = today.plusDays(3), repeatType = RepeatType.Monthly, category = "Housing"),
            Bill(name = "Phone", amountInCents = 7_800, dueDate = today.plusDays(5), repeatType = RepeatType.Monthly, category = "Utilities"),
            Bill(name = "Car insurance", amountInCents = 12_400, dueDate = today.plusDays(8), repeatType = RepeatType.Monthly, category = "Transportation"),
            Bill(name = "Streaming", amountInCents = 1_599, dueDate = today.plusDays(14), repeatType = RepeatType.Monthly, category = "Subscriptions"),
        ).forEach { saveBill(it) }
        savePaycheck(
            Paycheck(
                date = today.plusDays(9),
                estimatedAmountInCents = 165_000,
                notes = "Sample upcoming paycheck",
            ),
        )
    }
}
