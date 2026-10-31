package com.kdevs.spendly.data.local

import com.kdevs.spendly.data.local.entity.*
import com.kdevs.spendly.data.model.*

// Models (data/model) stay the shape the screens already use. Entities are the Room rows.
// toEntity needs the owner and a non-blank id: the repository creates ids with Firestore's auto-id style.

fun AccountEntity.toModel() = Account(id, name, type, icon, colorKey, openingPaise, archived, createdAt)

fun Account.toEntity(
    uid: String,
    state: Int = SyncState.PENDING,
) = AccountEntity(id, uid, name, type, icon, colorKey, openingPaise, archived, createdAt, state)

fun CategoryEntity.toModel() = Category(id, name, icon, colorKey, parentId, kind)

fun Category.toEntity(
    uid: String,
    state: Int = SyncState.PENDING,
) = CategoryEntity(id, uid, name, icon, colorKey, parentId, kind, state)

fun TxnEntity.toModel() = Txn(id, kind, amountPaise, accountId, toAccountId, categoryId, subcategoryId, note, dateMillis, recurringId)

fun Txn.toEntity(
    uid: String,
    state: Int = SyncState.PENDING,
) = TxnEntity(id, uid, kind, amountPaise, accountId, toAccountId, categoryId, subcategoryId, note, dateMillis, state, recurringId)

fun BudgetEntity.toModel() = Budget(id, categoryId, limitPaise)

fun Budget.toEntity(
    uid: String,
    state: Int = SyncState.PENDING,
) = BudgetEntity(id, uid, categoryId, limitPaise, state)

fun GoalEntity.toModel() = Goal(id, name, icon, colorKey, targetPaise, savedPaise, deadlineMillis)

fun Goal.toEntity(
    uid: String,
    state: Int = SyncState.PENDING,
) = GoalEntity(id, uid, name, icon, colorKey, targetPaise, savedPaise, deadlineMillis, state)

fun TemplateEntity.toModel() = Template(id, name, kind, amountPaise, accountId, categoryId, note)

fun Template.toEntity(
    uid: String,
    state: Int = SyncState.PENDING,
) = TemplateEntity(id, uid, name, kind, amountPaise, accountId, categoryId, note, state)

// Recurring has grown (subcategoryId, interval, anchorDay), so these use named arguments:
// positional ones break whenever a field is added in the middle of the class.
fun RecurringEntity.toModel() =
    Recurring(
        id = id,
        name = name,
        kind = kind,
        amountPaise = amountPaise,
        accountId = accountId,
        categoryId = categoryId,
        subcategoryId = subcategoryId,
        every = every,
        interval = interval,
        nextMillis = nextMillis,
        active = active,
        anchorDay = anchorDay,
    )