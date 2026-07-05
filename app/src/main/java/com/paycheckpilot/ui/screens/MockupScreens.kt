package com.paycheckpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycheckpilot.data.Bill
import com.paycheckpilot.ui.PaycheckPilotUiState
import java.time.LocalDate
import java.time.YearMonth

private val AppBlue = Color(0xFF0B65D8)
private val AppGreen = Color(0xFF18A957)
private val AppRed = Color(0xFFE94350)
private val AppYellow = Color(0xFFFFB300)
private val AppInk = Color(0xFF09152F)

@Composable
fun MockHomeScreen(
    state: PaycheckPilotUiState,
    onBudget: () -> Unit,
    onCalendar: () -> Unit,
    onWhatIf: () -> Unit,
) {
    val settings = state.settings
    val dashboard = state.dashboard
    MockScreen {
        if (settings == null || dashboard == null) {
            EmptyState("Set up your balance and next payday to start planning.")
            return@MockScreen
        }

        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Next Payday", color = Color(0xFF536072))
                    Text(settings.nextPayday.prettyDate(), style = MaterialTheme.typography.headlineSmall, color = AppInk, fontWeight = FontWeight.Bold)
                    Text("${dashboard.daysUntilPayday} days away", color = Color(0xFF536072))
                }
                IconBubble(Icons.Default.CalendarMonth, AppBlue)
            }
        }

        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Estimated Paycheck", fontWeight = FontWeight.Bold)
                Text(settings.payFrequency.label, color = Color(0xFF536072))
            }
            Text(settings.estimatedPaycheckInCents.moneyLabel(), style = MaterialTheme.typography.headlineMedium, color = AppGreen, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth(),
                color = AppGreen,
                trackColor = Color(0xFFE8EDF4),
            )
            Text("Estimate for next payday", color = Color(0xFF536072))
        }

        QuickOverviewCard(
            bills = dashboard.billsDueBeforePaydayInCents,
            spending = maxOf(0, settings.currentBalanceInCents - dashboard.safeToSpendInCents - dashboard.billsDueBeforePaydayInCents),
            savings = settings.safetyBufferInCents,
            free = dashboard.safeToSpendInCents,
            onViewAll = onBudget,
        )

        CardBlock(containerColor = Color(0xFFEAF3FF)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBubble(Icons.Default.Security, AppBlue)
                Column(Modifier.weight(1f)) {
                    Text(if (dashboard.mayRunShort) "You may run short" else "You're on track!", color = AppInk, fontWeight = FontWeight.Bold)
                    Text(
                        if (dashboard.mayRunShort) "Review bills before payday." else "Keep it up. Your plan is in good shape.",
                        color = Color(0xFF536072),
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppBlue)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onCalendar, modifier = Modifier.weight(1f)) { Text("Calendar") }
            Button(onClick = onWhatIf, modifier = Modifier.weight(1f)) { Text("What if") }
        }
    }
}

@Composable
fun MockBudgetScreen(state: PaycheckPilotUiState, onManageBills: () -> Unit) {
    val settings = state.settings
    val dashboard = state.dashboard
    MockScreen {
        if (settings == null || dashboard == null) {
            EmptyState("Set up Paycheck Pilot to see your budget.")
            return@MockScreen
        }
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("This Paycheck", fontWeight = FontWeight.Bold)
                Text(settings.nextPayday.prettyDate(), color = Color(0xFF536072))
            }
            Text("Net Income", color = Color(0xFF536072))
            Text(settings.estimatedPaycheckInCents.moneyLabel(), style = MaterialTheme.typography.headlineMedium, color = AppGreen, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { 0.75f }, modifier = Modifier.size(150.dp), strokeWidth = 18.dp, color = AppBlue, trackColor = Color(0xFFE8EDF4))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dashboard.safeToSpendInCents.moneyLabel(), fontWeight = FontWeight.Bold)
                    Text("Safe to spend", color = Color(0xFF536072))
                }
            }
            BudgetLine("Bills", dashboard.billsDueBeforePaydayInCents, 0.40f, AppRed, Icons.Default.AccountBalanceWallet)
            BudgetLine("Spending", maxOf(0, settings.currentBalanceInCents - dashboard.safeToSpendInCents - dashboard.billsDueBeforePaydayInCents), 0.25f, AppBlue, Icons.Default.ShoppingCart)
            BudgetLine("Buffer", settings.safetyBufferInCents, 0.14f, AppGreen, Icons.Default.Savings)
            BudgetLine("Free to Use", dashboard.safeToSpendInCents, 0.20f, AppYellow, Icons.Default.CheckCircle)
        }
        Button(onClick = onManageBills, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Manage Bills")
        }
    }
}

@Composable
fun MockCalendarScreen(state: PaycheckPilotUiState) {
    val today = LocalDate.now()
    val month = YearMonth.from(state.settings?.nextPayday ?: today)
    MockScreen {
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("<", color = Color(0xFF536072), style = MaterialTheme.typography.titleLarge)
                Text("${month.month.name.lowercase().replaceFirstChar { it.titlecase() }} ${month.year}", color = AppInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(">", color = Color(0xFF536072), style = MaterialTheme.typography.titleLarge)
            }
            CalendarGrid(month, state.settings?.nextPayday)
        }
        CardBlock {
            Text("Upcoming", color = AppInk, fontWeight = FontWeight.Bold)
            state.settings?.let {
                UpcomingRow(Icons.Default.Savings, "Payday", it.nextPayday.prettyDate(), it.estimatedPaycheckInCents.moneyLabel(), AppGreen)
            }
            state.bills.sortedBy { it.dueDate }.take(4).forEach { bill ->
                UpcomingRow(Icons.Default.Home, bill.name, bill.dueDate.prettyDate(), bill.amountInCents.moneyLabel(), AppBlue)
            }
        }
    }
}

