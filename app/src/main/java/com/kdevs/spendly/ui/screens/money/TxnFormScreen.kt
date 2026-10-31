package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
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
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Transactions

@Composable
fun TxnFormScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
    kind: String,
) {
    val txns by Repo.txns.collectAsState()
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val ctx = LocalContext.current
    val existing = remember(id, txns.isEmpty()) { txns.firstOrNull { it.id == id } }
    val editing = id.isNotBlank()

    var type by rememberSaveable(existing) { mutableStateOf(existing?.kind ?: kind) }
    var amount by rememberSaveable(existing) { mutableStateOf(existing?.let { paiseToField(it.amountPaise) } ?: "") }
    var accountId by rememberSaveable(existing) { mutableStateOf(existing?.accountId ?: "") }
    var catId by rememberSaveable(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var subId by rememberSaveable(existing) { mutableStateOf(existing?.subcategoryId ?: "") }
    var note by rememberSaveable(existing) { mutableStateOf(existing?.note ?: "") }
    var date by rememberSaveable(existing) { mutableStateOf(existing?.dateMillis ?: nowMillis()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()

    // Last account used for this type, if it still exists and is not archived; otherwise the first active one.
    fun defaultAccount(forType: String): String {
        val last = RecentPicks.lastAccount(ctx, forType)
        return accounts.firstOrNull { !it.archived && it.id == last }?.id
            ?: accounts.firstOrNull { !it.archived }?.id
            ?: ""
    }

    LaunchedEffect(accounts) { if (accountId.isBlank()) accountId = defaultAccount(type) }
    entry.Picked<String>("pickedAccount") { accountId = it }
    entry.Picked<String>("pickedCategory") { v ->
        splitPick(v).let {
            catId = it.first
            subId = it.second
        }
    }
    entry.Picked<Long>("pickedDate") { date = withTimeOf(it, date) }
    entry.Picked<Int>("pickedTime") { date = withMinutes(date, it) }

    val paise = parseRupees(amount) ?: 0L
    val cat = look.category(subId.ifBlank { catId })
    val parent = look.category(catId)
    val acc = look.account(accountId)
    val tint by androidx.compose.animation.animateColorAsState(
        if (type == "income") Success else Danger,
        androidx.compose.animation.core
            .tween(220),
        label = "txnTint",
    )

    // Recent category + sub-category pairs for the current type (new transactions only).
    val recents =
        remember(type, look.categories) {
            RecentPicks
                .categories(ctx, type)
                .mapNotNull { (c, s) ->
                    val p = look.category(c) ?: return@mapNotNull null
                    val sc = if (s.isBlank()) null else (look.category(s) ?: return@mapNotNull null)
                    QuickItem(c, s, sc?.name ?: p.name, sc?.icon ?: p.icon, p.colorKey)
                }.take(6)
        }

    ScreenScaffold(
        if (editing) str(R.string.edit_transaction) else str(R.string.add_transaction),
        { nav.popBackStack() },
        actions = { if (editing) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirmDelete = true }, tint = Danger) },
        bottomBar = {
            SavedBar(
                if (editing) {
                    str(
                        R.string.save_changes,
                    )
                } else {
                    str(R.string.save)
                },
                saver,
                enabled = paise > 0 && accountId.isNotBlank(),
                onSave = {
                    saver.run(scope, onDone = { nav.popBackStack() }) {
                        Repo.put(
                            "txns",
                            id,
                            Txn(
                                kind = type,
                                amountPaise = paise,
                                accountId = accountId,
                                categoryId = catId,
                                subcategoryId = subId,
                                note = note.trim(),
                                dateMillis = date,
                                recurringId = existing?.recurringId ?: "", // editing keeps the link to the recurring item
                            ),
                        )
                        if (!editing) RecentPicks.remember(ctx, type, accountId, catId, subId)
                    }
                },
            )
        },
        header = {
            SegmentedControl(listOf(str(R.string.expense), str(R.string.income)), if (type == "income") 1 else 0, {
                type = if (it == 1) "income" else "expense"
                catId = ""
                subId = ""
                if (!editing) accountId = defaultAccount(type)
            })
            AmountField(amount, { amount = it }, color = tint)
        },
    ) {
        if (!editing && recents.isNotEmpty()) {
            Text(str(R.string.recent_categories), color = Muted, fontSize = 12.sp)
            QuickCategories(
                items = recents,
                selectedCat = catId,
                selectedSub = subId,
                onPick = {
                    catId = it.catId
                    subId = it.subId
                },
                onMore = { nav.navigate(Routes.categoryPicker(type)) },
            )
        }
        Card(padding = 12.dp) {
            PickRowLeading(
                str(R.string.category),
                if (cat == null) {
                    ""
                } else {
                    (
                        parent?.name?.let { p ->
                            if (subId.isNotBlank()) "$p · ${cat.name}" else p
                        } ?: cat.name
                    )
                },
                leading = { IconCircle(cat?.icon ?: "other", parent?.colorKey ?: cat?.colorKey ?: "slate", 38.dp) },
                onClick = { nav.navigate(Routes.categoryPicker(type)) },
            )
            PickRowLeading(
                str(R.string.account),
                acc?.name ?: "",
                leading = { IconCircle(acc?.icon ?: "bank", acc?.colorKey ?: "blue", 38.dp) },
                onClick = { nav.navigate(Routes.accountPicker("account")) },
            )
            PickRow(str(R.string.date), dayLabel(date) + ", " + fullDate(date), Icons.Filled.CalendarMonth, Muted, {
                nav.navigate(Routes.datePicker(date))
            })
            PickRow(
                str(R.string.time),
                if (hasTime(date)) timeOf(date) else str(R.string.add_time),
                Icons.Filled.Schedule,
                Muted,
                { nav.navigate(Routes.timePicker(minutesOfDay(date))) },
                divider = false,
            )
        }
        AppTextField(note, { note = it }, str(R.string.note_optional), singleLine = false, minLines = 2)
    }
    if (confirmDelete) {
        ConfirmDialog(str(R.string.delete_transaction), str(R.string.this_cannot_be_undone), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("txns", id) }
        }, { confirmDelete = false }, danger = true)
    }
}

