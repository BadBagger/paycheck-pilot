package com.paycheckpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ChevronLeft
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paycheckpilot.data.PayFrequency
import com.paycheckpilot.data.RepeatType
import com.paycheckpilot.data.UserBudgetSettings
import com.paycheckpilot.ui.PaycheckPilotUiState
import java.time.LocalDate
import java.time.YearMonth

private val AppBlue = Color(0xFF0B65D8)
private val AppGreen = Color(0xFF18A957)
private val AppRed = Color(0xFFE94350)
private val AppYellow = Color(0xFFFFB300)
private val AppInk = Color(0xFF09152F)
private val AppMuted = Color(0xFF536072)
private val AppPage = Color(0xFFF5F7FB)
private val AppCard = Color.White

private data class DayProjection(
    val balanceInCents: Long,
    val hasPositiveChange: Boolean,
    val hasNegativeChange: Boolean,
    val events: List<CalendarEvent>,
)

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
                    Text("Next Payday", color = AppMuted)
                    Text(settings.nextPayday.prettyDate(), style = MaterialTheme.typography.headlineSmall, color = AppInk, fontWeight = FontWeight.Bold)
                    Text("${dashboard.daysUntilPayday} days away", color = AppMuted)
                }
                IconBubble(Icons.Default.CalendarMonth, AppBlue)
            }
        }

        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Estimated Paycheck", color = AppInk, fontWeight = FontWeight.Bold)
                Text(settings.payFrequency.label, color = AppMuted)
            }
            Text(settings.estimatedPaycheckInCents.moneyLabel(), style = MaterialTheme.typography.headlineMedium, color = AppGreen, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth(),
                color = AppGreen,
                trackColor = Color(0xFFE8EDF4),
            )
            Text("Estimate for next payday", color = AppMuted)
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
            PrimaryButton(onClick = onCalendar, modifier = Modifier.weight(1f), text = "Calendar")
            PrimaryButton(onClick = onWhatIf, modifier = Modifier.weight(1f), text = "What if")
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
                Text("This Paycheck", color = AppInk, fontWeight = FontWeight.Bold)
                Text(settings.nextPayday.prettyDate(), color = AppMuted)
            }
            Text("Net Income", color = AppMuted)
            Text(settings.estimatedPaycheckInCents.moneyLabel(), style = MaterialTheme.typography.headlineMedium, color = AppGreen, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { 0.75f }, modifier = Modifier.size(150.dp), strokeWidth = 18.dp, color = AppBlue, trackColor = Color(0xFFE8EDF4))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dashboard.safeToSpendInCents.moneyLabel(), color = AppInk, fontWeight = FontWeight.Bold)
                    Text("Safe to spend", color = AppMuted)
                }
            }
            BudgetLine("Bills", dashboard.billsDueBeforePaydayInCents, 0.40f, AppRed, Icons.Default.AccountBalanceWallet)
            BudgetLine("Spending", maxOf(0, settings.currentBalanceInCents - dashboard.safeToSpendInCents - dashboard.billsDueBeforePaydayInCents), 0.25f, AppBlue, Icons.Default.ShoppingCart)
            BudgetLine("Buffer", settings.safetyBufferInCents, 0.14f, AppGreen, Icons.Default.Savings)
            BudgetLine("Free to Use", dashboard.safeToSpendInCents, 0.20f, AppYellow, Icons.Default.CheckCircle)
        }
        Button(
            onClick = onManageBills,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF83D8C5), contentColor = AppInk),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Manage Bills")
        }
    }
}

@Composable
fun MockCalendarScreen(state: PaycheckPilotUiState) {
    val today = LocalDate.now()
    var month by remember(state.settings?.nextPayday) {
        mutableStateOf(YearMonth.from(state.settings?.nextPayday ?: today))
    }
    var selectedDate by remember { mutableStateOf(today) }
    val projections = calendarProjections(state, month, today)
    LaunchedEffect(month) {
        if (YearMonth.from(selectedDate) != month) {
            selectedDate = month.atDay(1)
        }
    }
    MockScreen {
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = AppMuted)
                }
                Text("${month.month.name.lowercase().replaceFirstChar { it.titlecase() }} ${month.year}", color = AppInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = AppMuted)
                }
            }
            CalendarGrid(month, selectedDate, today, projections, onSelectDate = { selectedDate = it })
        }
        SelectedDayCard(selectedDate, projections[selectedDate])
        CardBlock {
            Text("Upcoming", color = AppInk, fontWeight = FontWeight.Bold)
            calendarEvents(state, today, today.plusMonths(2))
                .filter { !it.date.isBefore(today) }
                .sortedBy { it.date }
                .take(4)
                .forEach { event ->
                    UpcomingRow(
                        if (event.amountInCents > 0) Icons.Default.Savings else Icons.Default.Home,
                        event.title,
                        event.date.prettyDate(),
                        kotlin.math.abs(event.amountInCents).moneyLabel(),
                        if (event.amountInCents > 0) AppGreen else AppBlue,
                    )
                }
        }
    }
}