@Composable
fun MockGoalsScreen(state: PaycheckPilotUiState) {
    val buffer = state.settings?.safetyBufferInCents ?: 0
    MockScreen {
        Text("Goals", style = MaterialTheme.typography.headlineMedium, color = AppInk, fontWeight = FontWeight.Bold)
        CardBlock {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Overall Progress", fontWeight = FontWeight.Bold)
                    Text("68%", style = MaterialTheme.typography.headlineLarge, color = AppGreen, fontWeight = FontWeight.Bold)
                    Text("You're on track!", color = Color(0xFF536072))
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { 0.68f }, modifier = Modifier.size(96.dp), strokeWidth = 10.dp, color = AppGreen, trackColor = Color(0xFFE8EDF4))
                    Icon(Icons.Default.Flag, contentDescription = null, tint = AppBlue)
                }
            }
            LinearProgressIndicator(progress = { 0.68f }, modifier = Modifier.fillMaxWidth(), color = AppGreen, trackColor = Color(0xFFE8EDF4))
            Text("${buffer.moneyLabel()} buffer target in progress", color = Color(0xFF536072))
        }
        GoalRow("Emergency Fund", "$1,020 / $1,500", "Stay prepared for the unexpected.", 0.68f, AppGreen, Icons.Default.Security)
        GoalRow("Vacation", "$600 / $1,200", "Your next adventure awaits.", 0.50f, AppBlue, Icons.Default.Savings)
        GoalRow("New Laptop", "$250 / $800", "Invest in your future.", 0.31f, Color(0xFF7E57C2), Icons.Default.AccountBalanceWallet)
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add New Goal")
        }
    }
}

@Composable
fun MockMoreScreen(onSettings: () -> Unit) {
    MockScreen {
        Text("Settings & Security", style = MaterialTheme.typography.headlineMedium, color = AppInk, fontWeight = FontWeight.Bold)
        Text("Your data. Your device. Your control.", color = Color(0xFF536072))
        CardBlock(containerColor = Color(0xFFEAF3FF)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Default.Security, AppBlue)
                Column {
                    Text("100% Private", color = AppInk, fontWeight = FontWeight.Bold)
                    Text("All your data stays on your device. We don't collect, store, or sell your personal information.", color = Color(0xFF536072))
                    Text("Paycheck Pilot is a planning tool, not financial advice.", color = AppBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
        MoreRow(Icons.Default.Lock, "Local-only data", "Your data never leaves this device", onSettings)
        MoreRow(Icons.Default.Security, "App lock", "Protect your app with device security", onSettings)
        MoreRow(Icons.Default.Notifications, "Notifications", "Manage reminders and updates your way", onSettings)
        MoreRow(Icons.Default.CalendarMonth, "Pay schedule", "Set your paydays and plan with confidence", onSettings)
        MoreRow(Icons.Default.Download, "Export backup", "Create a backup from local data later", onSettings)
    }
}

@Composable
private fun MockScreen(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F7FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun CardBlock(containerColor: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun IconBubble(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun QuickOverviewCard(bills: Long, spending: Long, savings: Long, free: Long, onViewAll: () -> Unit) {
    CardBlock {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Quick Overview", fontWeight = FontWeight.Bold)
            TextButton(onClick = onViewAll) { Text("View All") }
        }
        BudgetLine("Bills", bills, 0.40f, AppRed, Icons.Default.AccountBalanceWallet)
        BudgetLine("Spending", spending, 0.25f, AppBlue, Icons.Default.ShoppingCart)
        BudgetLine("Savings", savings, 0.14f, AppGreen, Icons.Default.Savings)
        BudgetLine("Free to Use", free, 0.20f, AppYellow, Icons.Default.CheckCircle)
    }
}

@Composable
private fun BudgetLine(label: String, amount: Long, percent: Float, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBubble(icon, color)
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(amount.moneyLabel(), fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(progress = { percent }, modifier = Modifier.fillMaxWidth(), color = color, trackColor = Color(0xFFE8EDF4))
        }
        Text("${(percent * 100).toInt()}%", color = Color(0xFF536072))
    }
}

@Composable
private fun CalendarGrid(month: YearMonth, selectedDate: LocalDate?) {
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value % 7
    val days = List(offset) { "" } + (1..month.lengthOfMonth()).map { it.toString() }
    val labels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach { Text(it, color = Color(0xFF8D97A8), style = MaterialTheme.typography.labelSmall) }
    }
    days.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEach { day ->
                val isSelected = day.toIntOrNull() == selectedDate?.dayOfMonth && selectedDate?.month == month.month
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AppBlue else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(day, color = if (isSelected) Color.White else AppInk)
                }
            }
            repeat(7 - week.size) { Spacer(Modifier.size(38.dp)) }
        }
    }
}

@Composable
private fun UpcomingRow(icon: ImageVector, title: String, date: String, amount: String, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBubble(icon, color)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(date, color = Color(0xFF536072))
        }
        Text(amount, color = if (color == AppGreen) AppGreen else AppInk, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GoalRow(title: String, amount: String, subtitle: String, progress: Float, color: Color, icon: ImageVector) {
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(icon, color)
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(amount, color = AppGreen, fontWeight = FontWeight.Bold)
                }
                Text("${(progress * 100).toInt()}%", color = Color(0xFF536072))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = AppGreen, trackColor = Color(0xFFE8EDF4))
                Text(subtitle, color = Color(0xFF536072))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppBlue)
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(icon, AppBlue)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF536072))
            }
            TextButton(onClick = onClick) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

private fun LocalDate.prettyDate(): String =
    "${month.name.lowercase().replaceFirstChar { it.titlecase() }} $dayOfMonth, $year"
