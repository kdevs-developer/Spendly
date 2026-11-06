package com.kdevs.spendly.di

import android.content.Context
import androidx.room.Room
import com.kdevs.spendly.data.local.SpendlyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext ctx: Context,
    ): SpendlyDatabase =
        Room
            .databaseBuilder(ctx, SpendlyDatabase::class.java, SpendlyDatabase.NAME)
            .addMigrations(SpendlyDatabase.MIGRATION_1_2, SpendlyDatabase.MIGRATION_2_3)
            .build()

    @Provides fun accounts(db: SpendlyDatabase) = db.accounts()

    @Provides fun categories(db: SpendlyDatabase) = db.categories()

    @Provides fun txns(db: SpendlyDatabase) = db.txns()

    @Provides fun budgets(db: SpendlyDatabase) = db.budgets()

    @Provides fun goals(db: SpendlyDatabase) = db.goals()

    @Provides fun recurring(db: SpendlyDatabase) = db.recurring()
}
