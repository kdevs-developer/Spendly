package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.launch

/** A row inside a form card that opens a picker screen. */
@Composable
fun PickRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Muted,
    onClick: () -> Unit,
    divider: Boolean = true,
) {
    SettingRow(label, value.ifBlank { str(R.string.choose) }, icon = icon, iconColor = tint, onClick = onClick, divider = divider)
}

@Composable
fun PickRowLeading(
    label: String,
    value: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    divider: Boolean = true,
) {
    SettingRow(label, value.ifBlank { str(R.string.choose) }, leading = leading, onClick = onClick, divider = divider)
}

@Composable
fun SavedBar(
    text: String,
    saver: Saver,
    enabled: Boolean = true,
    onSave: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    BottomActionBar {
        saver.error?.let { Text(it, color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        PrimaryButton(text, onSave, enabled = enabled, busy = saver.busy)
        extra()
    }
}

/** Encodes the category picker result as "parentId|subId". */
fun splitPick(v: String): Pair<String, String> = v.substringBefore('|') to v.substringAfter('|', "")

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
    val existing = remember(id, txns.isEmpty()) { txns.firstOrNull { it.id == id } }
    val editing = id.isNotBlank()

    var type by remember(existing) { mutableStateOf(existing?.kind ?: kind) }
    var amount by remember(existing) { mutableStateOf(existing?.let { paiseToField(it.amountPaise) } ?: "") }
    var accountId by remember(existing) { mutableStateOf(existing?.accountId ?: "") }
    var catId by remember(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var subId by remember(existing) { mutableStateOf(existing?.subcategoryId ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var date by remember(existing) { mutableLongStateOf(existing?.dateMillis ?: todayMillis()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()

    LaunchedEffect(accounts) { if (accountId.isBlank()) accountId = accounts.firstOrNull { !it.archived }?.id ?: "" }
    entry.Picked<String>("pickedAccount") { accountId = it }
    entry.Picked<String>("pickedCategory") { v ->
        splitPick(v).let {
            catId = it.first
            subId = it.second
        }
    }
    entry.Picked<Long>("pickedDate") { date = it }

    val paise = parseRupees(amount) ?: 0L
    val cat = look.category(subId.ifBlank { catId })
    val parent = look.category(catId)
    val acc = look.account(accountId)
    val tint by androidx.compose.animation.animateColorAsState(
        if (type == "income") Success else Danger,
        androidx.compose.animation.core.tween(220),
        label = "txnTint",
    )

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
                            ),
                        )
                    }
                },
            )
        },
        header = {
            SegmentedControl(listOf(str(R.string.expense), str(R.string.income)), if (type == "income") 1 else 0, {
                type = if (it == 1) "income" else "expense"
                catId = ""
                subId = ""
            })
            AmountField(amount, { amount = it }, color = tint)
        },
    ) {
        Card(padding = 12.dp) {
            PickRowLeading(
                str(R.string.category),
                if (cat == null) {
                    ""
                } else {
                    (
                        parent?.name?.let {
                                p ->
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
            }, divider = false)
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
fun TxnDetailScreen(
    nav: NavController,
    id: String,
) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val t = txns.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    ScreenScaffold(
        str(R.string.transaction),
        { nav.popBackStack() },
        actions = { if (t != null) RoundIconButton(Icons.Filled.Edit, str(R.string.edit), { nav.navigate(Routes.txnEdit(id)) }) },
    ) {
        if (t == null) {
            EmptyState(Icons.Filled.SearchOff, str(R.string.not_found), str(R.string.this_transaction_was_deleted))
            return@ScreenScaffold
        }
        val cat = look.category(t.subcategoryId.ifBlank { t.categoryId })
        val parent = look.category(t.categoryId)
        val color = if (t.kind == "transfer") "slate" else parent?.colorKey ?: cat?.colorKey ?: "slate"
        HeroCard(brush = colorSpec(color).brush) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconFor(if (t.kind == "transfer") "invest" else cat?.icon ?: "other"), null, tint = Color.White)
                }
                Column {
                    Text(
                        if (t.kind == "transfer") str(R.string.transfer) else cat?.name ?: str(R.string.uncategorised),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(t.kind.replaceFirstChar { it.uppercase() }, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                (
                    if (t.kind == "expense") {
                        "-"
                    } else if (t.kind == "income") {
                        "+"
                    } else {
                        ""
                    }
                ) + formatPaise(t.amountPaise),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
            )
        }
        Card(padding = 12.dp) {
            if (t.kind == "transfer") {
                PickRow(str(R.string.from), look.accountName(t.accountId), Icons.Filled.AccountBalanceWallet, Primary, {}, divider = true)
                PickRow(str(R.string.to_2), look.accountName(t.toAccountId), Icons.Filled.AccountBalance, Success, {}, divider = true)
            } else {
                PickRow(str(R.string.account), look.accountName(t.accountId), Icons.Filled.AccountBalanceWallet, Primary, {
                    nav.navigate(Routes.accountDetail(t.accountId))
                })
                if (parent != null) {
                    PickRow(
                        str(
                            R.string.category,
                        ),
                        parent.name + if (t.subcategoryId.isNotBlank()) " · ${look.categoryName(t.subcategoryId)}" else "",
                        Icons.Filled.Category,
                        clr(
                            R.color.swatch_purple,
                        ),
                        {
                        },
                    )
                }
            }
            PickRow(str(R.string.date), fullDate(t.dateMillis), Icons.Filled.CalendarMonth, Muted, {}, divider = t.note.isNotBlank())
            if (t.note.isNotBlank()) PickRow(str(R.string.note), t.note, Icons.Filled.Notes, Muted, {}, divider = false)
        }
        if (t.kind != "transfer") {
            SecondaryButton(str(R.string.duplicate), {
                saver.run(scope, onDone = { nav.popBackStack() }) { Repo.put("txns", "", t.copy(id = "", dateMillis = todayMillis())) }
            })
        }
        SecondaryButton(str(R.string.delete_transaction_2), { confirm = true }, danger = true)
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_transaction), str(R.string.this_cannot_be_undone), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("txns", id) }
        }, { confirm = false }, danger = true)
    }
}

