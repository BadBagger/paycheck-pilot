package com.paycheckpilot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.paycheckpilot.data.BankConnectionRepository
import com.paycheckpilot.data.BankConnectionStatus
import com.paycheckpilot.data.ConnectedAccount
import com.paycheckpilot.data.DetectedBill
import com.paycheckpilot.data.DetectedPaycheck
import com.paycheckpilot.ui.PaycheckPilotUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ConnectedAccountsScreen(
    state: PaycheckPilotUiState,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDemo: () -> Unit,
    onDisconnect: (ConnectedAccount) -> Unit,
    onDeleteLocal: () -> Unit,
    onDeleteBackend: () -> Unit,
    onBackendUrlChange: (String) -> Unit,
) {
    val visibleAccounts = state.connectedAccounts.filterNot {
        it.accountId == BankConnectionRepository.CONNECTING_PLACEHOLDER_ID
    }
    var backendUrl by remember(state.backendUrl) { mutableStateOf(state.backendUrl) }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Connected Accounts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Connect through Plaid Link. Paycheck Pilot never asks for bank usernames or passwords.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { PremiumBankSyncCard(state) }
        item { BankPrivacyCard() }
        item {
            BankStatusCard(state.connectionState, state.bankMessage)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onConnect,
                    enabled = state.mockPremiumEnabled && state.connectionState != BankConnectionStatus.Connecting && !state.bankSyncInProgress,
                    modifier = Modifier.weight(1f),
                ) { Text("Connect bank/card") }
                Button(
                    onClick = onSync,
                    enabled = state.mockPremiumEnabled && visibleAccounts.any { it.status == BankConnectionStatus.Connected } && !state.bankSyncInProgress,
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.bankSyncInProgress) "Syncing" else "Sync now") }
            }
        }
        item {
            TextButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                Text("Use demo financial data")
            }
        }
        item {
            Text(
                "Demo mode is available while bank sync is not configured.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        label = { Text("Hosted backend URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { onBackendUrlChange(backendUrl) }) {
                        Text("Save backend URL")
                    }
                }
            }
        }
        if (visibleAccounts.isEmpty()) {
            item { EmptyState("No connected accounts yet. Demo Mode works without Plaid.") }
        } else {
            items(visibleAccounts, key = { it.accountId }) { account ->
                ConnectedAccountCard(account, onDisconnect = { onDisconnect(account) })
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Disconnect removes the connection. Delete data clears safe summaries from this app; backend delete also asks the hosted backend to remove its stored data.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDeleteLocal) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text("Delete local")
                        }
                        TextButton(onClick = onDeleteBackend) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Text("Delete backend")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaycheckDetectionScreen(
    state: PaycheckPilotUiState,
    onApplyPaycheck: (DetectedPaycheck) -> Unit,
) {
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Paycheck Detection", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Detected deposits are estimates until you confirm them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.detectedPaychecks.isEmpty()) {
            item { EmptyState("No paycheck deposits detected yet. Sync or use Demo Mode.") }
        } else {
            items(state.detectedPaychecks, key = { it.paycheckId }) { paycheck ->
                DetectedPaycheckCard(paycheck, onApplyPaycheck)
            }
        }
    }
}

@Composable
fun IncomeHistoryScreen(state: PaycheckPilotUiState) {
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Income History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        val paychecks = state.detectedPaychecks.sortedByDescending { it.date }
        if (paychecks.isEmpty()) {
            item { EmptyState("No detected income history yet.") }
        } else {
            items(paychecks, key = { it.paycheckId }) { paycheck ->
                SimpleInfoCard(
                    title = paycheck.payerName,
                    subtitle = "${paycheck.date} • ${paycheck.cadence} • ${(paycheck.confidence * 100).toInt()}% confidence",
                    amount = paycheck.amountInCents.moneyLabel(),
                    positive = true,
                )
            }
        }
    }
}

@Composable
fun BillsBeforePaydayScreen(
    state: PaycheckPilotUiState,
    onApplyBill: (DetectedBill) -> Unit,
) {
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Bills Before Payday", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Review detected recurring expenses before adding them to your plan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val nextPayday = state.bankSummary?.nextPayday ?: state.settings?.nextPayday
        val bills = state.detectedBills
            .filter { nextPayday == null || !it.nextDueDate.isAfter(nextPayday) }
            .sortedBy { it.nextDueDate }
        if (bills.isEmpty()) {
            item { EmptyState("No detected bills before payday.") }
        } else {
            items(bills, key = { it.billId }) { bill ->
                DetectedBillCard(bill, onApplyBill)
            }
        }
    }
}

