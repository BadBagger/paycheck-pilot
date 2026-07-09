package com.paycheckpilot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycheckpilot.domain.BudgetCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ScreenScaffold(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, warning: Boolean = false) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun MoneyField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text("$") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: String, onValueChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selectedDate = date(value)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochMillis(),
    )

    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = { open = true }) {
                Text("Pick")
            }
        },
    )

    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millis.toLocalDate().toString())
                        }
                        open = false
                    },
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun <T> EnumMenu(label: String, value: T, options: List<T>, optionLabel: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = { expanded = true }) {
            Text(optionLabel(value))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("This removes it from Paycheck Pilot only. It does not touch your bank or bills.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun StatusChip(text: String) {
    AssistChip(onClick = {}, label = { Text(text) })
}

@Composable
fun EmptyState(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun LabeledRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

fun money(text: String): Long = runCatching { BudgetCalculator.parseMoneyToCents(text) }.getOrDefault(0)
fun date(text: String, fallback: LocalDate = LocalDate.now()): LocalDate = runCatching { LocalDate.parse(text) }.getOrDefault(fallback)
fun Long.moneyLabel(): String = BudgetCalculator.formatMoney(this)

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
