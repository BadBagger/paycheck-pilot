package com.paycheckpilot.data

interface PlaidBackendApi {
    suspend fun createLinkToken(): PlaidLinkToken
    suspend fun exchangePublicToken(publicToken: PlaidPublicToken, metadata: PlaidLinkMetadata?): BankConnectResult
    suspend fun syncPaycheckPilot(): BankSyncSummary
    suspend fun disconnectAccount(accountId: String)
    suspend fun deleteBackendData()
}
