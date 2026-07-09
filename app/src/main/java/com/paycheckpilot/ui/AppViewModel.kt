package com.paycheckpilot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paycheckpilot.data.AppDatabase
import com.paycheckpilot.data.AppInstallId
import com.paycheckpilot.data.BackendApiConfig
import com.paycheckpilot.data.BackendPlaidApi
import com.paycheckpilot.data.BankConnectionRepository
import com.paycheckpilot.data.BankConnectionStatus
import com.paycheckpilot.data.BankSettingsStore
import com.paycheckpilot.data.BankSummarySnapshot
import com.paycheckpilot.data.BankSyncRepository
import com.paycheckpilot.data.Bill
import com.paycheckpilot.data.BudgetRepository
import com.paycheckpilot.data.ConnectedAccount
import com.paycheckpilot.data.DetectedBill
import com.paycheckpilot.data.DetectedPaycheck
import com.paycheckpilot.data.DemoFinancialScenario
import com.paycheckpilot.data.PayFrequency
import com.paycheckpilot.data.Paycheck
import com.paycheckpilot.data.PlaidLinkMetadata
import com.paycheckpilot.data.PlaidPublicToken
import com.paycheckpilot.data.RepeatType
import com.paycheckpilot.data.UserBudgetSettings
import com.paycheckpilot.domain.BudgetCalculator
import com.paycheckpilot.domain.DashboardProjection
import com.paycheckpilot.domain.TimelineEvent
import com.paycheckpilot.widget.WidgetSnapshotStore
import com.paycheckpilot.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PaycheckPilotUiState(
    val settings: UserBudgetSettings? = null,
    val bills: List<Bill> = emptyList(),
    val paychecks: List<Paycheck> = emptyList(),
    val connectedAccounts: List<ConnectedAccount> = emptyList(),
    val detectedPaychecks: List<DetectedPaycheck> = emptyList(),
    val detectedBills: List<DetectedBill> = emptyList(),
    val bankSummary: BankSummarySnapshot? = null,
    val dashboard: DashboardProjection? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val bankSyncInProgress: Boolean = false,
    val bankMessage: String? = null,
    val pendingPlaidLinkToken: String? = null,
    val connectionState: BankConnectionStatus = BankConnectionStatus.NotConnected,
    val backendUrl: String = "",
    val mockPremiumEnabled: Boolean = false,
    val premiumUpsell: String = PREMIUM_UPSELL_COPY,
)

