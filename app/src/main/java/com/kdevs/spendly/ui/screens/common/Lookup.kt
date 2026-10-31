package com.kdevs.spendly.ui.screens.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.theme.*

/** Looks up names and icons from the live lists. */
class Lookup(val accounts: List<Account>, val categories: List<Category>) {
    fun account(id: String) = accounts.firstOrNull { it.id == id }

    fun category(id: String) = categories.firstOrNull { it.id == id }

    fun accountName(id: String) = account(id)?.name ?: str(R.string.account)

    fun categoryName(id: String) = category(id)?.name ?: str(R.string.uncategorised)

    fun subs(parentId: String) = categories.filter { it.parentId == parentId }

    fun parents(kind: String) = categories.filter { it.parentId.isBlank() && it.kind == kind }
}

@Composable
fun rememberLookup(): Lookup {
    val a by Repo.accounts.collectAsState()
    val c by Repo.categories.collectAsState()
    return remember(a, c) { Lookup(a, c) }
}
