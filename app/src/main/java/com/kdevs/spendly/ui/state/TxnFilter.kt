package com.kdevs.spendly.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kdevs.spendly.data.model.Txn

/** Filter chosen on the Filter screen, applied by the Activity list. */
object TxnFilter {
    var query by mutableStateOf("")
    var kinds by mutableStateOf(setOf<String>()) // empty = all
    var accountIds by mutableStateOf(setOf<String>())
    var categoryIds by mutableStateOf(setOf<String>())
    var fromMillis by mutableStateOf(0L) // 0 = no limit
    var toMillis by mutableStateOf(0L)
    var minPaise by mutableStateOf(0L)
    var maxPaise by mutableStateOf(0L)

    val activeCount: Int get() =
        listOf(
            kinds.isNotEmpty(),
            accountIds.isNotEmpty(),
            categoryIds.isNotEmpty(),
            fromMillis != 0L || toMillis != 0L,
            minPaise != 0L || maxPaise != 0L,
        ).count { it }

    fun reset() {
        query = ""
        kinds = emptySet()
        accountIds = emptySet()
        categoryIds = emptySet()
        fromMillis = 0
        toMillis = 0
        minPaise = 0
        maxPaise = 0
    }

    fun matches(
        t: Txn,
        catName: (String) -> String,
    ): Boolean {
        if (kinds.isNotEmpty() && t.kind !in kinds) return false
        if (accountIds.isNotEmpty() && t.accountId !in accountIds && t.toAccountId !in accountIds) return false
        if (categoryIds.isNotEmpty() && t.categoryId !in categoryIds && t.subcategoryId !in categoryIds) return false
        if (fromMillis != 0L && t.dateMillis < fromMillis) return false
        if (toMillis != 0L && t.dateMillis >= toMillis) return false
        if (minPaise != 0L && t.amountPaise < minPaise) return false
        if (maxPaise != 0L && t.amountPaise > maxPaise) return false
        val q = query.trim()
        if (q.isNotEmpty() && !(t.note.contains(q, true) || catName(t.categoryId).contains(q, true))) return false
        return true
    }
}