@Composable
private fun SelectedDayCard(date: LocalDate, projection: DayProjection?) {
    CardBlock(containerColor = Color(0xFFEAF3FF)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(date.prettyDate(), color = AppInk, fontWeight = FontWeight.Bold)
                Text("Selected day", color = AppMuted)
            }
            projection?.let {
                Text("Balance ${it.balanceInCents.moneyLabel()}", color = AppInk, fontWeight = FontWeight.Bold)
            }
        }
        if (projection == null || projection.events.isEmpty()) {
            Text("No planned bills or paychecks on this day.", color = AppMuted)
        } else {
            projection.events.forEach { event ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(event.title, color = AppInk, fontWeight = FontWeight.SemiBold)
                    Text(
                        event.amountInCents.signedMoneyLabel(),
                        color = if (event.amountInCents >= 0) AppGreen else AppRed,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
                    Text("Overall Progress", color = AppInk, fontWeight = FontWeight.Bold)
                    Text("68%", style = MaterialTheme.typography.headlineLarge, color = AppGreen, fontWeight = FontWeight.Bold)
                    Text("You're on track!", color = AppMuted)
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { 0.68f }, modifier = Modifier.size(96.dp), strokeWidth = 10.dp, color = AppGreen, trackColor = Color(0xFFE8EDF4))
                    Icon(Icons.Default.Flag, contentDescription = null, tint = AppBlue)
                }
            }
            LinearProgressIndicator(progress = { 0.68f }, modifier = Modifier.fillMaxWidth(), color = AppGreen, trackColor = Color(0xFFE8EDF4))
            Text("${buffer.moneyLabel()} buffer target in progress", color = AppMuted)
        }
        GoalRow("Emergency Fund", "$1,020 / $1,500", "Stay prepared for the unexpected.", 0.68f, AppGreen, Icons.Default.Security)
        GoalRow("Vacation", "$600 / $1,200", "Your next adventure awaits.", 0.50f, AppBlue, Icons.Default.Savings)
        GoalRow("New Laptop", "$250 / $800", "Invest in your future.", 0.31f, Color(0xFF7E57C2), Icons.Default.AccountBalanceWallet)
        Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppBlue, contentColor = Color.White)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add New Goal")
        }
    }
}

@Composable
fun MockMoreScreen(onSettings: () -> Unit) {
    MockScreen {
        Text("Settings & Security", style = MaterialTheme.typography.headlineMedium, color = AppInk, fontWeight = FontWeight.Bold)
        Text("Your data. Your device. Your control.", color = AppMuted)
        CardBlock(containerColor = Color(0xFFEAF3FF)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Default.Security, AppBlue)
                Column {
                    Text("100% Private", color = AppInk, fontWeight = FontWeight.Bold)
                    Text("All your data stays on your device. We don't collect, store, or sell your personal information.", color = AppMuted)
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppPage)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun CardBlock(containerColor: Color = AppCard, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = AppInk),
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
            Text("Quick Overview", color = AppInk, fontWeight = FontWeight.Bold)
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
                Text(amount.moneyLabel(), color = AppInk, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(progress = { percent }, modifier = Modifier.fillMaxWidth(), color = color, trackColor = Color(0xFFE8EDF4))
        }
        Text("${(percent * 100).toInt()}%", color = AppMuted)
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    projections: Map<LocalDate, DayProjection>,
    onSelectDate: (LocalDate) -> Unit,
) {
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
                val dayNumber = day.toIntOrNull()
                val date = dayNumber?.let { month.atDay(it) }
                val isSelected = date == selectedDate
                val isToday = date == today
                val dayProjection = date?.let { projections[it] }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(if (date != null) Modifier.clickable { onSelectDate(date) } else Modifier)
                        .background(
                            when {
                                isSelected -> AppBlue
                                isToday -> Color(0xFFDCEBFF)
                                dayProjection?.events?.isNotEmpty() == true -> Color(0xFFEAF3FF)
                                else -> Color.Transparent
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            day,
                            color = if (isSelected) Color.White else AppInk,
                            fontWeight = if (isToday || dayProjection?.events?.isNotEmpty() == true) FontWeight.Bold else FontWeight.Normal,
                        )
                        dayProjection?.takeIf { it.events.isNotEmpty() }?.let {
                            Text(
                                it.balanceInCents.compactMoneyLabel(),
                                color = if (isSelected) Color.White else if (it.hasNegativeChange && !it.hasPositiveChange) AppRed else AppGreen,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            repeat(7 - week.size) { Spacer(Modifier.size(44.dp)) }
        }
    }
}

@Composable
private fun UpcomingRow(icon: ImageVector, title: String, date: String, amount: String, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBubble(icon, color)
        Column(Modifier.weight(1f)) {
            Text(title, color = AppInk, fontWeight = FontWeight.SemiBold)
            Text(date, color = AppMuted)
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
                    Text(title, color = AppInk, fontWeight = FontWeight.Bold)
                    Text(amount, color = AppGreen, fontWeight = FontWeight.Bold)
                }
                Text("${(progress * 100).toInt()}%", color = AppMuted)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = AppGreen, trackColor = Color(0xFFE8EDF4))
                Text(subtitle, color = AppMuted)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppBlue)
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppCard, contentColor = AppInk)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(icon, AppBlue)
            Column(Modifier.weight(1f)) {
                Text(title, color = AppInk, fontWeight = FontWeight.Bold)
                Text(subtitle, color = AppMuted)
            }
            TextButton(onClick = onClick) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

