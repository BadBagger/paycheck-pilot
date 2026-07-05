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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycheckpilot.data.Bill
import com.paycheckpilot.data.RepeatType
import com.paycheckpilot.ui.PaycheckPilotUiState
import java.time.LocalDate

@Composable
fun BillsScreen(
    state: PaycheckPilotUiState,
    onSave: (Bill?, String, Long, LocalDate, RepeatType, String, String?) -> Unit,
    onDelete: (Bill) -> Unit,
    onPaidChange: (Long, Boolean) -> Unit,
) {
    var editing by remember { mutableStateOf<Bill?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Bill?>(null) }
    val payday = state.settings?.nextPayday

    ScreenScaffold("Bills") {
        Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Text("Add bill")
        }
        if (state.bills.isEmpty()) EmptyState("Add bills to see what is due before payday.")
        state.bills.sortedBy { it.dueDate }.forEach { bill ->
            val dueBeforePayday = payday != null && !bill.dueDate.isAfter(payday) && !bill.isPaid
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(bill.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${bill.category} • Due ${bill.dueDate} • ${bill.repeatType.label}")
                        }
                        Text(bill.amountInCents.moneyLabel(), fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = bill.isPaid, onCheckedChange = { onPaidChange(bill.id, it) })
                        Text(if (bill.isPaid) "Paid" else "Unpaid")
                    }
                    if (dueBeforePayday) StatusChip("Due before next payday")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editing = bill }) { Text("Edit") }
                        TextButton(onClick = { deleting = bill }) { Text("Delete") }
                    }
                }
            }
        }
    }

    if (adding) BillDialog(null, onDismiss = { adding = false }, onSave = {
        onSave(null, it.name, it.amountInCents, it.dueDate, it.repeatType, it.category, it.notes)
        adding = false
    })
    editing?.let { bill ->
        BillDialog(bill, onDismiss = { editing = null }, onSave = {
            onSave(bill, it.name, it.amountInCents, it.dueDate, it.repeatType, it.category, it.notes)
            editing = null
        })
    }
    deleting?.let { bill ->
        ConfirmDeleteDialog(
            title = "Delete ${bill.name}?",
            onDismiss = { deleting = null },
            onConfirm = {
                onDelete(bill)
                deleting = null
            },
        )
    }
}

@Composable
private fun BillDialog(bill: Bill?, onDismiss: () -> Unit, onSave: (Bill) -> Unit) {
    var name by remember { mutableStateOf(bill?.name.orEmpty()) }
    var amount by remember { mutableStateOf(bill?.amountInCents?.let { it / 100.0 }?.toString().orEmpty()) }
    var dueDate by remember { mutableStateOf((bill?.dueDate ?: LocalDate.now()).toString()) }
    var repeat by remember { mutableStateOf(bill?.repeatType ?: RepeatType.Monthly) }
    var category by remember { mutableStateOf(bill?.category ?: "General") }
    var notes by remember { mutableStateOf(bill?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bill == null) "Add bill" else "Edit bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                MoneyField("Amount", amount, { amount = it })
                DateField("Due date", dueDate, { dueDate = it })
                EnumMenu("Repeats", repeat, RepeatType.entries, { it.label }, { repeat = it })
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    Bill(
                        id = bill?.id ?: 0,
                        name = name,
                        amountInCents = money(amount),
                        dueDate = date(dueDate),
                        repeatType = repeat,
                        category = category,
                        isPaid = bill?.isPaid ?: false,
                        notes = notes.ifBlank { null },
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
