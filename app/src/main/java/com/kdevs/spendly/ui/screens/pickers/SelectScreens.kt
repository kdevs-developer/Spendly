package com.kdevs.spendly.ui.screens.pickers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.TxnRow
import com.kdevs.spendly.ui.screens.common.earned
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.common.spent
import com.kdevs.spendly.ui.screens.money.splitPick
import com.kdevs.spendly.ui.state.Selection
import com.kdevs.spendly.ui.state.TxnFilter
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Select and bulk

@Composable
fun SelectScreen(nav: NavController) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val list = remember(txns, look) { txns.filter { TxnFilter.matches(it) { id -> look.categoryName(id) } } }
    LaunchedEffect(Unit) { Selection.clear() }
    val chosen = Selection.ids.toSet()
    ScreenScaffold(
        str(R.string.select_items),
        {
            Selection.clear()
            nav.popBackStack()
        },
        actions = {
            Text(
                if (chosen.size == list.size && list.isNotEmpty()) str(R.string.clear) else str(R.string.select_all),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .clickable {
                            if (chosen.size == list.size) {
                                Selection.clear()
                            } else {
                                Selection.clear()
                                Selection.ids.addAll(list.map { it.id })
                            }
                        }.padding(10.dp),
            )
        },
        bottomBar = {
            BottomActionBar {
                PrimaryButton(
                    if (chosen.isEmpty()) {
                        str(
                            R.string.select_items_to_continue,
                        )
                    } else {
                        str(R.string.actions_for_selected, chosen.size)
                    },
                    {
                        nav.navigate(Routes.BULK)
                    },
                    enabled = chosen.isNotEmpty(),
                )
            }
        },
    ) {
        if (list.isEmpty()) {
            EmptyState(Icons.Filled.SearchOff, str(R.string.nothing_to_select), str(R.string.no_transactions_match_the_current_filt))
        } else {
            Card(padding = 12.dp) {
                list.forEachIndexed { i, t ->
                    TxnRow(t, look, { Selection.toggle(t.id) }, selected = t.id in chosen, showDate = true)
                    if (i < list.lastIndex) HorizontalDivider(color = Line)
                }
            }
        }
    }
}

@Composable
fun BulkScreen(
    nav: NavController,
    entry: NavBackStackEntry,
) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val picked = txns.filter { it.id in Selection.ids }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()

    fun finish(msg: String) {
        Selection.clear()
        nav.popBackStack(Routes.ACTIVITY, false)
    }

    suspend fun updateAll(f: (Txn) -> Txn) {
        picked.forEach { Repo.put("txns", it.id, f(it)) }
    }

    entry.Picked<String>("pickedCategory") { v ->
        val (c, s) = splitPick(v)
        saver.run(
            scope,
            onDone = { finish("") },
        ) { updateAll { if (it.kind == "transfer") it else it.copy(categoryId = c, subcategoryId = s) } }
    }
    entry.Picked<String>("pickedAccount") { a ->
        saver.run(scope, onDone = { finish("") }) { updateAll { if (it.kind == "transfer") it else it.copy(accountId = a) } }
    }
    entry.Picked<Long>("pickedDate") { d ->
        saver.run(scope, onDone = { finish("") }) { updateAll { it.copy(dateMillis = d) } }
    }

    ScreenScaffold(str(R.string.bulk_actions), { nav.popBackStack() }, busy = saver.busy, busyText = str(R.string.updating_transactions)) {
        HeroCard {
            Text(str(R.string.selected, picked.size), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            Text("-" + formatPaise(spent(picked)), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
            if (earned(picked) > 0) {
                Text(
                    str(R.string.plus_amount_income, formatPaise(earned(picked))),
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        saver.error?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold) }
        Card(padding = 12.dp) {
            SettingRow(str(R.string.change_category), icon = Icons.Filled.Category, iconColor = clr(R.color.swatch_purple), onClick = {
                nav.navigate(Routes.categoryPicker("expense", "bulk"))
            })
            SettingRow(str(R.string.change_account), icon = Icons.Filled.AccountBalanceWallet, iconColor = Primary, onClick = {
                nav.navigate(Routes.accountPicker("account"))
            })
            SettingRow(str(R.string.change_date), icon = Icons.Filled.CalendarMonth, iconColor = clr(R.color.swatch_teal), onClick = {
                nav.navigate(Routes.datePicker(todayMillis()))
            })
            SettingRow(
                str(R.string.delete_all),
                icon = Icons.Filled.Delete,
                iconColor = Danger,
                onClick = { confirm = true },
                divider = false,
            )
        }
        Hint(str(R.string.transfers_keep_their_accounts_category))
        SectionTitle(str(R.string.selected_items))
        Card(padding = 12.dp) {
            picked.forEachIndexed {
                i,
                t,
                ->
                TxnRow(t, look, {}, showDate = true)
                if (i < picked.lastIndex) HorizontalDivider(color = Line)
            }
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_transactions, picked.size), str(R.string.this_cannot_be_undone), str(R.string.delete), {
            saver.run(scope, onDone = { finish("") }) { Repo.removeMany("txns", picked.map { it.id }) }
        }, { confirm = false }, danger = true)
    }
}
