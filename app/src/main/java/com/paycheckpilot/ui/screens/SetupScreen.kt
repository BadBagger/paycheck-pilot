package com.paycheckpilot.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
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
        SetupEntryField("Current balance", balance, { balance = it }, prefix = "$")
        SetupEntryField("Safety buffer", buffer, { buffer = it }, prefix = "$")
        EnumMenu("Pay frequency", frequency, PayFrequency.entries, { it.label }, { frequency = it })
        DateField("Next payday", payday, { payday = it })
        SetupEntryField("Estimated paycheck", paycheck, { paycheck = it }, prefix = "$")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SetupEntryField("Hourly wage", hourly, { hourly = it }, modifier = Modifier.weight(1f), prefix = "$")
            SetupEntryField("Hours", hours, { hours = it }, modifier = Modifier.weight(1f))
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

@Composable
private fun SetupEntryField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(4.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .align(Alignment.TopStart),
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .border(1.dp, borderColor, shape)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (prefix != null) {
                        Text(prefix, color = textColor, fontWeight = FontWeight.Medium)
                    }
                    Box(Modifier.weight(1f)) {
                        innerTextField()
                    }
                }
            }
        },
    )
}