@Composable
fun TransferScreen(
    nav: NavController,
    entry: NavBackStackEntry,
) {
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(todayMillis()) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    LaunchedEffect(accounts) {
        val live = accounts.filter { !it.archived }
        if (from.isBlank()) from = live.getOrNull(0)?.id ?: ""
        if (to.isBlank()) to = live.getOrNull(1)?.id ?: ""
    }
    entry.Picked<String>("pickedAccount") { from = it }
    entry.Picked<String>("pickedAccount2") { to = it }
    entry.Picked<Long>("pickedDate") { date = it }
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
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var opening by remember(existing) { mutableStateOf(existing?.let { paiseToField(kotlin.math.abs(it.openingPaise)) } ?: "") }
    var icon by remember(existing) { mutableStateOf(existing?.icon ?: info.icon) }
    var color by remember(existing) { mutableStateOf(existing?.colorKey ?: info.colorKey) }
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
            leading = { Text("₹", fontWeight = FontWeight.Bold) },
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

fun iconLabel(key: String) = key.replaceFirstChar { it.uppercase() }

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
                    }?.label.orEmpty() + if (a.archived) " · Archived" else "",
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

// ------------------------------------------------------------------ Budgets

@Composable
fun BudgetsScreen(nav: NavController) {
    val budgets by Repo.budgets.collectAsState()
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val month = inRange(txns, monthStart(), nextMonthStart()).filter { it.kind == "expense" }
    ScreenScaffold(
        str(R.string.budgets),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.add_budget), { nav.navigate(Routes.budgetEdit("")) }) },
    ) {
        Hint(str(R.string.budgets_are_monthly_they_reset_on))
        if (budgets.isEmpty()) EmptyState(Icons.Filled.DonutLarge, str(R.string.no_budgets_yet), str(R.string.set_an_overall_limit_or_one))
        budgets.sortedBy { it.categoryId.isNotBlank() }.forEach { b ->
            val used =
                if (b.categoryId.isBlank()) {
                    month.sumOf {
                        it.amountPaise
                    }
                } else {
                    month.filter { it.categoryId == b.categoryId }.sumOf { it.amountPaise }
                }
            val c = look.category(b.categoryId)
            val over = used > b.limitPaise
            Card(onClick = { nav.navigate(Routes.budgetEdit(b.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconCircle(c?.icon ?: "bills", c?.colorKey ?: "blue", 42.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (b.categoryId.isBlank()) str(R.string.overall) else c?.name ?: str(R.string.category),
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink,
                        )
                        Text(str(R.string.of_2, formatPaise(used), formatPaise(b.limitPaise)), color = Muted, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Faint)
                }
                LabeledBar(
                    "",
                    used,
                    b.limitPaise,
                    if (over) Danger else colorSpec(c?.colorKey ?: "blue").solid,
                    if (over) str(R.string.over_by, formatPaise(used - b.limitPaise)) else "${formatPaise(b.limitPaise - used)} left",
                )
            }
        }
    }
}

@Composable
fun BudgetEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val budgets by Repo.budgets.collectAsState()
    val look = rememberLookup()
    val existing = remember(id, budgets.isEmpty()) { budgets.firstOrNull { it.id == id } }
    var catId by remember(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var limit by remember(existing) { mutableStateOf(existing?.let { paiseToField(it.limitPaise) } ?: "") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedCategory") { catId = splitPick(it).first }
    val c = look.category(catId)
    val paise = parseRupees(limit) ?: 0L
    ScreenScaffold(
        if (existing == null) str(R.string.new_budget) else str(R.string.edit_budget),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, paise > 0, onSave = {
                if (budgets.any { it.categoryId == catId && it.id != id }) {
                    saver.error = str(R.string.budget_already_exists)
                    return@SavedBar
                }
                saver.run(
                    scope,
                    onDone = { nav.popBackStack() },
                ) { Repo.put("budgets", id, Budget(categoryId = catId, limitPaise = paise)) }
            })
        },
    ) {
        Text(
            str(R.string.monthly_limit),
            color = Muted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        AmountField(limit, { limit = it }, Modifier.padding(vertical = 6.dp), color = Primary)
        Card(padding = 12.dp) {
            PickRowLeading(
                str(R.string.applies_to),
                if (catId.isBlank()) str(R.string.overall_all_spending) else c?.name.orEmpty(),
                { IconCircle(c?.icon ?: "bills", c?.colorKey ?: "blue", 38.dp) },
                { nav.navigate(Routes.categoryPicker("expense", "budget")) },
                divider = false,
            )
        }
        if (catId.isNotBlank()) {
            Text(
                str(R.string.use_overall_instead),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable { catId = "" }.padding(8.dp),
            )
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_budget), str(R.string.your_transactions_are_not_affected), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("budgets", id) }
        }, { confirm = false }, danger = true)
    }
}

