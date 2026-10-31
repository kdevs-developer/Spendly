package com.kdevs.spendly.data.local

/**
 * Where a local row stands against Firestore. Every synced table has a `syncState` column.
 *
 * - [SYNCED]  same as the server.
 * - [PENDING] saved on the phone, not yet pushed (create or edit).
 * - [DELETED] hidden from the app, waiting to be deleted on the server; the row is removed for good after that.
 */
object SyncState {
    const val SYNCED = 0
    const val PENDING = 1
    const val DELETED = 2
}
