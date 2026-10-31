package com.kdevs.spendly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kdevs.spendly.data.local.dao.*
import com.kdevs.spendly.data.local.entity.*

/**
 * On-phone copy of the private money data. Firestore stays the cloud store; Room is what the screens read,
 * so the app opens instantly and works without internet.
 *
 * Changing a table later means bumping [version] and adding a Migration. Do not use destructive fallback:
 * rows with syncState != SYNCED have not reached the server yet and would be lost.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TxnEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        TemplateEntity::class,
        RecurringEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao

    abstract fun categories(): CategoryDao

    abstract fun txns(): TxnDao

    abstract fun budgets(): BudgetDao

    abstract fun goals(): GoalDao

    abstract fun templates(): TemplateDao

    abstract fun recurring(): RecurringDao

    companion object {
        const val NAME = "spendly.db"

        /** v2: transactions remember which recurring item created them. Existing rows get "" (added by hand). */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE txns ADD COLUMN recurringId TEXT NOT NULL DEFAULT ''")
                }
            }
    }
}