@Composable
fun TransferScreen(
    nav: NavController,
    entry: NavBackStackEntry,
) {
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(nowMillis()) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    LaunchedEffect(accounts) {
        val live = accounts.filter { !it.archived }
        if (from.isBlank()) from = live.getOrNull(0)?.id ?: ""
        if (to.isBlank()) to = live.getOrNull(1)?.id ?: ""
    }
    entry.Picked<String>("pickedAccount") { from = it }
    entry.Picked<String>("pickedAccount2") { to = it }
    entry.Picked<Long>("pickedDate") { date = withTimeOf(it, date) }
    entry.Picked<Int>("pickedTime") { date = withMinutes(date, it) }
    val paise = parseRupees(amount) ?: 0L
    val ok = paise > 0 && from.isNotBlank() && to.isNotBlank() && from != to
    ScreenScaffold(str(R.string.transfer), { nav.popBackStack() }, bottomBar = {
        SavedBar(str(R.string.transfer), saver, ok, onSave = {
            saver.run(scope, onDone = { nav.popBackStack() }) {
                Repo.put(
                    "txns",
                    "",
                    Txn(kind = "transfer", amountPaise = paise, accountId = from, toAccountId = to, note = note.trim(), dateMillis = date),
                )
            }
        })
    }, header = {
        AmountField(amount, { amount = it }, color = Primary)
    }) {
        Card(padding = 12.dp) {
            val f = look.account(from)
            val t = look.account(to)
            PickRowLeading(str(R.string.from), f?.name ?: "", {
                IconCircle(f?.icon ?: "bank", f?.colorKey ?: "blue", 38.dp)
            }, { nav.navigate(Routes.accountPicker("account", to)) })
            PickRowLeading(str(R.string.to_2), t?.name ?: "", {
                IconCircle(t?.icon ?: "bank", t?.colorKey ?: "green", 38.dp)
            }, { nav.navigate(Routes.accountPicker("to", from)) })
            PickRow(
                str(R.string.date),
                fullDate(date),
                Icons.Filled.CalendarMonth,
                Muted,
                { nav.navigate(Routes.datePicker(date)) },
            )
            PickRow(
                str(R.string.time),
                if (hasTime(date)) timeOf(date) else str(R.string.add_time),
                Icons.Filled.Schedule,
                Muted,
                { nav.navigate(Routes.timePicker(minutesOfDay(date))) },
                divider = false,
            )
        }
        AppTextField(note, { note = it }, str(R.string.note_optional))
        if (from.isNotBlank() && from == to) {
            Text(
                str(R.string.choose_two_different_accounts),
                color = Danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