// ------------------------------------------------------------------ Goals

@Composable
fun GoalsScreen(nav: NavController) {
    val goals by Repo.goals.collectAsState()
    ScreenScaffold(
        str(R.string.savings_goals),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.new_goal), { nav.navigate(Routes.goalEdit("")) }) },
    ) {
        if (goals.isEmpty()) EmptyState(Icons.Filled.Flag, str(R.string.no_goals_yet), str(R.string.save_towards_a_trip_a_phone))
        goals.forEach { g ->
            val pct = if (g.targetPaise > 0) (g.savedPaise * 100 / g.targetPaise).coerceAtMost(100) else 0
            Card(onClick = { nav.navigate(Routes.goalEdit(g.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconCircle(g.icon, g.colorKey, 46.dp)
                    Column(Modifier.weight(1f)) {
                        Text(g.name, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text(
                            if (g.deadlineMillis > 0) str(R.string.by, fullDate(g.deadlineMillis)) else str(R.string.no_deadline),
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                    Text(str(R.string.text__5, pct), fontWeight = FontWeight.ExtraBold, color = colorSpec(g.colorKey).solid)
                }
                LabeledBar(
                    "",
                    g.savedPaise,
                    g.targetPaise,
                    colorSpec(g.colorKey).solid,
                    "${formatPaise(g.savedPaise)} of ${formatPaise(g.targetPaise)}",
                )
                if (g.savedPaise < g.targetPaise) {
                    SecondaryButton(str(R.string.add_savings), { nav.navigate(Routes.goalAdd(g.id)) })
                } else {
                    Text(str(R.string.goal_reached), color = Success, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun GoalEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val goals by Repo.goals.collectAsState()
    val existing = remember(id, goals.isEmpty()) { goals.firstOrNull { it.id == id } }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var target by remember(existing) { mutableStateOf(existing?.let { paiseToField(it.targetPaise) } ?: "") }
    var icon by remember(existing) { mutableStateOf(existing?.icon ?: "goal") }
    var color by remember(existing) { mutableStateOf(existing?.colorKey ?: "green") }
    var deadline by remember(existing) { mutableLongStateOf(existing?.deadlineMillis ?: 0L) }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    entry.Picked<String>("pickedColor") { color = it }
    entry.Picked<Long>("pickedDate") { deadline = it }
    val paise = parseRupees(target) ?: 0L
    ScreenScaffold(
        if (existing == null) str(R.string.new_goal) else str(R.string.edit_goal),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save_goal), saver, name.isNotBlank() && paise > 0, onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "goals",
                        id,
                        Goal(
                            name = name.trim(),
                            icon = icon,
                            colorKey = color,
                            targetPaise = paise,
                            savedPaise = existing?.savedPaise ?: 0,
                            deadlineMillis = deadline,
                        ),
                    )
                }
            })
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, str(R.string.goal_name))
        AppTextField(
            target,
            { v -> if (v.all { it.isDigit() || it == '.' }) target = v },
            str(R.string.target_amount),
            keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal,
            leading = { Text("₹", fontWeight = FontWeight.Bold) },
        )
        Card(padding = 12.dp) {
            PickRow(
                str(
                    R.string.deadline,
                ),
                if (deadline > 0) fullDate(deadline) else str(R.string.none),
                Icons.Filled.CalendarMonth,
                Muted,
                {
                    nav.navigate(Routes.datePicker(if (deadline > 0) deadline else todayMillis()))
                },
            )
            PickRowLeading(str(R.string.icon), iconLabel(icon), {
                IconCircle(icon, color, 38.dp)
            }, { nav.navigate(Routes.iconPicker("category", icon)) })
            PickRowLeading(
                str(R.string.colour),
                colorSpec(color).label,
                { Box(Modifier.size(38.dp).clip(CircleShape).background(colorSpec(color).brush)) },
                { nav.navigate(Routes.colorPicker(color)) },
                divider = false,
            )
        }
        if (deadline > 0) {
            Text(
                str(
                    R.string.remove_deadline,
                ),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.clip(CircleShape).clickable {
                        deadline = 0
                    }.padding(8.dp),
            )
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_goal), str(R.string.the_saved_amount_is_only_a), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("goals", id) }
        }, { confirm = false }, danger = true)
    }
}

