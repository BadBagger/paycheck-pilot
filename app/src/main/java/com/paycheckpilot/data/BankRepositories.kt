package com.paycheckpilot.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class BankConnectionRepository(
    private val backendApi: PlaidBackendApi,
    private val bankDao: BankDao,
) {
    val connectedAccounts: Flow<List<ConnectedAccount>> = bankDao.observeConnectedAccounts()

    suspend fun createLinkToken(): PlaidLinkToken = backendApi.createLinkToken()

    suspend fun exchangePublicToken(publicToken: PlaidPublicToken, metadata: PlaidLinkMetadata?) {
        val accounts = backendApi.exchangePublicToken(publicToken, metadata)
            .accounts
            .map { it.toEntity(lastSyncedAtMillis = null, status = BankConnectionStatus.Connected) }
        bankDao.deleteAccountById(CONNECTING_PLACEHOLDER_ID)
        bankDao.upsertAccounts(accounts)
    }

    suspend fun markConnectingPlaceholder() {
        bankDao.upsertAccounts(
            listOf(
                ConnectedAccount(
                    accountId = CONNECTING_PLACEHOLDER_ID,
                    institutionName = "Plaid Link",
                    accountName = "Connecting bank or card",
                    accountMask = "",
                    accountType = "Secure connection",
                    status = BankConnectionStatus.Connecting,
                ),
            ),
        )
    }

    suspend fun clearConnectingPlaceholder() {
        bankDao.deleteAccountById(CONNECTING_PLACEHOLDER_ID)
    }

    suspend fun connectDemoAccount() {
        bankDao.deleteAccountById(CONNECTING_PLACEHOLDER_ID)
        bankDao.upsertAccounts(
            listOf(
                ConnectedAccount(
                    accountId = DEMO_ACCOUNT_ID,
                    institutionName = "Demo Bank",
                    accountName = "Everyday Checking",
                    accountMask = "0000",
                    accountType = "checking",
                    status = BankConnectionStatus.Connected,
                    lastSyncedAtMillis = System.currentTimeMillis(),
                ),
            ),
        )
    }

    suspend fun disconnect(accountId: String) {
        if (accountId != DEMO_ACCOUNT_ID) backendApi.disconnectAccount(accountId)
        bankDao.setAccountStatus(accountId, BankConnectionStatus.Disconnected)
    }

    suspend fun deleteLocalBankData() {
        bankDao.deleteAllAccounts()
        bankDao.deleteAllDetectedPaychecks()
        bankDao.deleteAllDetectedBills()
        bankDao.deleteSummary()
    }

    suspend fun deleteBackendData() {
        backendApi.deleteBackendData()
    }

    companion object {
        const val CONNECTING_PLACEHOLDER_ID = "connecting-placeholder"
        const val DEMO_ACCOUNT_ID = "demo-checking"
    }
}

