package com.kdevs.spendly.data.local.dao

import androidx.room.Upsert

/** Insert-or-update, shared by every DAO. Room generates the SQL for T. */
interface BaseDao<T> {
    @Upsert suspend fun upsert(item: T)

    @Upsert suspend fun upsertAll(items: List<T>)
}