private fun LocalDate.prettyDate(): String =
    "${month.name.lowercase().replaceFirstChar { it.titlecase() }} $dayOfMonth, $year"

@Composable
private fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, text: String) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = AppBlue, contentColor = Color.White),
    ) {
        Text(text)
    }
}

private data class CalendarEvent(val date: LocalDate, val title: String, val amountInCents: Long)

private fun calendarProjections(state: PaycheckPilotUiState, month: YearMonth, today: LocalDate): Map<LocalDate, DayProjection> {
    val settings = state.settings ?: return emptyMap()
    val start = maxOf(today, month.atDay(1))
    val end = month.atEndOfMonth()
    if (start.isAfter(end)) return emptyMap()

    val events = calendarEvents(state, start, end).groupBy { it.date }

    var runningBalance = settings.currentBalanceInCents
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(end) }
        .associateWith { date ->
            val dayEvents = events[date].orEmpty()
            dayEvents.forEach { runningBalance += it.amountInCents }
            DayProjection(
                balanceInCents = runningBalance,
                hasPositiveChange = dayEvents.any { it.amountInCents > 0 },
                hasNegativeChange = dayEvents.any { it.amountInCents < 0 },
                events = dayEvents,
            )
        }
}

private fun calendarEvents(state: PaycheckPilotUiState, start: LocalDate, end: LocalDate): List<CalendarEvent> {
    val settings = state.settings ?: return emptyList()
    return buildList {
        paydayDatesInRange(settings, start, end).forEach {
            add(CalendarEvent(it, "Payday", settings.estimatedPaycheckInCents))
        }
        state.bills.filterNot { it.isPaid }.forEach { bill ->
            billDatesInRange(bill.dueDate, bill.repeatType, start, end).forEach {
                add(CalendarEvent(it, bill.name, -bill.amountInCents))
            }
        }
    }
}

private fun Long.signedMoneyLabel(): String =
    if (this >= 0) "+${moneyLabel()}" else "-${kotlin.math.abs(this).moneyLabel()}"

private fun paydayDatesInRange(settings: UserBudgetSettings, start: LocalDate, end: LocalDate): List<LocalDate> {
    var date = settings.nextPayday
    while (date.isAfter(start)) {
        val previous = date.minusPayPeriod(settings.payFrequency)
        if (!previous.isBefore(start.minusDays(40))) date = previous else break
    }
    while (date.isBefore(start)) date = date.plusPayPeriod(settings.payFrequency)
    return generateSequence(date) { it.plusPayPeriod(settings.payFrequency) }
        .takeWhile { !it.isAfter(end) }
        .toList()
}

private fun billDatesInRange(firstDueDate: LocalDate, repeatType: RepeatType, start: LocalDate, end: LocalDate): List<LocalDate> {
    if (repeatType == RepeatType.None) return listOf(firstDueDate).filter { !it.isBefore(start) && !it.isAfter(end) }
    var date = firstDueDate
    while (date.isBefore(start)) date = date.plusRepeat(repeatType)
    return generateSequence(date) { it.plusRepeat(repeatType) }
        .takeWhile { !it.isAfter(end) }
        .toList()
}

private fun LocalDate.plusPayPeriod(frequency: PayFrequency): LocalDate = when (frequency) {
    PayFrequency.Weekly -> plusWeeks(1)
    PayFrequency.Biweekly -> plusWeeks(2)
    PayFrequency.TwiceMonthly -> plusDays(15)
    PayFrequency.Monthly -> plusMonths(1)
}

private fun LocalDate.minusPayPeriod(frequency: PayFrequency): LocalDate = when (frequency) {
    PayFrequency.Weekly -> minusWeeks(1)
    PayFrequency.Biweekly -> minusWeeks(2)
    PayFrequency.TwiceMonthly -> minusDays(15)
    PayFrequency.Monthly -> minusMonths(1)
}

private fun LocalDate.plusRepeat(repeatType: RepeatType): LocalDate = when (repeatType) {
    RepeatType.None -> this
    RepeatType.Weekly -> plusWeeks(1)
    RepeatType.Biweekly -> plusWeeks(2)
    RepeatType.Monthly -> plusMonths(1)
    RepeatType.Yearly -> plusYears(1)
}

private fun Long.compactMoneyLabel(): String {
    val sign = if (this < 0) "-" else ""
    val absCents = kotlin.math.abs(this)
    val dollars = absCents / 100
    return if (dollars >= 1_000) "$sign\$${dollars / 1_000}k" else "$sign\$$dollars"
}