@Composable
fun GoalAddScreen(
    nav: NavController,
    id: String,
) {
    val goals by Repo.goals.collectAsState()
    val g = goals.firstOrNull { it.id == id }
    var amount by remember { mutableStateOf("") }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val paise = parseRupees(amount) ?: 0L
    ScreenScaffold(str(R.string.add_savings), { nav.popBackStack() }, bottomBar = {
        SavedBar(str(R.string.add_to_goal), saver, paise > 0 && g != null, onSave = {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.put("goals", id, g!!.copy(savedPaise = g.savedPaise + paise)) }
        })
    }) {
        if (g == null) return@ScreenScaffold
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(g.icon, g.colorKey, 72.dp) }
        Text(
            g.name,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Hint(str(R.string.saved_of, formatPaise(g.savedPaise), formatPaise(g.targetPaise)), center = true)
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 10.dp), color = colorSpec(g.colorKey).solid)
        if (paise > 0) Hint(str(R.string.after_this, formatPaise(g.savedPaise + paise)), center = true)
    }
}

// ------------------------------------------------------------------ Templates

@Composable
fun TemplatesScreen(nav: NavController) {
    val templates by Repo.templates.collectAsState()
    val look = rememberLookup()
    val accounts by Repo.accounts.collectAsState()
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    ScreenScaffold(
        str(R.string.templates),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.new_template), { nav.navigate(Routes.templateEdit("")) }) },
    ) {
        Hint(str(R.string.tap_use_to_add_the_transaction))
        saver.error?.let { Text(it, color = Danger) }
        if (templates.isEmpty()) {
            EmptyState(
                Icons.Filled.ContentCopy,
                str(R.string.no_templates_yet),
                str(R.string.save_the_things_you_buy_often),
            )
        }
        templates.forEach { t ->
            val c = look.category(t.categoryId)
            Card {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.weight(1f).clickable {
                            nav.navigate(Routes.templateEdit(t.id))
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 42.dp)
                        Column {
                            Text(t.name, fontWeight = FontWeight.ExtraBold, color = Ink)
                            Text(
                                str(R.string.text__6, formatPaise(t.amountPaise), look.accountName(t.accountId)),
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Box(
                        Modifier.clip(CircleShape).background(PrimaryContainer).clickable {
                            val acc = t.accountId.ifBlank { accounts.firstOrNull { !it.archived }?.id ?: "" }
                            if (acc.isBlank()) {
                                saver.error = str(R.string.add_an_account_first)
                                return@clickable
                            }
                            saver.run(scope, onDone = { nav.popBackStack() }) {
                                Repo.put(
                                    "txns",
                                    "",
                                    Txn(
                                        kind = t.kind,
                                        amountPaise = t.amountPaise,
                                        accountId = acc,
                                        categoryId = t.categoryId,
                                        note =
                                            t.note.ifBlank {
                                                t.name
                                            },
                                        dateMillis = todayMillis(),
                                    ),
                                )
                            }
                        }.padding(horizontal = 16.dp, vertical = 9.dp),
                    ) { Text(str(R.string.use), color = Primary, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

@Composable
fun TemplateEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val templates by Repo.templates.collectAsState()
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val existing = remember(id, templates.isEmpty()) { templates.firstOrNull { it.id == id } }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var kind by remember(existing) { mutableStateOf(existing?.kind ?: "expense") }
    var amount by remember(existing) { mutableStateOf(existing?.let { paiseToField(it.amountPaise) } ?: "") }
    var accountId by remember(existing) { mutableStateOf(existing?.accountId ?: "") }
    var catId by remember(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedAccount") { accountId = it }
    entry.Picked<String>("pickedCategory") { catId = splitPick(it).first }
    val paise = parseRupees(amount) ?: 0L
    val c = look.category(catId)
    val a = look.account(accountId)
    ScreenScaffold(
        if (existing == null) str(R.string.new_template) else str(R.string.edit_template),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save_template), saver, name.isNotBlank() && paise > 0, onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "templates",
                        id,
                        Template(
                            name = name.trim(),
                            kind = kind,
                            amountPaise = paise,
                            accountId = accountId,
                            categoryId = catId,
                            note = note.trim(),
                        ),
                    )
                }
            })
        },
    ) {
        SegmentedControl(listOf(str(R.string.expense), str(R.string.income)), if (kind == "income") 1 else 0, {
            kind = if (it == 1) "income" else "expense"
            catId = ""
        })
        AppTextField(name, { name = it }, str(R.string.template_name))
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 6.dp), color = if (kind == "income") Success else Danger)
        Card(padding = 12.dp) {
            PickRowLeading(str(R.string.category), c?.name ?: "", {
                IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 38.dp)
            }, { nav.navigate(Routes.categoryPicker(kind, "template")) })
            PickRowLeading(str(R.string.account), a?.name ?: str(R.string.ask_each_time), {
                IconCircle(a?.icon ?: "bank", a?.colorKey ?: "blue", 38.dp)
            }, { nav.navigate(Routes.accountPicker("account")) }, divider = false)
        }
        AppTextField(note, { note = it }, str(R.string.note_optional))
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_template), str(R.string.transactions_already_made_from_it_stay), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("templates", id) }
        }, { confirm = false }, danger = true)
    }
}

