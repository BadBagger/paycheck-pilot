package com.paycheckpilot.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate

class BackendPlaidApi(private val config: BackendApiConfig) : PlaidBackendApi {
    override suspend fun createLinkToken(): PlaidLinkToken = withContext(Dispatchers.IO) {
        val json = request("POST", "/api/plaid/create-link-token", JSONObject())
        PlaidLinkToken(
            token = json.getString("link_token"),
            expirationMillis = json.optString("expiration")
                .takeIf { it.isNotBlank() }
                ?.let { Instant.parse(it).toEpochMilli() }
                ?: (System.currentTimeMillis() + 30 * 60 * 1000),
            mockMode = json.optBoolean("mock_mode", false),
        )
    }

    override suspend fun exchangePublicToken(publicToken: PlaidPublicToken, metadata: PlaidLinkMetadata?): BankConnectResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("public_token", publicToken.value)
                .put("institution", metadata?.institution?.toJson() ?: JSONObject.NULL)
                .put("accounts", JSONArray().also { array ->
                    metadata?.accounts.orEmpty().forEach { array.put(it.toJson()) }
                })
            val json = request("POST", "/api/plaid/exchange-public-token", body)
            BankConnectResult(json.optJSONArray("accounts").toAccountSummaries())
        }

    override suspend fun syncPaycheckPilot(): BankSyncSummary = withContext(Dispatchers.IO) {
        val json = request("POST", "/api/paycheck/sync", JSONObject())
        val syncedAt = json.optString("syncedAt")
            .takeIf { it.isNotBlank() }
            ?.let { Instant.parse(it).toEpochMilli() }
            ?: System.currentTimeMillis()
        BankSyncSummary(
            accounts = json.optJSONArray("accounts").toAccountSummaries(),
            paychecks = json.optJSONArray("paychecks").toDetectedPaychecks(),
            bills = json.optJSONArray("bills").toDetectedBills(),
            snapshot = json.optJSONObject("summary")?.toBankSummary(syncedAt),
            syncedAtMillis = syncedAt,
        )
    }

    override suspend fun disconnectAccount(accountId: String) = withContext(Dispatchers.IO) {
        request("POST", "/api/plaid/disconnect", JSONObject().put("accountId", accountId))
        Unit
    }

    override suspend fun deleteBackendData() = withContext(Dispatchers.IO) {
        request("POST", "/api/account/delete", JSONObject())
        Unit
    }

    private fun request(method: String, path: String, body: JSONObject?): JSONObject {
        val baseUrl = config.currentBaseUrl().trimEnd('/')
        if (config.isPlaceholderEndpoint()) {
            throw IOException("Backend URL is not configured. Demo Mode works without Plaid.")
        }
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-PaycheckPilot-User-Id", config.userId)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IOException("Backend request failed ${connection.responseCode}: $responseText")
            }
            return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }
}

class BackendApiConfig(
    private val baseUrlProvider: () -> String,
    val userId: String,
    private val allowLocalHttp: Boolean = true,
) {
    fun currentBaseUrl(): String {
        val value = baseUrlProvider().trim()
        val normalized = value.lowercase()
        val isLocal = normalized.startsWith("http://10.0.2.2") ||
            normalized.startsWith("http://localhost") ||
            normalized.startsWith("http://127.0.0.1")
        require(normalized.startsWith("https://") || (allowLocalHttp && isLocal)) {
            "Bank backend must use HTTPS except local emulator development."
        }
        return value
    }

    fun isPlaceholderEndpoint(): Boolean =
        currentBaseUrl().contains("example.com", ignoreCase = true) ||
            currentBaseUrl().contains("paycheck-pilot-backend.example", ignoreCase = true)
}

private fun PlaidInstitutionMetadata.toJson(): JSONObject =
    JSONObject().put("institution_id", id).put("name", name)

private fun PlaidAccountMetadata.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("name", name)
        .put("mask", mask)
        .put("type", type)
        .put("subtype", subtype)

private fun JSONArray?.toAccountSummaries(): List<ConnectedAccountSummary> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                ConnectedAccountSummary(
                    accountId = item.optString("id", item.optString("accountId")),
                    institutionName = item.optString("institutionName", "Connected institution"),
                    accountName = item.optString("name", item.optString("accountName", "Account")),
                    accountMask = item.optString("mask", item.optString("accountMask", "")),
                    accountType = item.optString("subtype", item.optString("type", "account")),
                ),
            )
        }
    }
}

private fun JSONArray?.toDetectedPaychecks(): List<DetectedPaycheck> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                DetectedPaycheck(
                    paycheckId = item.optString("id", "paycheck-$index"),
                    payerName = item.optString("payerName", item.optString("merchantName", "Paycheck")),
                    amountInCents = item.optLong("amountInCents", item.optLong("amountCents")),
                    date = LocalDate.parse(item.optString("date")),
                    cadence = item.optString("cadence", "Recurring"),
                    confidence = item.optDouble("confidence", item.optDouble("confidenceScore", 0.8)).toFloat(),
                    accountNickname = item.optString("accountNickname", "Connected account"),
                ),
            )
        }
    }
}

private fun JSONArray?.toDetectedBills(): List<DetectedBill> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                DetectedBill(
                    billId = item.optString("id", "bill-$index"),
                    name = item.optString("name", item.optString("merchantName", "Bill")),
                    amountInCents = item.optLong("amountInCents", item.optLong("amountCents")),
                    nextDueDate = LocalDate.parse(item.optString("nextDueDate", item.optString("date"))),
                    cadence = item.optString("cadence", "Recurring"),
                    confidence = item.optDouble("confidence", item.optDouble("confidenceScore", 0.8)).toFloat(),
                    accountNickname = item.optString("accountNickname", "Connected account"),
                    category = item.optString("category", "Detected bill"),
                ),
            )
        }
    }
}

private fun JSONObject.toBankSummary(syncedAt: Long): BankSummarySnapshot =
    BankSummarySnapshot(
        accountBalanceInCents = optLong("accountBalanceInCents", optLong("currentBalanceInCents")),
        expectedPaycheckInCents = optLong("expectedPaycheckInCents"),
        nextPayday = optString("nextPayday").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
        billsBeforePaydayInCents = optLong("billsBeforePaydayInCents"),
        safeToSpendInCents = optLong("safeToSpendInCents"),
        warning = optString("warning").takeIf { it.isNotBlank() },
        syncedAtMillis = syncedAt,
    )
