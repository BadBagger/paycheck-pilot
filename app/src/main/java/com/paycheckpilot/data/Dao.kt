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
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDate ASC, name ASC")
    fun observeBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE dueDate <= :payday ORDER BY dueDate ASC, name ASC")
    fun observeBillsDueBefore(payday: LocalDate): Flow<List<Bill>>

    @Upsert
    suspend fun upsert(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)

    @Query("UPDATE bills SET isPaid = :isPaid WHERE id = :billId")
    suspend fun setPaid(billId: Long, isPaid: Boolean)
}

@Dao
interface PaycheckDao {
    @Query("SELECT * FROM paychecks ORDER BY date ASC")
    fun observePaychecks(): Flow<List<Paycheck>>

    @Query("SELECT * FROM paychecks WHERE date >= :today ORDER BY date ASC LIMIT 1")
    fun observeNextPaycheck(today: LocalDate): Flow<Paycheck?>

    @Upsert
    suspend fun upsert(paycheck: Paycheck)

    @Delete
    suspend fun delete(paycheck: Paycheck)
}
