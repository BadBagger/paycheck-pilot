package com.paycheckpilot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.paycheckpilot.data.PayFrequency
import java.time.LocalDate

@Composable
fun SetupScreen(
    onSave: (Long, Long, PayFrequency, LocalDate, Long, Long?, Double?) -> Unit,
    onSample: () -> Unit,
) {
    var balance by remember { mutableStateOf("1200") }
    var buffer by remember { mutableStateOf("100") }
    var frequency by remember { mutableStateOf(PayFrequency.Biweekly) }
    var payday by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var paycheck by remember { mutableStateOf("1500") }
    var hourly by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }

    ScreenScaffold("Set up Paycheck Pilot") {
        Text("Plan around your next payday with local estimates. Nothing connects to a bank or cloud account.")
        MoneyField("Current balance", balance, { balance = it })
        MoneyField("Safety buffer", buffer, { buffer = it })
        EnumMenu("Pay frequency", frequency, PayFrequency.entries, { it.label }, { frequency = it })
        DateField("Next payday", payday, { payday = it })
        MoneyField("Estimated paycheck", paycheck, { paycheck = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = hourly,
                onValueChange = { hourly = it },
                label = { Text("Hourly wage") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it },
                label = { Text("Hours") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Button(
            onClick = {
                onSave(
                    money(balance),
                    money(buffer),
                    frequency,
                    date(payday),
                    money(paycheck),
                    hourly.takeIf { it.isNotBlank() }?.let(::money),
                    hours.toDoubleOrNull(),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Start planning") }
        TextButton(onClick = onSample, modifier = Modifier.fillMaxWidth()) { Text("Use sample data") }
    }
}
