package com.kdevs.spendly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.kdevs.spendly.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/** One row of [AccountDao.observeBalances]. */
data class AccountBalance(val accountId: String, val balancePaise: Long)

@Dao
interface AccountDao : BaseDao<AccountEntity> {
    /** Live list for the signed-in person. Rows waiting to be deleted on the server are already hidden. */
    @Query("SELECT * FROM accounts WHERE uid = :uid AND syncState != 2 ORDER BY createdAt")
    fun observeAll(uid: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE uid = :uid AND syncState != 0")
    suspend fun pending(uid: String): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun get(id: String): AccountEntity?

    /** Delete = hide now, remove on the server later, then [remove] the row. */
    @Query("UPDATE accounts SET syncState = 2 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE accounts SET syncState = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun remove(id: String)

    /** Sign out or account delete: forget this person's rows on the phone. */
    @Query("DELETE FROM accounts WHERE uid = :uid")
    suspend fun clear(uid: String)

    /**
     * Balance of every account, worked out inside SQLite: opening + income - expense - transfers out + transfers in.
     * Replaces looping over all transactions on every screen draw. Re-emits when accounts or txns change.
     */
    @Query(
        """
        SELECT a.id AS accountId,
               a.openingPaise
               + COALESCE((SELECT SUM(CASE t.kind WHEN 'income' THEN t.amountPaise ELSE -t.amountPaise END)
                           FROM txns t
                           WHERE t.accountId = a.id AND t.uid = a.uid AND t.syncState != 2), 0)
               + COALESCE((SELECT SUM(t.amountPaise)
                           FROM txns t
                           WHERE t.kind = 'transfer' AND t.toAccountId = a.id AND t.uid = a.uid AND t.syncState != 2), 0)
               AS balancePaise
        FROM accounts a
        WHERE a.uid = :uid AND a.syncState != 2
        """,
    )
    fun observeBalances(uid: String): Flow<List<AccountBalance>>
}
