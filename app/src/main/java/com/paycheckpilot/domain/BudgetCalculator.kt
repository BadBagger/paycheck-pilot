package com.paycheckpilot.domain

import com.paycheckpilot.data.Bill
import com.paycheckpilot.data.Paycheck
import com.paycheckpilot.data.RepeatType
import com.paycheckpilot.data.UserBudgetSettings
import java.text.NumberFormat
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

data class DashboardProjection(
    val daysUntilPayday: Long,
    val billsDueBeforePaydayInCents: Long,
    val projectedLeftoverInCents: Long,
    val safeToSpendInCents: Long,
    val mayRunShort: Boolean,
    val safeToSpendIsLow: Boolean,
    val upcomingBills: List<Bill>,
)

data class TimelineEvent(
    val date: LocalDate,
    val title: String,
    val amountInCents: Long,
    val runningBalanceInCents: Long,
    val belowZero: Boolean,
    val belowSafetyBuffer: Boolean,
)

data class WhatIfResult(
    val isSafe: Boolean,
    val projectedLeftoverInCents: Long,
    val safeToSpendInCents: Long,
)

object BudgetCalculator {
    fun dashboard(
        settings: UserBudgetSettings,
        bills: List<Bill>,
        today: LocalDate = LocalDate.now(),
    ): DashboardProjection {
        val upcomingBills = billsDueBeforePayday(bills, today, settings.nextPayday)
        val due = upcomingBills.sumOf { it.amountInCents }
        val leftover = settings.currentBalanceInCents - due
        val safe = leftover - settings.safetyBufferInCents
        return DashboardProjection(
            daysUntilPayday = max(0, ChronoUnit.DAYS.between(today, settings.nextPayday)),
            billsDueBeforePaydayInCents = due,
            projectedLeftoverInCents = leftover,
            safeToSpendInCents = max(0, safe),
            mayRunShort = leftover < 0,
            safeToSpendIsLow = safe in 0..5_000,
            upcomingBills = upcomingBills,
        )
    }

    fun timeline(
        settings: UserBudgetSettings,
        bills: List<Bill>,
        paycheck: Paycheck? = null,
        today: LocalDate = LocalDate.now(),
    ): List<TimelineEvent> {
        val billEvents = billsDueBeforePayday(bills, today, settings.nextPayday)
            .filterNot { it.isPaid }
            .map { RawEvent(it.dueDate, it.name, -it.amountInCents) }
        val payAmount = paycheck?.actualAmountInCents ?: paycheck?.estimatedAmountInCents ?: settings.estimatedPaycheckInCents
        val payEvent = RawEvent(settings.nextPayday, "Paycheck", payAmount)
        var running = settings.currentBalanceInCents
        return (billEvents + payEvent)
            .sortedWith(compareBy<RawEvent> { it.date }.thenBy { it.amount > 0 })
            .map { event ->
                running += event.amount
                TimelineEvent(
                    date = event.date,
                    title = event.title,
                    amountInCents = event.amount,
                    runningBalanceInCents = running,
                    belowZero = running < 0,
                    belowSafetyBuffer = running in 0 until settings.safetyBufferInCents,
                )
            }
    }

    fun whatIf(
        settings: UserBudgetSettings,
        bills: List<Bill>,
        purchaseAmountInCents: Long,
        simulatedPaycheckInCents: Long? = null,
        billPaidEarlyId: Long? = null,
        today: LocalDate = LocalDate.now(),
    ): WhatIfResult {
        val adjustedBills = bills.map {
            if (it.id == billPaidEarlyId) it.copy(isPaid = true) else it
        }
        val due = billsDueBeforePayday(adjustedBills, today, settings.nextPayday).sumOf { it.amountInCents }
        val paycheckDelta = (simulatedPaycheckInCents ?: settings.estimatedPaycheckInCents) - settings.estimatedPaycheckInCents
        val leftover = settings.currentBalanceInCents - due - purchaseAmountInCents + paycheckDelta
        val safe = leftover - settings.safetyBufferInCents
        return WhatIfResult(
            isSafe = leftover >= settings.safetyBufferInCents,
            projectedLeftoverInCents = leftover,
            safeToSpendInCents = max(0, safe),
        )
    }

    fun estimateHourlyPay(hourlyRateInCents: Long?, hours: Double?): Long? {
        if (hourlyRateInCents == null || hours == null) return null
        return (hourlyRateInCents * hours).toLong()
    }

    fun nextBillDate(date: LocalDate, repeatType: RepeatType): LocalDate = when (repeatType) {
        RepeatType.None -> date
        RepeatType.Weekly -> date.plusWeeks(1)
        RepeatType.Biweekly -> date.plusWeeks(2)
        RepeatType.Monthly -> date.plusMonths(1)
        RepeatType.Yearly -> date.plusYears(1)
    }

    fun formatMoney(cents: Long): String {
        val dollars = cents / 100.0
        return NumberFormat.getCurrencyInstance(Locale.US).format(dollars)
    }

    fun parseMoneyToCents(text: String): Long {
        val clean = text.replace(Regex("[^0-9.-]"), "")
        if (clean.isEmpty() || clean.none(Char::isDigit)) return 0
        return clean.toBigDecimalOrNull()
            ?.movePointRight(2)
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toLong()
            ?: 0
    }

    private fun billsDueBeforePayday(bills: List<Bill>, today: LocalDate, payday: LocalDate): List<Bill> =
        bills.filter { !it.isPaid && !it.dueDate.isBefore(today) && !it.dueDate.isAfter(payday) }
            .sortedWith(compareBy<Bill> { it.dueDate }.thenBy { it.name })

    private data class RawEvent(val date: LocalDate, val title: String, val amount: Long)
}
