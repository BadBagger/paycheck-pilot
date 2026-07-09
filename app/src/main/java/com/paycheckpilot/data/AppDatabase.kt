package com.paycheckpilot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserBudgetSettings::class,
        Bill::class,
        Paycheck::class,
        ConnectedAccount::class,
        DetectedPaycheck::class,
        DetectedBill::class,
        BankSummarySnapshot::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): UserBudgetSettingsDao
    abstract fun billDao(): BillDao
    abstract fun paycheckDao(): PaycheckDao
    abstract fun bankDao(): BankDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS connected_accounts (
                        accountId TEXT NOT NULL PRIMARY KEY,
                        institutionName TEXT NOT NULL,
                        accountName TEXT NOT NULL,
                        accountMask TEXT NOT NULL,
                        accountType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        lastSyncedAtMillis INTEGER
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS detected_paychecks (
                        paycheckId TEXT NOT NULL PRIMARY KEY,
                        payerName TEXT NOT NULL,
                        amountInCents INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        cadence TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        accountNickname TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS detected_bills (
                        billId TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        amountInCents INTEGER NOT NULL,
                        nextDueDate TEXT NOT NULL,
                        cadence TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        accountNickname TEXT NOT NULL,
                        category TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bank_summary (
                        id INTEGER NOT NULL PRIMARY KEY,
                        accountBalanceInCents INTEGER NOT NULL,
                        expectedPaycheckInCents INTEGER NOT NULL,
                        nextPayday TEXT,
                        billsBeforePaydayInCents INTEGER NOT NULL,
                        safeToSpendInCents INTEGER NOT NULL,
                        warning TEXT,
                        syncedAtMillis INTEGER
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paycheck-pilot.db",
                )
                    .addMigrations(migration1To2)
                    .build()
                    .also { instance = it }
            }
    }
}