@Composable
fun BankSafeToSpendScreen(state: PaycheckPilotUiState) {
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Safe to Spend", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        val summary = state.bankSummary
        if (summary == null) {
            item { EmptyState("Sync a connected account or use Demo Mode to see bank-based estimates.") }
        } else {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Safe to spend", style = MaterialTheme.typography.titleMedium)
                        Text(summary.safeToSpendInCents.moneyLabel(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("Bills before payday: ${summary.billsBeforePaydayInCents.moneyLabel()}")
                        Text("Expected paycheck: ${summary.expectedPaycheckInCents.moneyLabel()}")
                        Text("Next payday: ${summary.nextPayday ?: "Unknown"}")
                        summary.warning?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                        Text("Last sync: ${summary.syncedAtMillis.formatTimestamp()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBankSyncCard(state: PaycheckPilotUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bank/card sync is Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(state.premiumUpsell)
            Text(
                if (state.mockPremiumEnabled) {
                    "Mock Premium is on. Bank/card sync and auto-updates are available for testing."
                } else {
                    "Free users can use manual planning and preview bank sync with demo financial data."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BankPrivacyCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null)
                Text("Private by design", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("Plaid Link handles bank login. The app sends only a public_token to the backend.")
            Text("Plaid access tokens stay encrypted on the backend. Android receives only safe account, income, bill, and spending summaries.")
        }
    }
}

@Composable
private fun BankStatusCard(status: BankConnectionStatus, message: String?) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Sync status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(status.displayLabel())
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun ConnectedAccountCard(account: ConnectedAccount, onDisconnect: () -> Unit) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(account.institutionName, fontWeight = FontWeight.Bold)
                    Text("${account.accountName} ${account.accountMask.maskForDisplay()}")
                    Text("${account.accountType} • ${account.status.displayLabel()}")
                    Text("Last sync: ${account.lastSyncedAtMillis.formatTimestamp()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.AccountBalance, contentDescription = null)
            }
            TextButton(onClick = onDisconnect) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
private fun DetectedPaycheckCard(paycheck: DetectedPaycheck, onApply: (DetectedPaycheck) -> Unit) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(paycheck.payerName, fontWeight = FontWeight.Bold)
                    Text("${paycheck.date} • ${paycheck.cadence} • ${paycheck.accountNickname}")
                }
                Text(paycheck.amountInCents.moneyLabel(), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { onApply(paycheck) }) {
                Icon(Icons.Default.Download, contentDescription = null)
                Text("Add to income")
            }
        }
    }
}

@Composable
private fun DetectedBillCard(bill: DetectedBill, onApply: (DetectedBill) -> Unit) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(bill.name, fontWeight = FontWeight.Bold)
                    Text("${bill.nextDueDate} • ${bill.cadence} • ${(bill.confidence * 100).toInt()}% confidence")
                }
                Text(bill.amountInCents.moneyLabel(), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { onApply(bill) }) {
                Icon(Icons.Default.Download, contentDescription = null)
                Text("Add to bills")
            }
        }
    }
}

@Composable
private fun SimpleInfoCard(title: String, subtitle: String, amount: String, positive: Boolean) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null)
                Column {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(amount, color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}

private fun BankConnectionStatus.displayLabel(): String = when (this) {
    BankConnectionStatus.NotConnected -> "Not connected"
    BankConnectionStatus.Connecting -> "Connecting"
    BankConnectionStatus.Connected -> "Connected"
    BankConnectionStatus.Syncing -> "Syncing"
    BankConnectionStatus.SyncFailed -> "Sync failed"
    BankConnectionStatus.Disconnected -> "Disconnected"
    BankConnectionStatus.PermissionRevoked -> "Permission revoked"
}

private fun String.maskForDisplay(): String = if (isBlank()) "****" else "****$this"

private fun Long?.formatTimestamp(): String {
    if (this == null) return "Never"
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
}