const val PREMIUM_UPSELL_COPY: String =
    "Premium connects Paycheck Pilot to your bank/card so it can find paychecks, bills, and safe-to-spend money automatically."

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BudgetRepository
    private val bankSettingsStore: BankSettingsStore
    private val bankConnectionRepository: BankConnectionRepository
    private val bankSyncRepository: BankSyncRepository
    private val bankSyncInProgress = MutableStateFlow(false)
    private val bankMessage = MutableStateFlow<String?>(null)
    private val pendingPlaidLinkToken = MutableStateFlow<String?>(null)
    private val connectionState = MutableStateFlow(BankConnectionStatus.NotConnected)

    val uiState: StateFlow<PaycheckPilotUiState>

    init {
        val db = AppDatabase.get(application)
        repository = BudgetRepository(db.settingsDao(), db.billDao(), db.paycheckDao())
        bankSettingsStore = BankSettingsStore(application)
        val backendApi = BackendPlaidApi(
            BackendApiConfig(
                baseUrlProvider = { bankSettingsStore.currentBackendUrl() },
                userId = AppInstallId.get(application),
                allowLocalHttp = false,
            ),
        )
        bankConnectionRepository = BankConnectionRepository(backendApi, db.bankDao())
        bankSyncRepository = BankSyncRepository(backendApi, db.bankDao())

        viewModelScope.launch {
            bankConnectionRepository.clearConnectingPlaceholder()
        }

        val baseState = combine(
            repository.settings,
            repository.bills,
            repository.paychecks,
            bankConnectionRepository.connectedAccounts,
            bankSyncRepository.detectedPaychecks,
            bankSyncRepository.detectedBills,
            bankSyncRepository.bankSummary,
            bankSettingsStore.backendUrl,
            bankSettingsStore.mockPremiumEnabled,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val settings = values[0] as UserBudgetSettings?
            @Suppress("UNCHECKED_CAST")
            val bills = values[1] as List<Bill>
            @Suppress("UNCHECKED_CAST")
            val paychecks = values[2] as List<Paycheck>
            @Suppress("UNCHECKED_CAST")
            val connectedAccounts = values[3] as List<ConnectedAccount>
            @Suppress("UNCHECKED_CAST")
            val detectedPaychecks = values[4] as List<DetectedPaycheck>
            @Suppress("UNCHECKED_CAST")
            val detectedBills = values[5] as List<DetectedBill>
            val bankSummary = values[6] as BankSummarySnapshot?
            val backendUrl = values[7] as String
            val mockPremiumEnabled = values[8] as Boolean
            val today = LocalDate.now()
            val nextPaycheck = paychecks.firstOrNull { !it.date.isBefore(today) }
            PaycheckPilotUiState(
                settings = settings,
                bills = bills,
                paychecks = paychecks,
                connectedAccounts = connectedAccounts,
                detectedPaychecks = detectedPaychecks,
                detectedBills = detectedBills,
                bankSummary = bankSummary,
                dashboard = settings?.let { BudgetCalculator.dashboard(it, bills, today) },
                timeline = settings?.let { BudgetCalculator.timeline(it, bills, nextPaycheck, today) }.orEmpty(),
                backendUrl = backendUrl,
                mockPremiumEnabled = mockPremiumEnabled,
            )
        }
        uiState = combine(
            baseState,
            bankSyncInProgress,
            bankMessage,
            pendingPlaidLinkToken,
            connectionState,
        ) { state, syncing, message, linkToken, connection ->
            state.copy(
                bankSyncInProgress = syncing,
                bankMessage = message,
                pendingPlaidLinkToken = linkToken,
                connectionState = state.inferConnectionState(syncing, connection),
            )
        }.onEach { state ->
            WidgetSnapshotStore.save(application, state)
            WidgetUpdater.updateAll(application)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaycheckPilotUiState())
    }

    fun saveSettings(
        currentBalance: Long,
        safetyBuffer: Long,
        payFrequency: PayFrequency,
        nextPayday: LocalDate,
        estimatedPaycheck: Long,
        hourlyRate: Long?,
        averageHours: Double?,
    ) = viewModelScope.launch {
        repository.saveSettings(
            UserBudgetSettings(
                currentBalanceInCents = currentBalance,
                safetyBufferInCents = safetyBuffer,
                payFrequency = payFrequency,
                nextPayday = nextPayday,
                estimatedPaycheckInCents = estimatedPaycheck,
                hourlyRateInCents = hourlyRate,
                averageHours = averageHours,
            ),
        )
        repository.savePaycheck(
            Paycheck(date = nextPayday, estimatedAmountInCents = estimatedPaycheck),
        )
    }

    fun saveBill(
        existing: Bill?,
        name: String,
        amount: Long,
        dueDate: LocalDate,
        repeatType: RepeatType,
        category: String,
        notes: String?,
    ) = viewModelScope.launch {
        repository.saveBill(
            Bill(
                id = existing?.id ?: 0,
                name = name.ifBlank { "Bill" },
                amountInCents = amount,
                dueDate = dueDate,
                repeatType = repeatType,
                category = category.ifBlank { "General" },
                isPaid = existing?.isPaid ?: false,
                notes = notes?.ifBlank { null },
            ),
        )
    }

    fun deleteBill(bill: Bill) = viewModelScope.launch { repository.deleteBill(bill) }
    fun setBillPaid(id: Long, isPaid: Boolean) = viewModelScope.launch { repository.setBillPaid(id, isPaid) }

    fun savePaycheck(
        existing: Paycheck?,
        date: LocalDate,
        estimatedAmount: Long,
        actualAmount: Long?,
        hoursWorked: Double?,
        notes: String?,
    ) = viewModelScope.launch {
        repository.savePaycheck(
            Paycheck(
                id = existing?.id ?: 0,
                date = date,
                estimatedAmountInCents = estimatedAmount,
                actualAmountInCents = actualAmount,
                hoursWorked = hoursWorked,
                notes = notes?.ifBlank { null },
            ),
        )
    }

    fun deletePaycheck(paycheck: Paycheck) = viewModelScope.launch { repository.deletePaycheck(paycheck) }

    fun addSampleData() = loadDemoFinancialData(DemoFinancialScenario.Default)

    fun applyEarlyBillPayment(bill: Bill) = viewModelScope.launch {
        repository.setBillPaid(bill.id, true)
    }

    fun updateBackendUrl(value: String) {
        bankSettingsStore.updateBackendUrl(value)
        bankMessage.value = "Backend URL updated."
    }

    fun setMockPremium(enabled: Boolean) {
        bankSettingsStore.updateMockPremium(enabled)
        bankMessage.value = if (enabled) {
            "Mock Premium enabled for testing bank/card sync."
        } else {
            "Mock Premium expired. Manual planning stays available and automatic bank sync is paused."
        }
    }

    fun startPlaidLink() {
        viewModelScope.launch {
            if (!bankSettingsStore.hasMockPremium()) {
                connectionState.value = BankConnectionStatus.NotConnected
                bankMessage.value = PREMIUM_UPSELL_COPY
                return@launch
            }
            connectionState.value = BankConnectionStatus.Connecting
            bankMessage.value = "Requesting a secure Plaid Link token..."
            runCatching {
                bankConnectionRepository.markConnectingPlaceholder()
                bankConnectionRepository.createLinkToken()
            }.onSuccess { linkToken ->
                if (linkToken.mockMode) {
                    connectMockBackend()
                } else {
                    pendingPlaidLinkToken.value = linkToken.token
                    bankMessage.value = "Opening Plaid Link..."
                }
            }.onFailure { error ->
                bankConnectionRepository.clearConnectingPlaceholder()
                connectionState.value = BankConnectionStatus.Disconnected
                bankMessage.value = error.toBankErrorMessage("Backend unavailable. Demo Mode works without Plaid.")
            }
        }
    }

    fun markPlaidLinkLaunched() {
        pendingPlaidLinkToken.value = null
    }

    fun handlePlaidSuccess(publicToken: String, metadata: PlaidLinkMetadata) {
        viewModelScope.launch {
            if (metadata.accounts.isEmpty()) {
                bankConnectionRepository.clearConnectingPlaceholder()
                connectionState.value = BankConnectionStatus.Disconnected
                bankMessage.value = "No supported checking, savings, or credit card accounts were found."
                return@launch
            }
            connectionState.value = BankConnectionStatus.Connecting
            bankMessage.value = "Bank connected. Saving safe account summary..."
            runCatching {
                bankConnectionRepository.exchangePublicToken(PlaidPublicToken(publicToken), metadata)
                bankSyncInProgress.value = true
                bankSyncRepository.syncNow()
            }.onSuccess {
                connectionState.value = BankConnectionStatus.Connected
                bankMessage.value = "Connected and synced. Review detected paychecks and bills."
            }.onFailure { error ->
                bankSyncRepository.markSyncFailed()
                connectionState.value = BankConnectionStatus.SyncFailed
                bankMessage.value = error.toBankErrorMessage("Connected, but sync failed. Planning still works.")
            }.also {
                bankSyncInProgress.value = false
            }
        }
    }

    fun handlePlaidExit(message: String, permissionRevoked: Boolean = false) {
        pendingPlaidLinkToken.value = null
        connectionState.value = if (permissionRevoked) BankConnectionStatus.PermissionRevoked else BankConnectionStatus.Disconnected
        bankMessage.value = message
        viewModelScope.launch { bankConnectionRepository.clearConnectingPlaceholder() }
    }

    fun syncBankAccounts() {
        viewModelScope.launch {
            if (!bankSettingsStore.hasMockPremium()) {
                connectionState.value = BankConnectionStatus.Disconnected
                bankMessage.value = "Mock Premium expired. Automatic bank sync is paused, but manual editing still works."
                return@launch
            }
            bankSyncInProgress.value = true
            bankMessage.value = null
            runCatching {
                bankSyncRepository.syncNow()
            }.onSuccess {
                connectionState.value = BankConnectionStatus.Connected
                bankMessage.value = "Sync complete. Review detected paycheck and bill summaries."
            }.onFailure { error ->
                bankSyncRepository.markSyncFailed()
                connectionState.value = BankConnectionStatus.SyncFailed
                bankMessage.value = error.toBankErrorMessage("Sync failed. Demo Mode still works.")
            }.also {
                bankSyncInProgress.value = false
            }
        }
    }

    fun loadBankDemoMode() {
        loadDemoFinancialData(DemoFinancialScenario.Default)
    }

    fun resetDemoData() {
        loadDemoFinancialData(DemoFinancialScenario.Default, resetExisting = true, label = "Demo data reset.")
    }

    fun simulateNextPayday() {
        loadDemoFinancialData(DemoFinancialScenario.NextPayday, label = "Demo next payday moved closer.")
    }

    fun simulateLowerPaycheck() {
        loadDemoFinancialData(DemoFinancialScenario.LowerPaycheck, label = "Demo lower paycheck loaded.")
    }

    fun simulateMissingPaycheck() {
        loadDemoFinancialData(DemoFinancialScenario.MissingPaycheck, label = "Demo missing paycheck loaded.")
    }

    fun simulateBillBeforePayday() {
        loadDemoFinancialData(DemoFinancialScenario.BillBeforePayday, label = "Demo bill-before-payday scenario loaded.")
    }

    private fun loadDemoFinancialData(
        scenario: DemoFinancialScenario,
        resetExisting: Boolean = true,
        label: String = "Demo Mode loaded. No bank connection was made.",
    ) {
        viewModelScope.launch {
            bankSyncInProgress.value = true
            connectionState.value = BankConnectionStatus.Connecting
            bankMessage.value = "Loading demo bank summaries..."
            runCatching {
                repository.addDemoFinancialData(scenario, resetExisting = resetExisting)
                bankConnectionRepository.connectDemoAccount()
                bankSyncRepository.syncDemoData(uiState.value.settings, scenario)
            }.onSuccess {
                connectionState.value = BankConnectionStatus.Connected
                bankMessage.value = label
            }.onFailure { error ->
                connectionState.value = BankConnectionStatus.SyncFailed
                bankMessage.value = error.toBankErrorMessage("Demo Mode failed to load.")
            }.also {
                bankSyncInProgress.value = false
            }
        }
    }

    fun disconnectAccount(account: ConnectedAccount) {
        viewModelScope.launch {
            runCatching {
                bankConnectionRepository.disconnect(account.accountId)
            }.onSuccess {
                connectionState.value = BankConnectionStatus.Disconnected
                bankMessage.value = "${account.institutionName} disconnected."
            }.onFailure { error ->
                bankMessage.value = error.toBankErrorMessage("Disconnect failed. Try again.")
            }
        }
    }

    fun deleteBankData(deleteBackend: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (deleteBackend) bankConnectionRepository.deleteBackendData()
                bankConnectionRepository.deleteLocalBankData()
            }.onSuccess {
                connectionState.value = BankConnectionStatus.NotConnected
                bankMessage.value = "Bank sync data deleted from this app${if (deleteBackend) " and backend" else ""}."
            }.onFailure { error ->
                bankMessage.value = error.toBankErrorMessage("Could not delete all bank sync data.")
            }
        }
    }

    fun applyDetectedBill(detectedBill: DetectedBill) {
        saveBill(
            existing = null,
            name = detectedBill.name,
            amount = detectedBill.amountInCents,
            dueDate = detectedBill.nextDueDate,
            repeatType = detectedBill.cadence.toRepeatType(),
            category = detectedBill.category.ifBlank { "Detected bill" },
            notes = "Detected from connected account summary. Confidence ${(detectedBill.confidence * 100).toInt()}%.",
        )
    }

    fun applyDetectedPaycheck(detectedPaycheck: DetectedPaycheck) {
        savePaycheck(
            existing = null,
            date = detectedPaycheck.date,
            estimatedAmount = detectedPaycheck.amountInCents,
            actualAmount = detectedPaycheck.amountInCents,
            hoursWorked = null,
            notes = "Detected from ${detectedPaycheck.accountNickname}. Confidence ${(detectedPaycheck.confidence * 100).toInt()}%.",
        )
    }

    private suspend fun connectMockBackend() {
        connectionState.value = BankConnectionStatus.Connecting
        bankMessage.value = "Demo backend connected. Syncing paycheck summaries..."
        runCatching {
            bankConnectionRepository.exchangePublicToken(
                PlaidPublicToken("public-mock-paycheck-pilot"),
                PlaidLinkMetadata(
                    institution = com.paycheckpilot.data.PlaidInstitutionMetadata("ins_mock", "Plaid Sandbox Bank"),
                    accounts = listOf(
                        com.paycheckpilot.data.PlaidAccountMetadata(
                            id = "mock-checking",
                            name = "Everyday Checking",
                            mask = "0000",
                            type = "depository",
                            subtype = "checking",
                        ),
                    ),
                ),
            )
            bankSyncInProgress.value = true
            bankSyncRepository.syncNow()
        }.onSuccess {
            connectionState.value = BankConnectionStatus.Connected
            bankMessage.value = "Demo backend synced. Review detected paychecks and bills."
        }.onFailure {
            bankConnectionRepository.connectDemoAccount()
            repository.addDemoFinancialData(DemoFinancialScenario.Default)
            bankSyncRepository.syncDemoData(uiState.value.settings, DemoFinancialScenario.Default)
            connectionState.value = BankConnectionStatus.Connected
            bankMessage.value = "Demo mode is available while bank sync is not configured."
        }.also {
            bankSyncInProgress.value = false
        }
    }
}

