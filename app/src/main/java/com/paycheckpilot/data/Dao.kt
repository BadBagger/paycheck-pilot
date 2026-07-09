package com.paycheckpilot.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface UserBudgetSettingsDao {
    @Query("SELECT * FROM user_budget_settings WHERE id = 1")
    fun observeSettings(): Flow<UserBudgetSettings?>

    @Query("SELECT * FROM user_budget_settings WHERE id = 1")
    suspend fun getSettings(): UserBudgetSettings?

    @Upsert
    suspend fun upsert(settings: UserBudgetSettings)

    @Query("DELETE FROM user_budget_settings")
    suspend fun deleteAll()
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDate ASC, name ASC")
    fun observeBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE dueDate <= :payday ORDER BY dueDate ASC, name ASC")
    fun observeBillsDueBefore(payday: LocalDate): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE name = :name AND amountInCents = :amountInCents AND dueDate = :dueDate LIMIT 1")
    suspend fun findMatchingBill(name: String, amountInCents: Long, dueDate: LocalDate): Bill?

    @Upsert
    suspend fun upsert(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)

    @Query("UPDATE bills SET isPaid = :isPaid WHERE id = :billId")
    suspend fun setPaid(billId: Long, isPaid: Boolean)

    @Query("DELETE FROM bills")
    suspend fun deleteAll()
}

@Dao
interface PaycheckDao {
    @Query("SELECT * FROM paychecks ORDER BY date ASC")
    fun observePaychecks(): Flow<List<Paycheck>>

    @Query("SELECT * FROM paychecks WHERE date >= :today ORDER BY date ASC LIMIT 1")
    fun observeNextPaycheck(today: LocalDate): Flow<Paycheck?>

    @Query("SELECT * FROM paychecks WHERE date = :date AND estimatedAmountInCents = :estimatedAmountInCents LIMIT 1")
    suspend fun findMatchingPaycheck(date: LocalDate, estimatedAmountInCents: Long): Paycheck?

    @Upsert
    suspend fun upsert(paycheck: Paycheck)

    @Delete
    suspend fun delete(paycheck: Paycheck)

    @Query("DELETE FROM paychecks")
    suspend fun deleteAll()
}

@Dao
interface BankDao {
    @Query("SELECT * FROM connected_accounts ORDER BY institutionName ASC, accountName ASC")
    fun observeConnectedAccounts(): Flow<List<ConnectedAccount>>

    @Query("SELECT * FROM detected_paychecks ORDER BY date DESC")
    fun observeDetectedPaychecks(): Flow<List<DetectedPaycheck>>

    @Query("SELECT * FROM detected_bills ORDER BY nextDueDate ASC, name ASC")
    fun observeDetectedBills(): Flow<List<DetectedBill>>

    @Query("SELECT * FROM bank_summary WHERE id = 1")
    fun observeBankSummary(): Flow<BankSummarySnapshot?>

    @Upsert
    suspend fun upsertAccounts(accounts: List<ConnectedAccount>)

    @Upsert
    suspend fun upsertDetectedPaychecks(paychecks: List<DetectedPaycheck>)

    @Upsert
    suspend fun upsertDetectedBills(bills: List<DetectedBill>)

    @Upsert
    suspend fun upsertSummary(snapshot: BankSummarySnapshot)

    @Query("DELETE FROM connected_accounts WHERE accountId = :accountId")
    suspend fun deleteAccountById(accountId: String)

    @Query("UPDATE connected_accounts SET status = :status WHERE accountId = :accountId")
    suspend fun setAccountStatus(accountId: String, status: BankConnectionStatus)

    @Query("UPDATE connected_accounts SET status = :status WHERE status != 'Disconnected'")
    suspend fun setAllActiveStatuses(status: BankConnectionStatus)

    @Query("UPDATE connected_accounts SET lastSyncedAtMillis = :syncedAt, status = 'Connected' WHERE status != 'Disconnected'")
    suspend fun markAllSynced(syncedAt: Long)

    @Query("DELETE FROM connected_accounts")
    suspend fun deleteAllAccounts()

    @Query("DELETE FROM detected_paychecks")
    suspend fun deleteAllDetectedPaychecks()

    @Query("DELETE FROM detected_paychecks WHERE paycheckId = :paycheckId")
    suspend fun deleteDetectedPaycheck(paycheckId: String)

    @Query("DELETE FROM detected_bills")
    suspend fun deleteAllDetectedBills()

    @Query("DELETE FROM detected_bills WHERE billId = :billId")
    suspend fun deleteDetectedBill(billId: String)

    @Query("DELETE FROM bank_summary")
    suspend fun deleteSummary()
}
