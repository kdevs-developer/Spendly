package com.kdevs.spendly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.kdevs.spendly.data.local.entity.TxnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao : BaseDao<TxnEntity> {
    /** Live list for the signed-in person. Rows waiting to be deleted on the server are already hidden. */
    @Query("SELECT * FROM txns WHERE uid = :uid AND syncState != 2 ORDER BY dateMillis DESC")
    fun observeAll(uid: String): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txns WHERE uid = :uid AND syncState != 0")
    suspend fun pending(uid: String): List<TxnEntity>

    @Query("SELECT * FROM txns WHERE id = :id")
    suspend fun get(id: String): TxnEntity?

    /** Delete = hide now, remove on the server later, then [remove] the row. */
    @Query("UPDATE txns SET syncState = 2 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE txns SET syncState = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM txns WHERE id = :id")
    suspend fun remove(id: String)

    /** Sign out or account delete: forget this person's rows on the phone. */
    @Query("DELETE FROM txns WHERE uid = :uid")
    suspend fun clear(uid: String)
}
