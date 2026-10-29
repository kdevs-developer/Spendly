package com.kdevs.spendly.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.kdevs.spendly.ui.theme.*

@Composable
private fun rememberChat(id: String): Chat? {
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    return chat
}

@Composable
private fun rememberSplits(id: String): List<SplitEntry> {
    val l by remember(id) { Repo.splits(id) }.collectAsState(initial = emptyList())
    return l
}

private fun Chat.nameOf(
    uid: String,
    me: String,
) = if (uid == me) str(R.string.you) else memberNames[uid] ?: str(R.string.member)

@Composable
fun SplitsScreen(
    nav: NavController,
    id: String,
) {
    val me = Repo.uid
    val chat = rememberChat(id)
    val entries = rememberSplits(id)
    val saver = rememberSaver()
    val ctx = LocalContext.current
    val balances = SplitMath.balances(entries)
    val mine = balances[me] ?: 0L
    val plan = SplitMath.simplify(balances)

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(chat?.name ?: str(R.string.splits), { nav.popBackStack() }) {
            HeroCard(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(clr(R.color.swatch_teal), clr(R.color.swatch_teal_dark))),
            ) {
                Text(
                    if (mine == 0L) {
                        str(R.string.all_settled_up)
                    } else if (mine > 0) {
                        str(R.string.you_are_owed)
                    } else {
                        str(R.string.you_owe)
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(formatPaise(kotlin.math.abs(mine)), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
                Spacer(Modifier.height(10.dp))
                chat?.members?.forEach { u ->
                    val b = balances[u] ?: 0L
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            chat?.nameOf(u, me) ?: "",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(formatPaise(b, withSign = true), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            if (plan.isNotEmpty()) {
                SectionTitle(str(R.string.to_settle_up))
                Card(padding = 12.dp) {
                    plan.forEachIndexed { i, t ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Avatar(chat?.nameOf(t.from, me) ?: "?", colorKeyFor(t.from), 34.dp)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Faint, modifier = Modifier.size(18.dp))
                            Avatar(chat?.nameOf(t.to, me) ?: "?", colorKeyFor(t.to), 34.dp)
                            Text(
                                str(R.string.pays, chat?.nameOf(t.from, me), chat?.nameOf(t.to, me)),
                                Modifier.weight(1f),
                                color = Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(formatPaise(t.paise), fontWeight = FontWeight.ExtraBold, color = Ink)
                        }
                        if (i < plan.lastIndex) HorizontalDivider(color = Line)
                    }
                }
                SecondaryButton(str(R.string.settle_up), { nav.navigate(Routes.settle(id)) })
            }
            SectionTitle(str(R.string.expenses))
            saver.error?.let { Text(it, color = Danger) }
            if (entries.isEmpty()) {
                EmptyState(Icons.Filled.Receipt, str(R.string.no_expenses_yet), str(R.string.add_the_first_shared_expense))
            } else {
                Card(padding = 12.dp) {
                    entries.forEachIndexed { i, e ->
                        val settle = e.kind == "settlement"
                        SettingRow(
                            if (settle) "${chat?.nameOf(e.paidBy, me)} paid ${chat?.nameOf(e.settleTo, me)}" else e.description,
                            str(R.string.text__2, fullDate(e.dateMillis)) + if (!settle) " · paid by ${chat?.nameOf(e.paidBy, me)}" else "",
                            icon = if (settle) Icons.Filled.CheckCircle else Icons.Filled.Receipt,
                            iconColor = if (settle) Success else clr(R.color.swatch_teal),
                            trailing = { Text(formatPaise(e.totalPaise), fontWeight = FontWeight.ExtraBold, color = Ink) },
                            onClick = { nav.navigate(Routes.splitEntry(id, e.id)) },
                            divider = i < entries.lastIndex,
                        )
                    }
                }
            }
            if (entries.isNotEmpty()) {
                Hint("Tap an entry to see who owes what.")
                SecondaryButton("Share summary", {
                    val text =
                        buildString {
                            append(chat?.name?.ifBlank { null } ?: "Spendly split").append("\n")
                            if (plan.isEmpty()) {
                                append("All settled up.")
                            } else {
                                plan.forEach { t -> append("${chat?.nameOf(t.from, me)} pays ${chat?.nameOf(t.to, me)} ${formatPaise(t.paise)}\n") }
                            }
                        }.trim()
                    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                    ctx.startActivity(Intent.createChooser(i, "Share summary"))
                })
            }
            Spacer(Modifier.height(80.dp))
        }
        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) {
            Fab(Icons.Filled.Add, str(R.string.add_expense)) { nav.navigate(Routes.addSplit(id)) }
        }
    }
}

/** One expense or payment: who paid, the split type, and what each person owes. The person who added it can delete it. */
@Composable
fun SplitEntryScreen(
    nav: NavController,
    chatId: String,
    entryId: String,
) {
    val me = Repo.uid
    val chat = rememberChat(chatId)
    val entries = rememberSplits(chatId)
    val e = entries.firstOrNull { it.id == entryId }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    ScreenScaffold(if (e?.kind == "settlement") "Payment" else "Expense", { nav.popBackStack() }) {
        if (e == null) {
            EmptyState(Icons.Filled.SearchOff, str(R.string.not_found), "This entry was deleted.")
            return@ScreenScaffold
        }
        val settle = e.kind == "settlement"
        HeroCard(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(clr(R.color.swatch_teal), clr(R.color.swatch_teal_dark)))) {
            Text(
                if (settle) "${chat?.nameOf(e.paidBy, me)} paid ${chat?.nameOf(e.settleTo, me)}" else e.description,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(formatPaise(e.totalPaise), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
        }
        Card(padding = 12.dp) {
            if (!settle) SettingRow("Paid by", chat?.nameOf(e.paidBy, me) ?: "", icon = Icons.Filled.Person, iconColor = Primary)
            SettingRow(
                str(R.string.date),
                fullDate(e.dateMillis),
                icon = Icons.Filled.CalendarMonth,
                iconColor = Muted,
                divider = !settle,
            )
            if (!settle) {
                SettingRow(
                    "Split",
                    when (e.splitType) {
                        "exact" -> "Exact amounts"
                        "percent" -> "By percentage"
                        "shares" -> "By shares"
                        else -> "Equally"
                    },
                    icon = Icons.Filled.CallSplit,
                    iconColor = clr(R.color.swatch_teal),
                    divider = false,
                )
            }
        }
        if (!settle && e.owed.isNotEmpty()) {
            SectionTitle("Who owes what")
            Card(padding = 12.dp) {
                val list = e.owed.entries.toList()
                list.forEachIndexed { i, (u, p) ->
                    SettingRow(
                        chat?.nameOf(u, me) ?: "",
                        leading = { Avatar(chat?.nameOf(u, me) ?: "?", colorKeyFor(u), 38.dp) },
                        trailing = { Text(formatPaise(p), fontWeight = FontWeight.ExtraBold, color = Ink) },
                        divider = i < list.lastIndex,
                    )
                }
            }
        }
        saver.error?.let { Text(it, color = Danger) }
        if (e.createdBy == me) {
            SecondaryButton(str(R.string.delete), { confirm = true }, danger = true)
        } else {
            Hint("Only the person who added this can delete it.", center = true)
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_this_entry), str(R.string.balances_will_be_recalculated), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.deleteSplit(chatId, entryId) }
        }, { confirm = false }, danger = true)
    }
}

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
    var recordShare by remember { mutableStateOf(false) }
    var shareAccount by rememberSaveable { mutableStateOf("") }
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    LaunchedEffect(accounts) { if (shareAccount.isBlank()) shareAccount = accounts.firstOrNull { !it.archived }?.id ?: "" }
    entry.Picked<Long>("pickedDate") { date = it }
    entry.Picked<String>("pickedAccount") { shareAccount = it }

    val members = chat?.members.orEmpty()
    val total = parseRupees(amount) ?: 0L
    val ids = members.filter { it in included }

    // Work out what each person owes for the chosen split type.
    val owed: Map<String, Long>? =
        when (type) {
            0 -> SplitMath.equal(total, ids)
            1 -> ids.associateWith { parseRupees(values[it].orEmpty()) ?: 0L }.takeIf { it.values.sum() == total }
            2 ->
                ids.associateWith { ((values[it]?.toDoubleOrNull() ?: 0.0) * 100).toInt() }.takeIf {
                        m ->
                    m.values.sum() == 10000
                }?.let { SplitMath.percent(total, it) }
            else ->
                ids.associateWith { values[it]?.toIntOrNull() ?: 0 }.takeIf {
                        m ->
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
    val myShare = owed?.get(me) ?: 0L
    val ok = total > 0 && desc.isNotBlank() && owed != null && ids.isNotEmpty() && (!recordShare || shareAccount.isNotBlank())

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
                    ),
                )
                // Your own share also goes into your expenses, so your balances and reports include it.
                if (recordShare && myShare > 0) {
                    Repo.put(
                        "txns",
                        "",
                        Txn(kind = "expense", amountPaise = myShare, accountId = shareAccount, note = desc.trim(), dateMillis = date),
                    )
                }
            }
        })
    }) {
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 6.dp), color = Ink)
        AppTextField(desc, { desc = it }, str(R.string.what_was_it_for))
        Card(padding = 12.dp) {
            PickRow(str(R.string.paid_by), chat?.nameOf(paidBy, me) ?: "", Icons.Filled.Person, Primary, { payerMenu = !payerMenu })
            if (payerMenu) {
                members.forEach { u ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
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
        Card(padding = 12.dp) {
            SettingRow(
                "Add my share to my expenses",
                if (myShare > 0) formatPaise(myShare) else "",
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
        if (type in 2..3 && owed != null) Hint(ids.joinToString("  ·  ") { "${chat?.nameOf(it, me)} ${formatPaise(owed[it] ?: 0L)}" })
        problem?.let { Text(it, color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        if (type == 0 && total > 0 && ids.isNotEmpty()) {
            Hint(
                str(R.string.each_any_leftover_paise_go_one, formatPaise(total), ids.size, formatPaise(total / ids.size)),
            )
        }
    }
}

@Composable
fun SettleUpScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val me = Repo.uid
    val chat = rememberChat(id)
    val entries = rememberSplits(id)
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val plan = SplitMath.simplify(SplitMath.balances(entries)).filter { it.from == me || it.to == me }
    var sel by remember(plan.size) { mutableIntStateOf(0) }
    val t = plan.getOrNull(sel)
    var amount by rememberSaveable(t) { mutableStateOf(t?.let { paiseToField(it.paise) } ?: "") }
    var accountId by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(todayMillis()) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    LaunchedEffect(accounts) { if (accountId.isBlank()) accountId = accounts.firstOrNull { !it.archived }?.id ?: "" }
    entry.Picked<String>("pickedAccount") { accountId = it }
    entry.Picked<Long>("pickedDate") { date = it }

    val paise = parseRupees(amount) ?: 0L
    val iPay = t?.from == me
    val other = t?.let { if (iPay) it.to else it.from }
    val acc = look.account(accountId)

    ScreenScaffold(str(R.string.settle_up), { nav.popBackStack() }, bottomBar = {
        SavedBar(
            if (t == null) {
                str(R.string.nothing_to_settle)
            } else if (iPay) {
                str(R.string.record_paid, formatPaise(paise))
            } else {
                str(R.string.record_received, formatPaise(paise))
            },
            saver,
            t != null && paise > 0 && paise <= t.paise && accountId.isNotBlank(),
            onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.addSplit(
                        id,
                        SplitEntry(
                            kind = "settlement",
                            description =
                                note.trim().ifBlank {
                                    str(R.string.settle_up)
                                },
                            totalPaise = paise,
                            paidBy = t!!.from,
                            settleTo = t.to,
                            dateMillis = date,
                        ),
                    )
                    // Also note it in your own accounts so your balance matches reality.
                    Repo.put(
                        "txns",
                        "",
                        Txn(
                            kind = if (iPay) "expense" else "income",
                            amountPaise = paise,
                            accountId = accountId,
                            note = str(R.string.settled_with, chat?.nameOf(other!!, me)),
                            dateMillis = date,
                        ),
                    )
                }
            },
        )
    }) {
        if (t == null) {
            EmptyState(Icons.Filled.CheckCircle, str(R.string.all_settled), str(R.string.no_one_owes_you_and_you))
            return@ScreenScaffold
        }
        if (plan.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.forEachIndexed {
                        i,
                        p,
                    ->
                    SelectChip(chat?.nameOf(if (p.from == me) p.to else p.from, me) ?: "?", i == sel) { sel = i }
                }
            }
        }
        HeroCard(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(clr(R.color.swatch_teal), clr(R.color.swatch_teal_dark))),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Avatar(chat?.nameOf(t.from, me) ?: "?", colorKeyFor(t.from), 56.dp)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp).size(28.dp),
                )
                Avatar(chat?.nameOf(t.to, me) ?: "?", colorKeyFor(t.to), 56.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (iPay) str(R.string.you_pay, chat?.nameOf(t.to, me)) else "${chat?.nameOf(t.from, me)} pays you",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                formatPaise(paise),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                str(R.string.suggested, formatPaise(t.paise)),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        AmountField(amount, { amount = it }, color = Ink)
        if (paise > t.paise) {
            Text(
                str(R.string.that_is_more_than_the_owed, formatPaise(t.paise)),
                color = Danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Card(padding = 12.dp) {
            PickRowLeading(if (iPay) str(R.string.money_comes_out_of) else str(R.string.money_goes_into), acc?.name ?: "", {
                IconCircle(acc?.icon ?: "bank", acc?.colorKey ?: "blue", 38.dp)
            }, { nav.navigate(Routes.accountPicker("account")) })
            PickRow(
                str(R.string.date),
                fullDate(date),
                Icons.Filled.CalendarMonth,
                Muted,
                { nav.navigate(Routes.datePicker(date)) },
                divider = false,
            )
        }
        AppTextField(note, { note = it }, str(R.string.note_optional))
        Hint(str(R.string.spendly_only_records_this_payment_it))
        saver.error?.let { Text(it, color = Danger) }
    }
}