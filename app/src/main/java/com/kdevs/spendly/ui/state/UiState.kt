package com.kdevs.spendly.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.kdevs.spendly.data.model.Txn

/** Multi-select used by the Select screen and Bulk actions screen. */
object Selection {
    val ids = mutableStateListOf<String>()

    fun toggle(id: String) {
        if (id in ids) ids.remove(id) else ids.add(id)
    }

    fun clear() = ids.clear()
}

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

/** Period and accounts chosen on the Report filter screen. period: month | last30 | year | custom */
object ReportFilter {
    var period by mutableStateOf("month")
    var customFrom by mutableStateOf(0L)
    var customTo by mutableStateOf(0L)
    var accountIds by mutableStateOf(setOf<String>())
    var kind by mutableStateOf("expense")
}

/**
 * Pickers send their result back to the previous screen through the saved state handle.
 * Keys: pickedColor, pickedIcon, pickedAccount, pickedAccount2, pickedCategory, pickedSub, pickedDate.
 */
fun NavController.setPicked(
    key: String,
    value: Any?,
) {
    previousBackStackEntry?.savedStateHandle?.set(key, value)
}
