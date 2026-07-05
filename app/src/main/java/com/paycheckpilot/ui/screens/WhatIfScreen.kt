package com.paycheckpilot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paycheckpilot.data.Bill
import com.paycheckpilot.domain.BudgetCalculator
import com.paycheckpilot.ui.PaycheckPilotUiState

@Composable
fun WhatIfScreen(state: PaycheckPilotUiState, onApplyEarlyBill: (Bill) -> Unit) {
    var purchase by remember { mutableStateOf("") }
    var simulatedPaycheck by remember { mutableStateOf("") }
    var simulatedHours by remember { mutableStateOf("") }
    var earlyBill by remember { mutableStateOf<Bill?>(null) }
    val settings = state.settings
    val result = settings?.let {
        val hourlyEstimate = if (simulatedHours.isNotBlank()) {
            BudgetCalculator.estimateHourlyPay(it.hourlyRateInCents, simulatedHours.toDoubleOrNull())
        } else null
        BudgetCalculator.whatIf(
            settings = it,
            bills = state.bills,
            purchaseAmountInCents = money(purchase),
            simulatedPaycheckInCents = hourlyEstimate ?: simulatedPaycheck.takeIf { text -> text.isNotBlank() }?.let(::money),
            billPaidEarlyId = earlyBill?.id,
        )
    }

    ScreenScaffold("What-if Calculator") {
        if (settings == null || result == null) {
            EmptyState("Set up your balance and payday before running what-if checks.")
            return@ScreenScaffold
        }
        MoneyField("Possible purchase", purchase, { purchase = it })
        MoneyField("Lower paycheck estimate", simulatedPaycheck, { simulatedPaycheck = it })
        OutlinedTextField(
            value = simulatedHours,
            onValueChange = { simulatedHours = it },
            label = { Text("Fewer work hours") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Simulate paying a bill early", style = MaterialTheme.typography.titleMedium)
        if (state.dashboard?.upcomingBills.orEmpty().isEmpty()) EmptyState("No unpaid bills before payday to simulate.")
        state.dashboard?.upcomingBills.orEmpty().forEach { bill ->
            TextButton(onClick = { earlyBill = if (earlyBill?.id == bill.id) null else bill }) {
                Text(if (earlyBill?.id == bill.id) "Selected: ${bill.name}" else "${bill.name} (${bill.amountInCents.moneyLabel()})")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("New projected leftover", result.projectedLeftoverInCents.moneyLabel(), warning = result.projectedLeftoverInCents < 0)
                StatCard("New safe to spend", result.safeToSpendInCents.moneyLabel(), warning = !result.isSafe)
                Text(if (result.isSafe) "This looks safe before payday." else "This may make you run short or dip below your buffer.")
            }
        }
        earlyBill?.let { bill ->
            Button(onClick = { onApplyEarlyBill(bill); earlyBill = null }, modifier = Modifier.fillMaxWidth()) {
                Text("Apply early payment for ${bill.name}")
            }
        }
        Text("Simulations are not saved unless you apply an early bill payment.")
    }
}
