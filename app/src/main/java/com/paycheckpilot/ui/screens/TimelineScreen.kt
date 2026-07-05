package com.paycheckpilot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycheckpilot.ui.PaycheckPilotUiState

@Composable
fun TimelineScreen(state: PaycheckPilotUiState) {
    ScreenScaffold("Timeline") {
        Text("Running balance from today through the next payday.")
        if (state.timeline.isEmpty()) {
            EmptyState("Add setup details and bills to see the timeline.")
        }
        state.timeline.forEach { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        event.belowZero -> MaterialTheme.colorScheme.errorContainer
                        event.belowSafetyBuffer -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(event.title, fontWeight = FontWeight.SemiBold)
                            Text(event.date.toString())
                        }
                        Text(event.amountInCents.moneyLabel(), fontWeight = FontWeight.SemiBold)
                    }
                    LabeledRow("Running balance", event.runningBalanceInCents.moneyLabel())
                    if (event.belowZero) Text("You may run short", color = MaterialTheme.colorScheme.error)
                    if (event.belowSafetyBuffer) Text("Below safety buffer")
                }
            }
        }
    }
}