// ------------------------------------------------------------------ Recurring

private val everyLabels =
    listOf(
        "daily" to str(R.string.daily),
        "weekly" to str(R.string.weekly),
        "monthly" to str(R.string.monthly),
        "yearly" to str(R.string.yearly),
    )

@Composable
fun RecurringScreen(nav: NavController) {
    val items by Repo.recurring.collectAsState()
    val look = rememberLookup()
    val scope = rememberCoroutineScope()
    ScreenScaffold(
        str(R.string.recurring),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.new_recurring_item), { nav.navigate(Routes.recurringEdit("")) }) },
    ) {
        Hint(str(R.string.spendly_adds_these_for_you_on))
        if (items.isEmpty()) {
            EmptyState(
                Icons.Filled.Repeat,
                str(R.string.nothing_repeating),
                str(R.string.add_rent_subscriptions_or_your_salary),
            )
        }
        items.forEach { r ->
            val c = look.category(r.categoryId)
            Card {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.weight(1f).clickable {
                            nav.navigate(Routes.recurringEdit(r.id))
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 42.dp)
                        Column {
                            Text(r.name, fontWeight = FontWeight.ExtraBold, color = Ink)
                            Text(
                                str(
                                    R.string.next_2,
                                    formatPaise(r.amountPaise),
                                    everyLabels.first {
                                        it.first == r.every
                                    }.second,
                                    fullDate(r.nextMillis),
                                ),
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Switch(r.active, { v -> scope.launch { runCatching { Repo.put("recurring", r.id, r.copy(active = v)) } } })
                }
            }
        }
    }
}