private fun PaycheckPilotUiState.inferConnectionState(syncing: Boolean, fallback: BankConnectionStatus): BankConnectionStatus = when {
    syncing -> BankConnectionStatus.Syncing
    connectedAccounts.any { it.status == BankConnectionStatus.PermissionRevoked } -> BankConnectionStatus.PermissionRevoked
    connectedAccounts.any { it.status == BankConnectionStatus.SyncFailed } -> BankConnectionStatus.SyncFailed
    connectedAccounts.any { it.status == BankConnectionStatus.Connected } -> BankConnectionStatus.Connected
    connectedAccounts.any { it.status == BankConnectionStatus.Connecting } -> BankConnectionStatus.Connecting
    connectedAccounts.any { it.status == BankConnectionStatus.Disconnected } -> BankConnectionStatus.Disconnected
    else -> fallback
}

private fun String.toRepeatType(): RepeatType = when (lowercase()) {
    "weekly" -> RepeatType.Weekly
    "biweekly" -> RepeatType.Biweekly
    "annual", "yearly" -> RepeatType.Yearly
    else -> RepeatType.Monthly
}

private fun Throwable.toBankErrorMessage(fallback: String): String {
    val text = message.orEmpty().lowercase()
    return when {
        "backend url is not configured" in text -> "Demo mode is available while bank sync is not configured."
        "plaid credentials are required" in text || "secret" in text -> "Plaid sandbox secrets are missing on the backend."
        "link token" in text || "link_token" in text -> "Link token failed. Check the hosted backend Plaid sandbox configuration."
        "beta_full" in text -> "The Plaid beta is full. Demo Mode still works."
        "beta_not_allowed" in text -> "This install is not on the Plaid beta allowlist. Demo Mode still works."
        "bank backend must use https" in text -> "Bank sync backend must use HTTPS."
        "expired" in text || "invalid" in text -> "Link token expired. Tap Connect bank/card again."
        "offline" in text || "timeout" in text -> "Network offline or backend timed out."
        "network" in text || "unable" in text || "failed" in text -> "Demo mode is available while bank sync is not configured."
        "no supported" in text -> "No supported checking, savings, or credit card accounts were found."
        "relink" in text || "permission" in text || "revoked" in text -> "Permission revoked. Reconnect to continue syncing."
        else -> fallback
    }
}
