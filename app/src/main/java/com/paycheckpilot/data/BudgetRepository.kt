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
        addDemoFinancialData(DemoFinancialScenario.Default, today, resetExisting = false)
    }

    suspend fun addDemoFinancialData(
        scenario: DemoFinancialScenario = DemoFinancialScenario.Default,
        today: LocalDate = LocalDate.now(),
        resetExisting: Boolean = true,
    ) {
        if (resetExisting) {
            settingsDao.deleteAll()
            billDao.deleteAll()
            paycheckDao.deleteAll()
        }
        val nextPayday = when (scenario) {
            DemoFinancialScenario.NextPayday -> today.plusDays(2)
            DemoFinancialScenario.MissingPaycheck -> today.minusDays(1)
            else -> today.plusDays(7)
        }
        val expectedPaycheck = 70_000L
        val actualNextPaycheck = when (scenario) {
            DemoFinancialScenario.LowerPaycheck -> 61_800L
            DemoFinancialScenario.MissingPaycheck -> null
            else -> expectedPaycheck
        }
        val rentDue = if (scenario == DemoFinancialScenario.BillBeforePayday) today.plusDays(1) else today.plusDays(6)
        saveSettings(
            UserBudgetSettings(
                currentBalanceInCents = if (scenario == DemoFinancialScenario.BillBeforePayday) 92_000 else 184_000,
                safetyBufferInCents = 20_000,
                payFrequency = PayFrequency.Weekly,
                nextPayday = nextPayday,
                estimatedPaycheckInCents = expectedPaycheck,
                hourlyRateInCents = 1_750,
                averageHours = 40.0,
            ),
        )
        listOf(
            Bill(name = "Rent", amountInCents = 85_000, dueDate = rentDue, repeatType = RepeatType.Monthly, category = "Housing"),
            Bill(name = "Phone bill", amountInCents = 8_200, dueDate = today.plusDays(3), repeatType = RepeatType.Monthly, category = "Utilities"),
            Bill(name = "Electric bill", amountInCents = 15_750, dueDate = today.plusDays(5), repeatType = RepeatType.Monthly, category = "Utilities", notes = "Demo variable monthly bill"),
            Bill(name = "Netflix", amountInCents = 1_549, dueDate = today.plusDays(2), repeatType = RepeatType.Monthly, category = "Streaming"),
            Bill(name = "Gym", amountInCents = 2_999, dueDate = today.plusDays(9), repeatType = RepeatType.Monthly, category = "Health"),
            Bill(name = "Gas", amountInCents = 4_600, dueDate = today.plusDays(1), repeatType = RepeatType.Weekly, category = "Transportation", notes = "Demo recurring gas purchase"),
            Bill(name = "Groceries", amountInCents = 9_250, dueDate = today.plusDays(4), repeatType = RepeatType.Weekly, category = "Food", notes = "Demo recurring grocery run"),
            Bill(name = "Spotify duplicate example", amountInCents = 1_199, dueDate = today.plusDays(2), repeatType = RepeatType.None, category = "Needs review", notes = "Demo possible duplicate charge"),
            Bill(name = "Spotify duplicate example", amountInCents = 1_199, dueDate = today.plusDays(3), repeatType = RepeatType.None, category = "Needs review", notes = "Demo possible duplicate charge"),
        ).forEach { saveBill(it) }
        listOfNotNull(
            Paycheck(date = today.minusDays(14), estimatedAmountInCents = 70_000, actualAmountInCents = 70_000, notes = "Demo weekly paycheck from PUBLIX PAYROLL"),
            Paycheck(date = today.minusDays(7), estimatedAmountInCents = 70_000, actualAmountInCents = 70_000, notes = "Demo weekly paycheck from PUBLIX PAYROLL"),
            Paycheck(date = nextPayday, estimatedAmountInCents = expectedPaycheck, actualAmountInCents = actualNextPaycheck, notes = when (scenario) {
                DemoFinancialScenario.LowerPaycheck -> "Demo lower paycheck from PUBLIX PAYROLL"
                DemoFinancialScenario.MissingPaycheck -> "Demo expected paycheck not found yet"
                else -> "Demo upcoming paycheck from PUBLIX PAYROLL"
            }),
            Paycheck(date = today.plusDays(14), estimatedAmountInCents = 142_000, notes = "Demo biweekly paycheck from ACME PAYROLL"),
            Paycheck(date = today.minusDays(2), estimatedAmountInCents = 18_000, actualAmountInCents = 16_300, notes = "Demo variable gig income from DOORDASH"),
        ).forEach { savePaycheck(it) }
    }
}
