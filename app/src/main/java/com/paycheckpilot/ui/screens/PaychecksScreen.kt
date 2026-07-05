package com.paycheckpilot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycheckpilot.data.Paycheck
import com.paycheckpilot.domain.BudgetCalculator
import com.paycheckpilot.ui.PaycheckPilotUiState
import java.time.LocalDate

@Composable
fun PaychecksScreen(
    state: PaycheckPilotUiState,
    onSave: (Paycheck?, LocalDate, Long, Long?, Double?, String?) -> Unit,
    onDelete: (Paycheck) -> Unit,
) {
    var editing by remember { mutableStateOf<Paycheck?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Paycheck?>(null) }
    val settings = state.settings
    val upcoming = state.paychecks.firstOrNull { !it.date.isBefore(LocalDate.now()) }

    ScreenScaffold("Paychecks / Income") {
        if (settings != null) {
            StatCard("Upcoming paycheck", upcoming?.date?.toString() ?: settings.nextPayday.toString())
            StatCard("Estimated amount", (upcoming?.estimatedAmountInCents ?: settings.estimatedPaycheckInCents).moneyLabel())
            BudgetCalculator.estimateHourlyPay(settings.hourlyRateInCents, settings.averageHours)?.let {
                StatusChip("Hourly estimate: ${it.moneyLabel()} gross")
            }
        }
        Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Text("Add paycheck")
        }
        if (state.paychecks.isEmpty()) EmptyState("Paychecks you add here are estimates until you enter actual pay.")
        state.paychecks.sortedBy { it.date }.forEach { paycheck ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(paycheck.date.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (paycheck.actualAmountInCents == null) "Estimate" else "Actual entered")
                        }
                        Text((paycheck.actualAmountInCents ?: paycheck.estimatedAmountInCents).moneyLabel(), fontWeight = FontWeight.SemiBold)
                    }
                    paycheck.hoursWorked?.let { Text("Hours worked: $it") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editing = paycheck }) { Text("Edit") }
                        TextButton(onClick = { deleting = paycheck }) { Text("Delete") }
                    }
                }
            }
        }
    }

    if (adding) PaycheckDialog(null, onDismiss = { adding = false }, onSave = {
        onSave(null, it.date, it.estimatedAmountInCents, it.actualAmountInCents, it.hoursWorked, it.notes)
        adding = false
    })
    editing?.let { paycheck ->
        PaycheckDialog(paycheck, onDismiss = { editing = null }, onSave = {
            onSave(paycheck, it.date, it.estimatedAmountInCents, it.actualAmountInCents, it.hoursWorked, it.notes)
            editing = null
        })
    }
    deleting?.let { paycheck ->
        ConfirmDeleteDialog(
            title = "Delete paycheck?",
            onDismiss = { deleting = null },
            onConfirm = {
                onDelete(paycheck)
                deleting = null
            },
        )
    }
}

@Composable
private fun PaycheckDialog(paycheck: Paycheck?, onDismiss: () -> Unit, onSave: (Paycheck) -> Unit) {
    var dateText by remember { mutableStateOf((paycheck?.date ?: LocalDate.now()).toString()) }
    var estimate by remember { mutableStateOf(paycheck?.estimatedAmountInCents?.let { it / 100.0 }?.toString().orEmpty()) }
    var actual by remember { mutableStateOf(paycheck?.actualAmountInCents?.let { it / 100.0 }?.toString().orEmpty()) }
    var hours by remember { mutableStateOf(paycheck?.hoursWorked?.toString().orEmpty()) }
    var notes by remember { mutableStateOf(paycheck?.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (paycheck == null) "Add paycheck" else "Edit paycheck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Date", dateText, { dateText = it })
                MoneyField("Estimated pay", estimate, { estimate = it })
                MoneyField("Actual pay", actual, { actual = it })
                OutlinedTextField(hours, { hours = it }, label = { Text("Hours worked") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    Paycheck(
                        id = paycheck?.id ?: 0,
                        date = date(dateText),
                        estimatedAmountInCents = money(estimate),
                        actualAmountInCents = actual.takeIf { it.isNotBlank() }?.let(::money),
                        hoursWorked = hours.toDoubleOrNull(),
                        notes = notes.ifBlank { null },
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
