package com.paycheckpilot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.paycheckpilot.data.PayFrequency
import com.paycheckpilot.ui.PaycheckPilotUiState

@Composable
fun SettingsScreen(
    state: PaycheckPilotUiState,
    onSave: (Long, Long, PayFrequency, java.time.LocalDate, Long, Long?, Double?) -> Unit,
    onSample: () -> Unit,
    onResetDemo: () -> Unit,
    onSimulateNextPayday: () -> Unit,
    onSimulateLowerPaycheck: () -> Unit,
    onSimulateMissingPaycheck: () -> Unit,
    onSimulateBillBeforePayday: () -> Unit,
    onMockPremiumChange: (Boolean) -> Unit,
) {
    val settings = state.settings
    var balance by remember(settings) { mutableStateOf(settings?.currentBalanceInCents?.let { it / 100.0 }?.toString().orEmpty()) }
    var buffer by remember(settings) { mutableStateOf(settings?.safetyBufferInCents?.let { it / 100.0 }?.toString().orEmpty()) }
    var frequency by remember(settings) { mutableStateOf(settings?.payFrequency ?: PayFrequency.Biweekly) }
    var payday by remember(settings) { mutableStateOf(settings?.nextPayday?.toString().orEmpty()) }
    var paycheck by remember(settings) { mutableStateOf(settings?.estimatedPaycheckInCents?.let { it / 100.0 }?.toString().orEmpty()) }
    var hourly by remember(settings) { mutableStateOf(settings?.hourlyRateInCents?.let { it / 100.0 }?.toString().orEmpty()) }
    var hours by remember(settings) { mutableStateOf(settings?.averageHours?.toString().orEmpty()) }

    ScreenScaffold("Settings") {
        MoneyField("Current balance", balance, { balance = it })
        MoneyField("Safety buffer", buffer, { buffer = it })
        EnumMenu("Pay frequency", frequency, PayFrequency.entries, { it.label }, { frequency = it })
        DateField("Next payday", payday, { payday = it })
        MoneyField("Estimated paycheck", paycheck, { paycheck = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(hourly, { hourly = it }, label = { Text("Hourly wage") }, prefix = { Text("$") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(hours, { hours = it }, label = { Text("Hours") }, singleLine = true, modifier = Modifier.weight(1f))
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
        ) { Text("Save settings") }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Premium plan", fontWeight = FontWeight.Bold)
                Text(state.premiumUpsell)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Mock Premium", fontWeight = FontWeight.SemiBold)
                        Text(if (state.mockPremiumEnabled) "On for testing" else "Off. Manual planning stays free.")
                    }
                    Switch(checked = state.mockPremiumEnabled, onCheckedChange = onMockPremiumChange)
                }
                Text("Free: manual paycheck setup, manual bills, manual safe-to-spend, basic payday countdown, basic reminders, and limited CSV import when available.")
                Text("Premium: bank/card sync, automatic paycheck and bill detection, auto-updated safe-to-spend, lower or missing paycheck alerts, bills-before-payday watch-outs, Renewal Radar sharing, advanced reports, backup/export, style packs, and widgets.")
                Text("If Premium expires, existing manual data and confirmed paychecks stay visible. Automatic bank sync pauses and manual editing remains available.")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Developer demo mode")
                Text("Demo mode is available while bank sync is not configured.")
                Button(onClick = onSample, modifier = Modifier.fillMaxWidth()) { Text("Use demo financial data") }
                TextButton(onClick = onResetDemo, modifier = Modifier.fillMaxWidth()) { Text("Reset demo data") }
                TextButton(onClick = onSimulateNextPayday, modifier = Modifier.fillMaxWidth()) { Text("Simulate next payday") }
                TextButton(onClick = onSimulateLowerPaycheck, modifier = Modifier.fillMaxWidth()) { Text("Simulate lower paycheck") }
                TextButton(onClick = onSimulateMissingPaycheck, modifier = Modifier.fillMaxWidth()) { Text("Simulate missing paycheck") }
                TextButton(onClick = onSimulateBillBeforePayday, modifier = Modifier.fillMaxWidth()) { Text("Simulate bill before payday") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About")
                Text("Paycheck Pilot is a planning tool. It does not provide financial advice and does not connect to your bank directly.")
                Text("Plaid Link is optional for bank sync. Demo mode and manual planning work without Plaid credentials, a hosted backend, or a real bank account.")
            }
        }
    }
}
