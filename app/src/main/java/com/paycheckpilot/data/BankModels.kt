package com.paycheckpilot.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class BankConnectionStatus {
    NotConnected,
    Connecting,
    Connected,
    Syncing,
    SyncFailed,
    Disconnected,
    PermissionRevoked,
}

enum class DemoFinancialScenario {
    Default,
    NextPayday,
    LowerPaycheck,
    MissingPaycheck,
    BillBeforePayday,
}

@Entity(tableName = "connected_accounts")
data class ConnectedAccount(
    @PrimaryKey val accountId: String,
    val institutionName: String,
    val accountName: String,
    val accountMask: String,
    val accountType: String,
    val status: BankConnectionStatus = BankConnectionStatus.Connected,
    val lastSyncedAtMillis: Long? = null,
)

@Entity(tableName = "detected_paychecks")
data class DetectedPaycheck(
    @PrimaryKey val paycheckId: String,
    val payerName: String,
    val amountInCents: Long,
    val date: LocalDate,
    val cadence: String,
    val confidence: Float,
    val accountNickname: String,
)

@Entity(tableName = "detected_bills")
data class DetectedBill(
    @PrimaryKey val billId: String,
    val name: String,
    val amountInCents: Long,
    val nextDueDate: LocalDate,
    val cadence: String,
    val confidence: Float,
    val accountNickname: String,
    val category: String,
)

@Entity(tableName = "bank_summary")
data class BankSummarySnapshot(
    @PrimaryKey val id: Int = 1,
    val accountBalanceInCents: Long = 0,
    val expectedPaycheckInCents: Long = 0,
    val nextPayday: LocalDate? = null,
    val billsBeforePaydayInCents: Long = 0,
    val safeToSpendInCents: Long = 0,
    val warning: String? = null,
    val syncedAtMillis: Long? = null,
)

data class PlaidLinkToken(
    val token: String,
    val expirationMillis: Long,
    val mockMode: Boolean = false,
)

data class PlaidPublicToken(val value: String)

data class PlaidInstitutionMetadata(val id: String?, val name: String?)

data class PlaidAccountMetadata(
    val id: String,
    val name: String,
    val mask: String?,
    val type: String,
    val subtype: String?,
)

data class PlaidLinkMetadata(
    val institution: PlaidInstitutionMetadata?,
    val accounts: List<PlaidAccountMetadata>,
)

data class ConnectedAccountSummary(
    val accountId: String,
    val institutionName: String,
    val accountName: String,
    val accountMask: String,
    val accountType: String,
)

data class BankConnectResult(val accounts: List<ConnectedAccountSummary>)

data class BankSyncSummary(
    val accounts: List<ConnectedAccountSummary>,
    val paychecks: List<DetectedPaycheck>,
    val bills: List<DetectedBill>,
    val snapshot: BankSummarySnapshot?,
    val syncedAtMillis: Long,
)
