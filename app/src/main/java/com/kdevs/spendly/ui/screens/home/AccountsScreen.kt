package com.kdevs.spendly.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Accounts

@Composable
fun AccountsScreen(nav: NavController) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val live = accounts.filter { !it.archived }
    // One pass over the transactions for every account, redone only when the data changes (not on each redraw).
    val balances = remember(accounts, txns) { Repo.balances(accounts, txns) }
    val total = live.sumOf { balances[it.id] ?: 0L }

    TabScreen(
        str(R.string.accounts),
        Routes.ACCOUNTS,
        nav,
        fab = { Fab(Icons.Filled.Add, str(R.string.add_account)) { nav.navigate(Routes.ACCOUNT_TYPE) } },
    ) {
        HeroCard {
            Text(str(R.string.net_worth), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(formatPaise(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
            Text(str(R.string.accounts_2, live.size), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
        if (live.isEmpty()) EmptyState(Icons.Filled.AccountBalance, str(R.string.no_accounts_yet), str(R.string.add_a_bank_wallet_cash_or))
        AccountTypes.forEach { type ->
            val group = live.filter { it.type == type.key }
            if (group.isNotEmpty()) {
                SectionTitle(type.label)
                Card(padding = 12.dp) {
                    group.forEachIndexed { i, a ->
                        val bal = balances[a.id] ?: 0L
                        SettingRow(
                            a.name,
                            leading = { IconCircle(a.icon, a.colorKey, 44.dp) },
                            onClick = { nav.navigate(Routes.accountDetail(a.id)) },
                            trailing = { Text(formatPaise(bal), fontWeight = FontWeight.ExtraBold, color = if (bal < 0) Danger else Ink) },
                            divider = i < group.lastIndex,
                        )
                    }
                }
            }
        }
        val archived = accounts.filter { it.archived }
        if (archived.isNotEmpty()) {
            SectionTitle(str(R.string.archived))
            Card(padding = 12.dp) {
                archived.forEachIndexed { i, a ->
                    SettingRow(a.name, leading = {
                        IconCircle(a.icon, "slate", 44.dp)
                    }, onClick = { nav.navigate(Routes.accountDetail(a.id)) }, divider = i < archived.lastIndex)
                }
            }
        }
        Spacer(Modifier.height(70.dp))
    }
}
