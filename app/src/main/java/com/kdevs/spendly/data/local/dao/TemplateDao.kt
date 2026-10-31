package com.kdevs.spendly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.kdevs.spendly.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao : BaseDao<TemplateEntity> {
    /** Live list for the signed-in person. Rows waiting to be deleted on the server are already hidden. */
    @Query("SELECT * FROM templates WHERE uid = :uid AND syncState != 2 ORDER BY name")
    fun observeAll(uid: String): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE uid = :uid AND syncState != 0")
    suspend fun pending(uid: String): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun get(id: String): TemplateEntity?

    /** Delete = hide now, remove on the server later, then [remove] the row. */
    @Query("UPDATE templates SET syncState = 2 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE templates SET syncState = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun remove(id: String)

    /** Sign out or account delete: forget this person's rows on the phone. */
    @Query("DELETE FROM templates WHERE uid = :uid")
    suspend fun clear(uid: String)
}
