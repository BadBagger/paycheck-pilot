package com.paycheckpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paycheckpilot.ui.AppViewModel
import com.paycheckpilot.ui.screens.BillsScreen
import com.paycheckpilot.ui.screens.HomeScreen
import com.paycheckpilot.ui.screens.MockBudgetScreen
import com.paycheckpilot.ui.screens.MockCalendarScreen
import com.paycheckpilot.ui.screens.MockGoalsScreen
import com.paycheckpilot.ui.screens.MockHomeScreen
import com.paycheckpilot.ui.screens.MockMoreScreen
import com.paycheckpilot.ui.screens.PaychecksScreen
import com.paycheckpilot.ui.screens.SettingsScreen
import com.paycheckpilot.ui.screens.SetupScreen
import com.paycheckpilot.ui.screens.TimelineScreen
import com.paycheckpilot.ui.screens.WhatIfScreen
import com.paycheckpilot.ui.theme.PaycheckPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaycheckPilotTheme {
                PaycheckPilotApp()
            }
        }
    }
}

private data class Destination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

private val destinations = listOf(
    Destination("home", "Home", { Icon(Icons.Default.Home, contentDescription = null) }),
    Destination("budget", "Budget", { Icon(Icons.Default.BarChart, contentDescription = null) }),
    Destination("calendar", "Calendar", { Icon(Icons.Default.CalendarMonth, contentDescription = null) }),
    Destination("goals", "Goals", { Icon(Icons.Default.TrackChanges, contentDescription = null) }),
    Destination("more", "More", { Icon(Icons.Default.MoreHoriz, contentDescription = null) }),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaycheckPilotApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()

    LaunchedEffect(state.settings, backStack?.destination?.route) {
        val route = backStack?.destination?.route
        if (state.settings == null && route != "setup") {
            navController.navigate("setup") { launchSingleTop = true }
        } else if (state.settings != null && route == "setup") {
            navController.navigate("home") { popUpTo("setup") { inclusive = true } }
        }
    }

    Scaffold(
        topBar = {
            val route = backStack?.destination?.route
            if (route != "setup") {
                CenterAlignedTopAppBar(
                    title = { Text(topBarTitle(route), color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0B65D8),
                    ),
                )
            }
        },
        bottomBar = {
            val route = backStack?.destination?.route
            if (route != "setup") {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = backStack?.destination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = { navController.navigate(destination.route) { launchSingleTop = true } },
                            icon = destination.icon,
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("setup") {
                SetupScreen(
                    onSave = { balance, buffer, frequency, payday, paycheck, hourly, hours ->
                        viewModel.saveSettings(balance, buffer, frequency, payday, paycheck, hourly, hours)
                        navController.navigate("home") { popUpTo("setup") { inclusive = true } }
                    },
                    onSample = {
                        viewModel.addSampleData()
                        navController.navigate("home") { popUpTo("setup") { inclusive = true } }
                    },
                )
            }
            composable("home") {
                MockHomeScreen(
                    state,
                    onBudget = { navController.navigate("budget") },
                    onCalendar = { navController.navigate("calendar") },
                    onWhatIf = { navController.navigate("whatif") },
                )
            }
            composable("budget") { MockBudgetScreen(state, onManageBills = { navController.navigate("bills") }) }
            composable("calendar") { MockCalendarScreen(state) }
            composable("goals") { MockGoalsScreen(state) }
            composable("more") { MockMoreScreen(onSettings = { navController.navigate("settings") }) }
            composable("bills") { BillsScreen(state, viewModel::saveBill, viewModel::deleteBill, viewModel::setBillPaid) }
            composable("paychecks") { PaychecksScreen(state, viewModel::savePaycheck, viewModel::deletePaycheck) }
            composable("timeline") { TimelineScreen(state) }
            composable("whatif") { WhatIfScreen(state, viewModel::applyEarlyBillPayment) }
            composable("settings") { SettingsScreen(state, viewModel::saveSettings, viewModel::addSampleData) }
        }
    }
}

private fun topBarTitle(route: String?): String = when (route) {
    "budget" -> "Budget"
    "calendar" -> "Calendar"
    "goals" -> "Paycheck Pilot"
    "more", "settings" -> "Paycheck Pilot"
    "bills" -> "Bills"
    "paychecks" -> "Income"
    "timeline" -> "Timeline"
    "whatif" -> "What if"
    else -> "Paycheck Pilot"
}
