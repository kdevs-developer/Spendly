package com.kdevs.spendly.ui.screens.splits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.kdevs.spendly.ui.screens.common.TickBox
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.money.PickRow
import com.kdevs.spendly.ui.screens.money.PickRowLeading
import com.kdevs.spendly.ui.screens.money.SavedBar
import com.kdevs.spendly.ui.screens.money.splitPick
import com.kdevs.spendly.ui.theme.*

@Composable
fun AddSplitScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val me = Repo.uid
    val chat = rememberChat(id)
    var desc by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var paidBy by rememberSaveable { mutableStateOf(me) }
    var date by rememberSaveable { mutableStateOf(todayMillis()) }
    var type by rememberSaveable { mutableStateOf(0) } // 0 equal, 1 exact, 2 percent, 3 shares
    var included by rememberSaveable(chat?.members) { mutableStateOf(chat?.members?.toSet() ?: emptySet()) }
    var values by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var payerMenu by remember { mutableStateOf(false) }
    // rememberSaveable: the account picker is a separate screen, so plain remember would reset these on return.
    var recordShare by rememberSaveable { mutableStateOf(false) }
    var shareAccount by rememberSaveable { mutableStateOf("") }
    var catId by rememberSaveable { mutableStateOf("") }
    var subId by rememberSaveable { mutableStateOf("") }
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    LaunchedEffect(accounts) { if (shareAccount.isBlank()) shareAccount = accounts.firstOrNull { !it.archived }?.id ?: "" }
    entry.Picked<Long>("pickedDate") { date = it }
    entry.Picked<String>("pickedAccount") { shareAccount = it }
    entry.Picked<String>("pickedCategory") { v ->
        splitPick(v).let {
            catId = it.first
            subId = it.second
        }
    }

    val members = chat?.members.orEmpty()
    val total = parseRupees(amount) ?: 0L
    val ids = members.filter { it in included }

    // Work out what each person owes for the chosen split type.
    val owed: Map<String, Long>? =
        when (type) {
            0 -> SplitMath.equal(total, ids)
            1 -> ids.associateWith { parseRupees(values[it].orEmpty()) ?: 0L }.takeIf { it.values.sum() == total }
            2 ->
                ids
                    .associateWith { ((values[it]?.toDoubleOrNull() ?: 0.0) * 100).toInt() }
                    .takeIf { m ->
                        m.values.sum() == 10000
                    }?.let { SplitMath.percent(total, it) }
            else ->
                ids
                    .associateWith { values[it]?.toIntOrNull() ?: 0 }
                    .takeIf { m ->
                        m.values.sum() > 0
                    }?.let { SplitMath.shares(total, it) }
        }
    val problem =
        when {
            total <= 0 -> null
            ids.isEmpty() -> str(R.string.choose_at_least_one_person)
            owed == null && type == 1 ->
                str(
                    R.string.amounts_must_add_up_to_now,
                    formatPaise(total),
                    formatPaise(
                        ids.sumOf {
                            parseRupees(values[it].orEmpty()) ?: 0L
                        },
                    ),
                )
            owed == null && type == 2 -> str(R.string.percentages_must_add_up_to_100)
            owed == null -> str(R.string.give_at_least_one_share)
            else -> null
        }
    // Only the person who paid has money leaving their account. The full amount goes in, so the account matches
    // reality; the others' settle-up payments then come back as income. If someone else paid, the settle-up
    // itself records your expense, so recording it here too would count it twice.
    val record = recordShare && paidBy == me
    val ok = total > 0 && desc.isNotBlank() && owed != null && ids.isNotEmpty() && (!record || shareAccount.isNotBlank())
    val cat = look.category(subId.ifBlank { catId })
    val parent = look.category(catId)

    ScreenScaffold(str(R.string.add_group_expense), { nav.popBackStack() }, bottomBar = {
        SavedBar(str(R.string.save_expense), saver, ok, onSave = {
            saver.run(scope, onDone = { nav.popBackStack() }) {
                Repo.addSplit(
                    id,
                    SplitEntry(
                        kind = "expense",
                        description = desc.trim(),
                        totalPaise = total,
                        paidBy = paidBy,
                        splitType = listOf("equal", "exact", "percent", "shares")[type],
                        owed = owed!!.filterValues { it > 0 || type == 0 },
                        dateMillis = date,
                        categoryId = catId,
                        subcategoryId = subId,
                    ),
                )
                // You paid the bill, so the full amount leaves your account. Also shows up in your transactions.
                if (record) {
                    Repo.put(
                        "txns",
                        "",
                        Txn(
                            kind = "expense",
                            amountPaise = total,
                            accountId = shareAccount,
                            categoryId = catId,
                            subcategoryId = subId,
                            note = desc.trim(),
                            dateMillis = withNowTime(date),
                        ),
                    )
                }
            }
        })
    }) {
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 6.dp), color = Ink)
        AppTextField(desc, { desc = it }, str(R.string.what_was_it_for))
        Card(padding = 12.dp) {
            PickRowLeading(
                str(R.string.category),
                if (cat == null) {
                    ""
                } else {
                    parent?.name?.let { p -> if (subId.isNotBlank()) "$p · ${cat.name}" else p } ?: cat.name
                },
                leading = { IconCircle(cat?.icon ?: "other", parent?.colorKey ?: cat?.colorKey ?: "slate", 38.dp) },
                onClick = { nav.navigate(Routes.categoryPicker("expense")) },
            )
            PickRow(str(R.string.paid_by), chat?.nameOf(paidBy, me) ?: "", Icons.Filled.Person, Primary, { payerMenu = !payerMenu })
            if (payerMenu) {
                members.forEach { u ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                paidBy = u
                                payerMenu = false
                            }.padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Avatar(chat?.nameOf(u, me) ?: "?", colorKeyFor(u), 30.dp)
                        Text(chat?.nameOf(u, me) ?: "", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Ink)
                        if (u == paidBy) Icon(Icons.Filled.Check, null, tint = Primary)
                    }
                }
            }
            PickRow(
                str(R.string.date),
                fullDate(date),
                Icons.Filled.CalendarMonth,
                Muted,
                { nav.navigate(Routes.datePicker(date)) },
                divider = false,
            )
        }
        SegmentedControl(listOf(str(R.string.equal), str(R.string.exact), str(R.string.percent), str(R.string.shares)), type, {
            type = it
            values = emptyMap()
        })
        Card(padding = 12.dp) {
            members.forEachIndexed { i, u ->
                val on = u in included
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.clip(CircleShape).clickable { included = if (on) included - u else included + u }) { TickBox(on) }
                    Avatar(chat?.nameOf(u, me) ?: "?", colorKeyFor(u), 34.dp)
                    Text(chat?.nameOf(u, me) ?: "", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Ink)
                    if (on && type != 0) {
                        Box(Modifier.width(96.dp)) {
                            AppTextField(
                                values[u].orEmpty(),
                                { v -> values = values + (u to v) },
                                when (type) {
                                    1 -> "₹"
                                    2 -> "%"
                                    else -> "x"
                                },
                                keyboard = if (type == 3) KeyboardType.Number else KeyboardType.Decimal,
                            )
                        }
                    } else if (on) {
                        Text(formatPaise(owed?.get(u) ?: 0L), fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                }
                if (i < members.lastIndex) HorizontalDivider(color = Line)
            }
        }
        if (paidBy == me) {
            Card(padding = 12.dp) {
                SettingRow(
                    str(R.string.add_my_share),
                    if (total > 0) formatPaise(total) else "",
                    icon = Icons.Filled.AccountBalanceWallet,
                    iconColor = Primary,
                    trailing = { Switch(recordShare, { recordShare = it }) },
                    divider = recordShare,
                )
                if (recordShare) {
                    PickRowLeading(
                        str(R.string.account),
                        look.account(shareAccount)?.name ?: "",
                        { IconCircle(look.account(shareAccount)?.icon ?: "bank", look.account(shareAccount)?.colorKey ?: "blue", 38.dp) },
                        { nav.navigate(Routes.accountPicker("account")) },
                        divider = false,
                    )
                }
            }
            if (recordShare && total > 0) Hint(str(R.string.add_my_share_hint, formatPaise(total)))
        } else {
            Hint(str(R.string.other_paid_hint, chat?.nameOf(paidBy, me) ?: ""))
        }
        if (type in 2..3 && owed != null) Hint(ids.joinToString("  ·  ") { "${chat?.nameOf(it, me)} ${formatPaise(owed[it] ?: 0L)}" })
        problem?.let { Text(it, color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        if (type == 0 && total > 0 && ids.isNotEmpty()) {
            Hint(
                str(R.string.each_any_leftover_paise_go_one, formatPaise(total), ids.size, formatPaise(total / ids.size)),
            )
        }
    }
}
