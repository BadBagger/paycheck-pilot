package com.paycheckpilot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paycheckpilot.ui.PaycheckPilotUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: PaycheckPilotUiState, onAddBill: () -> Unit, onAddPaycheck: () -> Unit, onWhatIf: () -> Unit) {
    val settings = state.settings
    val projection = state.dashboard
    ScreenScaffold("Paycheck Pilot") {
        if (settings == null || projection == null) {
            EmptyState("Set up your balance and next payday to start planning.")
            return@ScreenScaffold
        }
        StatCard("Current balance", settings.currentBalanceInCents.moneyLabel())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Next payday", settings.nextPayday.toString(), Modifier.weight(1f))
            StatCard("Days until payday", projection.daysUntilPayday.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Bills before payday", projection.billsDueBeforePaydayInCents.moneyLabel(), Modifier.weight(1f))
            StatCard("Left after bills", projection.projectedLeftoverInCents.moneyLabel(), Modifier.weight(1f), projection.mayRunShort)
        }
        StatCard("Safe to spend", projection.safeToSpendInCents.moneyLabel(), warning = projection.safeToSpendIsLow || projection.mayRunShort)
        if (projection.mayRunShort) {
            StatusChip("You may run short before payday")
        } else if (projection.safeToSpendIsLow) {
            StatusChip("Safe-to-spend is low")
        } else {
            StatusChip("Plan looks okay before payday")
        }
        Text("Upcoming bills before payday", style = MaterialTheme.typography.titleMedium)
        if (projection.upcomingBills.isEmpty()) EmptyState("No unpaid bills due before payday.")
        projection.upcomingBills.forEach { bill ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${bill.name}\n${bill.dueDate}", modifier = Modifier.weight(1f))
                    Text(bill.amountInCents.moneyLabel())
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddBill) { Icon(Icons.Default.Add, null); Text("Add bill") }
            Button(onClick = onAddPaycheck) { Icon(Icons.Default.Payments, null); Text("Add paycheck") }
            Button(onClick = onWhatIf) { Icon(Icons.Default.Calculate, null); Text("What if I spend money?") }
        }
    }
}
