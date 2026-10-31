package com.kdevs.spendly.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kdevs.spendly.data.local.SyncState

// One Room table per private Firestore collection under users/{uid}/.
// Money is whole paise (Long). `uid` keeps two logins on one phone apart.
// `syncState` is a SyncState value: it drives the push to Firestore (module 3).

@Entity(tableName = "accounts", indices = [Index("uid")])
data class AccountEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val type: String,
    val icon: String,
    val colorKey: String,
    val openingPaise: Long,
    val archived: Boolean,
    val createdAt: Long,
    val syncState: Int = SyncState.PENDING,
)

@Entity(tableName = "categories", indices = [Index("uid"), Index("parentId")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val icon: String,
    val colorKey: String,
    val parentId: String,
    val kind: String,
    val syncState: Int = SyncState.PENDING,
)

@Entity(
    tableName = "txns",
    indices = [Index("uid", "dateMillis"), Index("accountId"), Index("toAccountId"), Index("categoryId")],
)
data class TxnEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val kind: String,
    val amountPaise: Long,
    val accountId: String,
    val toAccountId: String,
    val categoryId: String,
    val subcategoryId: String,
    val note: String,
    val dateMillis: Long,
    val syncState: Int = SyncState.PENDING,
)

@Entity(tableName = "budgets", indices = [Index("uid")])
data class BudgetEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val categoryId: String,
    val limitPaise: Long,
    val syncState: Int = SyncState.PENDING,
)

@Entity(tableName = "goals", indices = [Index("uid")])
data class GoalEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val icon: String,
    val colorKey: String,
    val targetPaise: Long,
    val savedPaise: Long,
    val deadlineMillis: Long,
    val syncState: Int = SyncState.PENDING,
)

@Entity(tableName = "templates", indices = [Index("uid")])
data class TemplateEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val kind: String,
    val amountPaise: Long,
    val accountId: String,
    val categoryId: String,
    val note: String,
    val syncState: Int = SyncState.PENDING,
)

@Entity(tableName = "recurring", indices = [Index("uid")])
data class RecurringEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val kind: String,
    val amountPaise: Long,
    val accountId: String,
    val categoryId: String,
    val subcategoryId: String = "",
    val interval: Int = 1,
    val anchorDay: Int = 0,
    val every: String,
    val nextMillis: Long,
    val active: Boolean,
    val syncState: Int = SyncState.PENDING,
)
