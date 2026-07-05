package com.paycheckpilot.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class PayFrequency(val label: String) {
    Weekly("Weekly"),
    Biweekly("Biweekly"),
    TwiceMonthly("Twice monthly"),
    Monthly("Monthly"),
}

enum class RepeatType(val label: String) {
    None("Does not repeat"),
    Weekly("Weekly"),
    Biweekly("Biweekly"),
    Monthly("Monthly"),
    Yearly("Yearly"),
}

@Entity(tableName = "user_budget_settings")
data class UserBudgetSettings(
    @PrimaryKey val id: Int = 1,
    val currentBalanceInCents: Long,
    val safetyBufferInCents: Long,
    val payFrequency: PayFrequency,
    val nextPayday: LocalDate,
    val estimatedPaycheckInCents: Long,
    val hourlyRateInCents: Long? = null,
    val averageHours: Double? = null,
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountInCents: Long,
    val dueDate: LocalDate,
    val repeatType: RepeatType,
    val category: String,
    val isPaid: Boolean = false,
    val notes: String? = null,
)

@Entity(tableName = "paychecks")
data class Paycheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val estimatedAmountInCents: Long,
    val actualAmountInCents: Long? = null,
    val hoursWorked: Double? = null,
    val notes: String? = null,
)
