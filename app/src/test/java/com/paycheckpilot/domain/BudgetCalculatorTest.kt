package com.paycheckpilot.domain

import com.paycheckpilot.data.Bill
import com.paycheckpilot.data.PayFrequency
import com.paycheckpilot.data.RepeatType
import com.paycheckpilot.data.UserBudgetSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetCalculatorTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 4)
    private val settings = UserBudgetSettings(
        currentBalanceInCents = 100_000,
        safetyBufferInCents = 20_000,
        payFrequency = PayFrequency.Biweekly,
        nextPayday = today.plusDays(7),
        estimatedPaycheckInCents = 150_000,
    )

    @Test
    fun dashboardCountsOnlyUnpaidBillsDueBeforePayday() {
        val bills = listOf(
            bill("Rent", 60_000, today.plusDays(2)),
            bill("Paid phone", 8_000, today.plusDays(3), isPaid = true),
            bill("After payday", 30_000, today.plusDays(9)),
        )

        val result = BudgetCalculator.dashboard(settings, bills, today)

        assertEquals(60_000, result.billsDueBeforePaydayInCents)
        assertEquals(40_000, result.projectedLeftoverInCents)
        assertEquals(20_000, result.safeToSpendInCents)
        assertFalse(result.mayRunShort)
    }

    @Test
    fun dashboardWarnsWhenProjectedLeftoverIsNegative() {
        val result = BudgetCalculator.dashboard(settings, listOf(bill("Rent", 125_000, today.plusDays(2))), today)

        assertTrue(result.mayRunShort)
        assertEquals(0, result.safeToSpendInCents)
    }

    @Test
    fun timelineShowsBelowBufferAndBelowZeroStates() {
        val result = BudgetCalculator.timeline(
            settings = settings.copy(currentBalanceInCents = 50_000),
            bills = listOf(bill("Insurance", 35_000, today.plusDays(1)), bill("Rent", 25_000, today.plusDays(2))),
            today = today,
        )

        assertTrue(result[0].belowSafetyBuffer)
        assertTrue(result[1].belowZero)
    }

    @Test
    fun whatIfDoesNotSaveButReflectsSimulation() {
        val bills = listOf(bill("Rent", 70_000, today.plusDays(2), id = 12))

        val result = BudgetCalculator.whatIf(
            settings = settings,
            bills = bills,
            purchaseAmountInCents = 10_000,
            simulatedPaycheckInCents = 120_000,
            billPaidEarlyId = 12,
            today = today,
        )

        assertEquals(60_000, result.projectedLeftoverInCents)
        assertTrue(result.isSafe)
    }

    private fun bill(
        name: String,
        amount: Long,
        dueDate: LocalDate,
        isPaid: Boolean = false,
        id: Long = 0,
    ) = Bill(
        id = id,
        name = name,
        amountInCents = amount,
        dueDate = dueDate,
        repeatType = RepeatType.Monthly,
        category = "General",
        isPaid = isPaid,
    )
}