@Composable
fun RecurringEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val items by Repo.recurring.collectAsState()
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val existing = remember(id, items.isEmpty()) { items.firstOrNull { it.id == id } }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var kind by remember(existing) { mutableStateOf(existing?.kind ?: "expense") }
    var amount by remember(existing) { mutableStateOf(existing?.let { paiseToField(it.amountPaise) } ?: "") }
    var accountId by remember(existing) { mutableStateOf(existing?.accountId ?: "") }
    var catId by remember(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var every by remember(existing) { mutableStateOf(existing?.every ?: "monthly") }
    var next by remember(existing) { mutableLongStateOf(existing?.nextMillis ?: todayMillis()) }
    var active by remember(existing) { mutableStateOf(existing?.active ?: true) }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    LaunchedEffect(accounts) { if (accountId.isBlank()) accountId = accounts.firstOrNull { !it.archived }?.id ?: "" }
    entry.Picked<String>("pickedAccount") { accountId = it }
    entry.Picked<String>("pickedCategory") { catId = splitPick(it).first }
    entry.Picked<Long>("pickedDate") { next = it }
    val paise = parseRupees(amount) ?: 0L
    val c = look.category(catId)
    val a = look.account(accountId)
    ScreenScaffold(
        if (existing == null) str(R.string.new_recurring_item) else str(R.string.edit_recurring_item),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, name.isNotBlank() && paise > 0 && accountId.isNotBlank(), onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "recurring",
                        id,
                        Recurring(
                            name = name.trim(),
                            kind = kind,
                            amountPaise = paise,
                            accountId = accountId,
                            categoryId = catId,
                            every = every,
                            nextMillis = next,
                            active = active,
                        ),
                    )
                }
            })
        },
    ) {
        SegmentedControl(listOf(str(R.string.expense), str(R.string.income)), if (kind == "income") 1 else 0, {
            kind = if (it == 1) "income" else "expense"
            catId = ""
        })
        AppTextField(name, { name = it }, str(R.string.name_for_example_rent))
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 6.dp), color = if (kind == "income") Success else Danger)
        SectionTitle(str(R.string.repeats))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            everyLabels.forEach { (k, l) -> SelectChip(l, every == k) { every = k } }
        }
        Card(padding = 12.dp) {
            PickRowLeading(str(R.string.category), c?.name ?: "", {
                IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 38.dp)
            }, { nav.navigate(Routes.categoryPicker(kind, "recurring")) })
            PickRowLeading(str(R.string.account), a?.name ?: "", {
                IconCircle(a?.icon ?: "bank", a?.colorKey ?: "blue", 38.dp)
            }, { nav.navigate(Routes.accountPicker("account")) })
            PickRow(str(R.string.next_date), fullDate(next), Icons.Filled.CalendarMonth, Muted, { nav.navigate(Routes.datePicker(next)) })
            SettingRow(str(R.string.active), icon = Icons.Filled.PowerSettingsNew, iconColor = Success, trailing = {
                Switch(active, { active = it })
            }, divider = false)
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_recurring_item), str(R.string.past_transactions_it_created_stay), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("recurring", id) }
        }, { confirm = false }, danger = true)
    }
}
