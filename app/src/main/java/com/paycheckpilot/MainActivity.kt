package com.paycheckpilot

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.plaid.link.FastOpenPlaidLink
import com.plaid.link.Plaid
import com.plaid.link.linkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
import com.paycheckpilot.data.PlaidAccountMetadata
import com.paycheckpilot.data.PlaidInstitutionMetadata
import com.paycheckpilot.data.PlaidLinkMetadata
import com.paycheckpilot.ui.AppViewModel
import com.paycheckpilot.ui.screens.BankSafeToSpendScreen
import com.paycheckpilot.ui.screens.BillsScreen
import com.paycheckpilot.ui.screens.BillsBeforePaydayScreen
import com.paycheckpilot.ui.screens.ConnectedAccountsScreen
import com.paycheckpilot.ui.screens.HomeScreen
import com.paycheckpilot.ui.screens.IncomeHistoryScreen
import com.paycheckpilot.ui.screens.MockBudgetScreen
import com.paycheckpilot.ui.screens.MockCalendarScreen
import com.paycheckpilot.ui.screens.MockGoalsScreen
import com.paycheckpilot.ui.screens.MockHomeScreen
import com.paycheckpilot.ui.screens.MockMoreScreen
import com.paycheckpilot.ui.screens.PaychecksScreen
import com.paycheckpilot.ui.screens.PaycheckDetectionScreen
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

