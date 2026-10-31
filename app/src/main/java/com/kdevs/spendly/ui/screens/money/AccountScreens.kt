package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.DayGroups
import com.kdevs.spendly.ui.screens.common.earned
import com.kdevs.spendly.ui.screens.common.inRange
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.common.spent
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Accounts

@Composable
fun AccountTypeScreen(nav: NavController) {
    ScreenScaffold(str(R.string.add_account), { nav.popBackStack() }) {
        Hint(str(R.string.pick_the_kind_of_account_each))
        AccountTypes.forEach { t ->
            Card(onClick = { nav.navigate(Routes.accountEdit("", t.key)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconCircle(t.icon, t.colorKey, 52.dp)
                    Column(Modifier.weight(1f)) {
                        Text(t.label, fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 16.sp)
                        Text(t.blurb, color = Muted, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Faint)
                }
            }
        }
    }
}

@Composable
fun AccountEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
    typeArg: String,
) {
    val accounts by Repo.accounts.collectAsState()
    val existing = remember(id, accounts.isEmpty()) { accounts.firstOrNull { it.id == id } }
    val type = existing?.type ?: typeArg.ifBlank { "bank" }
    val info = AccountTypes.first { it.key == type }
    val isCard = type == "card"
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var opening by rememberSaveable(existing) { mutableStateOf(existing?.let { paiseToField(kotlin.math.abs(it.openingPaise)) } ?: "") }
    var icon by rememberSaveable(existing) { mutableStateOf(existing?.icon ?: info.icon) }
    var color by rememberSaveable(existing) { mutableStateOf(existing?.colorKey ?: info.colorKey) }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    entry.Picked<String>("pickedColor") { color = it }

    ScreenScaffold(
        if (existing == null) str(R.string.spendly_new, info.label.lowercase()) else str(R.string.edit_account),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(if (existing == null) str(R.string.add_account) else str(R.string.save_changes), saver, name.isNotBlank(), onSave = {
                val op = (parseRupees(opening) ?: 0L).let { if (isCard) -it else it }
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "accounts",
                        id,
                        Account(
                            name = name.trim(),
                            type = type,
                            icon = icon,
                            colorKey = color,
                            openingPaise = op,
                            archived = existing?.archived ?: false,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                }
            })
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, str(R.string.account_name))
        AppTextField(
            opening,
            { v ->
                if (v.all {
                        it.isDigit() || it == '.'
                    }
                ) {
                    opening = v
                }
            },
            if (isCard) str(R.string.amount_you_owe_now) else str(R.string.opening_balance),
            keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal,
            leading = { Text(str(R.string.rupee_symbol), fontWeight = FontWeight.Bold) },
        )
        Card(padding = 12.dp) {
            PickRowLeading(str(R.string.icon), iconLabel(icon), {
                IconCircle(icon, color, 38.dp)
            }, { nav.navigate(Routes.iconPicker("account", icon)) })
            PickRowLeading(
                str(R.string.colour),
                colorSpec(color).label,
                { Box(Modifier.size(38.dp).clip(CircleShape).background(colorSpec(color).brush)) },
                { nav.navigate(Routes.colorPicker(color)) },
                divider = false,
            )
        }
        if (isCard) Hint(str(R.string.cards_count_as_money_you_owe))
    }
    if (confirm) {
        ConfirmDialog(str(R.string.remove_this_account), str(R.string.if_it_has_transactions_it_is), str(R.string.remove), {
            saver.run(scope, onDone = { nav.popBackStack(Routes.ACCOUNTS, false) }) { Repo.deleteAccount(id) }
        }, { confirm = false }, danger = true)
    }
}

@Composable
fun AccountDetailScreen(
    nav: NavController,
    id: String,
) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val a = accounts.firstOrNull { it.id == id }
    val mine = txns.filter { it.accountId == id || it.toAccountId == id }
    ScreenScaffold(
        a?.name ?: str(R.string.account),
        { nav.popBackStack() },
        actions = {
            if (a != null) {
                RoundIconButton(
                    Icons.Filled.Edit,
                    str(R.string.edit_account),
                    { nav.navigate(Routes.accountEdit(id)) },
                )
            }
        },
    ) {
        if (a == null) {
            EmptyState(Icons.Filled.SearchOff, str(R.string.not_found), str(R.string.this_account_no_longer_exists))
            return@ScreenScaffold
        }
        val bal = Repo.balanceOf(id, accounts, txns)
        val month = inRange(mine, monthStart(), nextMonthStart())
        HeroCard(brush = colorSpec(a.colorKey).brush) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(iconFor(a.icon), null, tint = Color.White)
                Text(
                    AccountTypes.firstOrNull {
                        it.key == a.type
                    }?.label.orEmpty() + if (a.archived) " · " + str(R.string.archived) else "",
                    color =
                        Color.White.copy(
                            alpha = 0.9f,
                        ),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(formatPaise(bal), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
            Text(
                str(
                    R.string.this_month_2,
                    formatPaise(
                        spent(
                            month.filter {
                                it.accountId == id
                            },
                        ),
                    ),
                    formatPaise(earned(month.filter { it.accountId == id })),
                ),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!a.archived) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { SecondaryButton(str(R.string.add_expense), { nav.navigate(Routes.txnAdd("expense")) }) }
                Box(Modifier.weight(1f)) { SecondaryButton(str(R.string.transfer), { nav.navigate(Routes.TRANSFER) }) }
            }
        }
        SectionTitle(str(R.string.transactions_2))
        if (mine.isEmpty()) {
            Hint(str(R.string.no_transactions_on_this_account_yet))
        } else {
            DayGroups(mine, look, {
                nav.navigate(Routes.txnDetail(it.id))
            })
        }
    }
}