class BankSyncRepository(
    private val backendApi: PlaidBackendApi,
    private val bankDao: BankDao,
) {
    val detectedPaychecks: Flow<List<DetectedPaycheck>> = bankDao.observeDetectedPaychecks()
    val detectedBills: Flow<List<DetectedBill>> = bankDao.observeDetectedBills()
    val bankSummary: Flow<BankSummarySnapshot?> = bankDao.observeBankSummary()

    suspend fun syncNow() {
        bankDao.setAllActiveStatuses(BankConnectionStatus.Syncing)
        val result = backendApi.syncPaycheckPilot()
        val syncedAt = result.syncedAtMillis
        if (result.accounts.isNotEmpty()) {
            bankDao.upsertAccounts(result.accounts.map { it.toEntity(syncedAt, BankConnectionStatus.Connected) })
        } else {
            bankDao.markAllSynced(syncedAt)
        }
        bankDao.upsertDetectedPaychecks(result.paychecks)
        bankDao.upsertDetectedBills(result.bills)
        result.snapshot?.let { bankDao.upsertSummary(it.copy(syncedAtMillis = syncedAt)) }
    }

    suspend fun markSyncFailed() {
        bankDao.setAllActiveStatuses(BankConnectionStatus.SyncFailed)
    }

    suspend fun syncDemoData(
        settings: UserBudgetSettings? = null,
        scenario: DemoFinancialScenario = DemoFinancialScenario.Default,
    ) {
        val today = LocalDate.now()
        val syncedAt = System.currentTimeMillis()
        val nextPayday = when (scenario) {
            DemoFinancialScenario.NextPayday -> today.plusDays(2)
            DemoFinancialScenario.MissingPaycheck -> today.minusDays(1)
            else -> settings?.nextPayday ?: today.plusDays(7)
        }
        val expectedPaycheck = settings?.estimatedPaycheckInCents ?: 70_000L
        val displayedPaycheck = if (scenario == DemoFinancialScenario.LowerPaycheck) 61_800L else expectedPaycheck
        val balance = when (scenario) {
            DemoFinancialScenario.BillBeforePayday -> 92_000L
            DemoFinancialScenario.LowerPaycheck -> 126_000L
            DemoFinancialScenario.MissingPaycheck -> 74_000L
            else -> settings?.currentBalanceInCents ?: 184_000L
        }
        val rentDate = if (scenario == DemoFinancialScenario.BillBeforePayday) today.plusDays(1) else nextPayday.minusDays(1).coerceAtLeast(today.plusDays(1))
        val bills = listOf(
            DetectedBill(
                billId = "demo-rent",
                name = "Rent",
                amountInCents = 85_000,
                nextDueDate = rentDate,
                cadence = "Monthly",
                confidence = 0.91f,
                accountNickname = "Demo Checking",
                category = "Housing",
            ),
            DetectedBill(
                billId = "demo-phone",
                name = "Phone bill",
                amountInCents = 8_200,
                nextDueDate = today.plusDays(3),
                cadence = "Monthly",
                confidence = 0.84f,
                accountNickname = "Demo Checking",
                category = "Utilities",
            ),
            DetectedBill(
                billId = "demo-electric",
                name = "Electric bill",
                amountInCents = 15_750,
                nextDueDate = today.plusDays(5),
                cadence = "Monthly",
                confidence = 0.78f,
                accountNickname = "Demo Checking",
                category = "Utilities",
            ),
            DetectedBill(
                billId = "demo-netflix",
                name = "Netflix",
                amountInCents = 1_549,
                nextDueDate = today.plusDays(2),
                cadence = "Monthly",
                confidence = 0.94f,
                accountNickname = "Demo Debit",
                category = "Streaming",
            ),
            DetectedBill(
                billId = "demo-gym",
                name = "Gym",
                amountInCents = 2_999,
                nextDueDate = today.plusDays(9),
                cadence = "Monthly",
                confidence = 0.86f,
                accountNickname = "Demo Debit",
                category = "Health",
            ),
            DetectedBill(
                billId = "demo-gas",
                name = "Gas station",
                amountInCents = 4_600,
                nextDueDate = today.plusDays(1),
                cadence = "Weekly",
                confidence = 0.72f,
                accountNickname = "Demo Checking",
                category = "Transportation",
            ),
            DetectedBill(
                billId = "demo-groceries",
                name = "Grocery Store",
                amountInCents = 9_250,
                nextDueDate = today.plusDays(4),
                cadence = "Weekly",
                confidence = 0.69f,
                accountNickname = "Demo Checking",
                category = "Food",
            ),
            DetectedBill(
                billId = "demo-duplicate-1",
                name = "Spotify duplicate example",
                amountInCents = 1_199,
                nextDueDate = today.plusDays(2),
                cadence = "Needs review",
                confidence = 0.45f,
                accountNickname = "Demo Debit",
                category = "Possible duplicate",
            ),
            DetectedBill(
                billId = "demo-duplicate-2",
                name = "Spotify duplicate example",
                amountInCents = 1_199,
                nextDueDate = today.plusDays(3),
                cadence = "Needs review",
                confidence = 0.45f,
                accountNickname = "Demo Debit",
                category = "Possible duplicate",
            ),
        )
        val dueBeforePayday = bills.filter { !it.nextDueDate.isAfter(nextPayday) }.sumOf { it.amountInCents }
        val safe = (balance - dueBeforePayday - (settings?.safetyBufferInCents ?: 20_000L)).coerceAtLeast(0)
        bankDao.upsertDetectedPaychecks(
            listOf(
                DetectedPaycheck(
                    paycheckId = "demo-publix-next",
                    payerName = "PUBLIX PAYROLL",
                    amountInCents = displayedPaycheck,
                    date = nextPayday,
                    cadence = "Weekly",
                    confidence = if (scenario == DemoFinancialScenario.MissingPaycheck) 0.35f else 0.91f,
                    accountNickname = "Demo Checking",
                ),
                DetectedPaycheck(
                    paycheckId = "demo-publix-previous",
                    payerName = "PUBLIX PAYROLL",
                    amountInCents = expectedPaycheck,
                    date = today.minusDays(7),
                    cadence = "Weekly",
                    confidence = 0.82f,
                    accountNickname = "Demo Checking",
                ),
                DetectedPaycheck(
                    paycheckId = "demo-acme-next",
                    payerName = "ACME PAYROLL",
                    amountInCents = 142_000,
                    date = today.plusDays(14),
                    cadence = "Biweekly",
                    confidence = 0.88f,
                    accountNickname = "Demo Checking",
                ),
                DetectedPaycheck(
                    paycheckId = "demo-doordash-gig",
                    payerName = "DOORDASH",
                    amountInCents = 16_300,
                    date = today.minusDays(2),
                    cadence = "Irregular/gig",
                    confidence = 0.64f,
                    accountNickname = "Demo Checking",
                ),
            ),
        )
        bankDao.upsertDetectedBills(bills)
        bankDao.upsertSummary(
            BankSummarySnapshot(
                accountBalanceInCents = balance,
                expectedPaycheckInCents = expectedPaycheck,
                nextPayday = nextPayday,
                billsBeforePaydayInCents = dueBeforePayday,
                safeToSpendInCents = safe,
                warning = when (scenario) {
                    DemoFinancialScenario.LowerPaycheck -> "This paycheck looks lower than usual."
                    DemoFinancialScenario.MissingPaycheck -> "Expected paycheck not found yet."
                    DemoFinancialScenario.BillBeforePayday -> "Rent and other bills are expected before payday."
                    else -> if (balance - dueBeforePayday < 0) "Upcoming bills may hit before your next paycheck." else null
                },
                syncedAtMillis = syncedAt,
            ),
        )
        bankDao.markAllSynced(syncedAt)
    }
}

private fun ConnectedAccountSummary.toEntity(lastSyncedAtMillis: Long?, status: BankConnectionStatus): ConnectedAccount =
    ConnectedAccount(
        accountId = accountId,
        institutionName = institutionName,
        accountName = accountName,
        accountMask = accountMask,
        accountType = accountType,
        status = status,
        lastSyncedAtMillis = lastSyncedAtMillis,
    )

private fun LocalDate.coerceAtLeast(minimum: LocalDate): LocalDate =
    if (isBefore(minimum)) minimum else this