private val drawerDestinations = listOf(
    Destination("home", "Home", { Icon(Icons.Default.Home, contentDescription = null) }),
    Destination("budget", "Budget", { Icon(Icons.Default.BarChart, contentDescription = null) }),
    Destination("calendar", "Calendar", { Icon(Icons.Default.CalendarMonth, contentDescription = null) }),
    Destination("bills", "Bills", { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }),
    Destination("paychecks", "Income", { Icon(Icons.Default.Event, contentDescription = null) }),
    Destination("accounts", "Connected accounts", { Icon(Icons.Default.Settings, contentDescription = null) }),
    Destination("detected-paychecks", "Paycheck detection", { Icon(Icons.Default.Event, contentDescription = null) }),
    Destination("income-history", "Income history", { Icon(Icons.Default.BarChart, contentDescription = null) }),
    Destination("detected-bills", "Bills before payday", { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }),
    Destination("bank-safe", "Bank safe to spend", { Icon(Icons.Default.TrackChanges, contentDescription = null) }),
    Destination("timeline", "Timeline", { Icon(Icons.Default.BarChart, contentDescription = null) }),
    Destination("whatif", "What if", { Icon(Icons.Default.TrackChanges, contentDescription = null) }),
    Destination("settings", "Settings", { Icon(Icons.Default.Settings, contentDescription = null) }),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaycheckPilotApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val application = LocalContext.current.applicationContext as Application
    val plaidLauncher = rememberLauncherForActivityResult(FastOpenPlaidLink()) { result ->
        when (result) {
            is LinkSuccess -> {
                val publicToken = result.publicToken
                if (publicToken.isNullOrBlank()) {
                    viewModel.handlePlaidExit("Plaid Link completed without a public token. Try connecting again.")
                } else {
                    viewModel.handlePlaidSuccess(publicToken, result.toPaycheckMetadata())
                }
            }
            is LinkExit -> {
                val errorCode = result.error?.errorCode?.toString().orEmpty()
                viewModel.handlePlaidExit(
                    message = result.toUserMessage(),
                    permissionRevoked = errorCode.contains("REVOKED", ignoreCase = true) ||
                        errorCode.contains("RELINK", ignoreCase = true),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        Plaid.setLinkEventListener { }
    }

    LaunchedEffect(state.pendingPlaidLinkToken) {
        val token = state.pendingPlaidLinkToken ?: return@LaunchedEffect
        val handler = Plaid.create(
            application,
            linkTokenConfiguration {
                this.token = token
            },
        )
        plaidLauncher.launch(handler)
        viewModel.markPlaidLinkLaunched()
    }

    LaunchedEffect(state.settings, backStack?.destination?.route) {
        val route = backStack?.destination?.route
        if (state.settings == null && route != "setup") {
            navController.navigate("setup") { launchSingleTop = true }
        } else if (state.settings != null && route == "setup") {
            navController.navigate("home") { popUpTo("setup") { inclusive = true } }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = backStack?.destination?.route != "setup",
        drawerContent = {
            if (backStack?.destination?.route != "setup") {
                ModalDrawerSheet {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Text("Paycheck Pilot", modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
                        HorizontalDivider()
                        drawerDestinations.forEach { destination ->
                            NavigationDrawerItem(
                                selected = backStack?.destination?.hierarchy?.any { it.route == destination.route } == true,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(destination.route) { launchSingleTop = true }
                                },
                                icon = destination.icon,
                                label = { Text(destination.label) },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                val route = backStack?.destination?.route
                if (route != "setup") {
                    CenterAlignedTopAppBar(
                        title = { Text(topBarTitle(route), color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate("settings") { launchSingleTop = true } }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Settings", tint = Color.White)
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
                        },
                        onSample = {
                            viewModel.addSampleData()
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
                composable("accounts") {
                    ConnectedAccountsScreen(
                        state = state,
                        onConnect = viewModel::startPlaidLink,
                        onSync = viewModel::syncBankAccounts,
                        onDemo = viewModel::loadBankDemoMode,
                        onDisconnect = viewModel::disconnectAccount,
                        onDeleteLocal = { viewModel.deleteBankData(deleteBackend = false) },
                        onDeleteBackend = { viewModel.deleteBankData(deleteBackend = true) },
                        onBackendUrlChange = viewModel::updateBackendUrl,
                    )
                }
                composable("detected-paychecks") {
                    PaycheckDetectionScreen(
                        state = state,
                        onApplyPaycheck = viewModel::applyDetectedPaycheck,
                        onExcludePaycheck = viewModel::excludeDetectedPaycheck,
                    )
                }
                composable("income-history") { IncomeHistoryScreen(state) }
                composable("detected-bills") {
                    BillsBeforePaydayScreen(
                        state = state,
                        onApplyBill = viewModel::applyDetectedBill,
                        onExcludeBill = viewModel::excludeDetectedBill,
                    )
                }
                composable("bank-safe") { BankSafeToSpendScreen(state) }
                composable("timeline") { TimelineScreen(state) }
                composable("whatif") { WhatIfScreen(state, viewModel::applyEarlyBillPayment) }
                composable("settings") {
                    SettingsScreen(
                        state = state,
                        onSave = viewModel::saveSettings,
                        onSample = viewModel::addSampleData,
                        onResetDemo = viewModel::resetDemoData,
                        onSimulateNextPayday = viewModel::simulateNextPayday,
                        onSimulateLowerPaycheck = viewModel::simulateLowerPaycheck,
                        onSimulateMissingPaycheck = viewModel::simulateMissingPaycheck,
                        onSimulateBillBeforePayday = viewModel::simulateBillBeforePayday,
                        onMockPremiumChange = viewModel::setMockPremium,
                    )
                }
            }
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
    "accounts" -> "Connected Accounts"
    "detected-paychecks" -> "Paycheck Detection"
    "income-history" -> "Income History"
    "detected-bills" -> "Bills Before Payday"
    "bank-safe" -> "Safe to Spend"
    "timeline" -> "Timeline"
    "whatif" -> "What if"
    else -> "Paycheck Pilot"
}

private fun LinkSuccess.toPaycheckMetadata(): PlaidLinkMetadata =
    PlaidLinkMetadata(
        institution = metadata.institution?.let {
            PlaidInstitutionMetadata(id = it.id, name = it.name)
        },
        accounts = metadata.accounts.map {
            PlaidAccountMetadata(
                id = it.id,
                name = it.name ?: "Account",
                mask = it.mask,
                type = it.subtype.accountType.json,
                subtype = it.subtype.json,
            )
        },
    )

private fun LinkExit.toUserMessage(): String {
    val error = error
    if (error != null) {
        error.displayMessage?.takeIf { it.isNotBlank() }?.let { return it }
        error.errorMessage?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return when (metadata.status?.jsonValue) {
        "institution_not_supported" -> "Institution connection failed or is not supported."
        "institution_not_found" -> "Institution not found. Try searching another bank or card issuer."
        "requires_credentials" -> "Plaid Link was canceled before connection finished."
        else -> "Plaid Link was canceled. Paycheck planning still works."
    }
}
