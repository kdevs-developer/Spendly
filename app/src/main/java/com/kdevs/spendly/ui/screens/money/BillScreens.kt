package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import java.time.temporal.ChronoUnit

// ------------------------------------------------------------------ Bills

private val everyKeys = listOf("none", "weekly", "monthly", "yearly")
private val remindKeys = listOf(0, 1, 3, 7)

/** "Due today", "Due in 3 days", "Overdue by 2 days" */
private fun dueText(dueMillis: Long): String {
    val days = ChronoUnit.DAYS.between(java.time.LocalDate.now(), toLocalDate(dueMillis)).toInt()
    return when {
        days < 0 -> str(R.string.overdue_by_days, -days)
        days == 0 -> str(R.string.due_today)
        days == 1 -> str(R.string.due_tomorrow)
        else -> str(R.string.due_in_days, days)
    }
}

@Composable
fun BillsScreen(nav: NavController) {
    val bills by Repo.bills.collectAsState()
    val look = rememberLookup()
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    val unpaid = bills.filter { !it.paid }.sortedBy { it.dueMillis }
    val paid = bills.filter { it.paid }.sortedByDescending { it.paidMillis }
    val today = todayMillis()

    ScreenScaffold(
        str(R.string.bills),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.new_bill), { nav.navigate(Routes.billEdit("")) }) },
    ) {
        Hint(str(R.string.bills_hint))
        saver.error?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold) }
        if (bills.isEmpty()) {
            EmptyState(Icons.Filled.Receipt, str(R.string.no_bills_yet), str(R.string.add_a_bill_to_get_a_reminder))
        }
        unpaid.forEach { b ->
            val c = look.category(b.subcategoryId.ifBlank { b.categoryId })
            val overdue = b.dueMillis < today
            Card(padding = 12.dp) {
                SettingRow(
                    b.name,
                    dueText(b.dueMillis) + " · " + fullDate(b.dueMillis) +
                        if (b.every != "none") " · " + str(R.string.repeats_every_x, everyLabel(b.every)) else "",
                    leading = { IconCircle(c?.icon ?: "other", look.category(b.categoryId)?.colorKey ?: "slate", 42.dp) },
                    trailing = {
                        Text(
                            formatPaise(b.amountPaise),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (overdue) Danger else Ink,
                        )
                    },
                    onClick = { nav.navigate(Routes.billEdit(b.id)) },
                    divider = false,
                )
                Spacer(Modifier.height(6.dp))
                SecondaryButton(str(R.string.mark_paid), {
                    saver.run(scope, onDone = {}) {
                        Repo.markBillPaid(b)
                        Undo.offer(str(R.string.bill_marked_paid, b.name)) { Repo.undoBillPaid(b) }
                    }
                })
            }
        }
        if (paid.isNotEmpty()) {
            SectionTitle(str(R.string.paid))
            Card(padding = 12.dp) {
                paid.forEachIndexed { i, b ->
                    SettingRow(
                        b.name,
                        str(R.string.paid_on, fullDate(b.paidMillis)),
                        icon = Icons.Filled.CheckCircle,
                        iconColor = Success,
                        trailing = { Text(formatPaise(b.amountPaise), color = Muted, fontWeight = FontWeight.Bold) },
                        onClick = { nav.navigate(Routes.billEdit(b.id)) },
                        divider = i < paid.lastIndex,
                    )
                }
            }
        }
    }
}

private fun everyLabel(key: String): String =
    when (key) {
        "weekly" -> str(R.string.weekly)
        "monthly" -> str(R.string.monthly)
        "yearly" -> str(R.string.yearly)
        else -> str(R.string.once)
    }

@Composable
fun BillEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val bills by Repo.bills.collectAsState()
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val existing = remember(id, bills.isEmpty()) { bills.firstOrNull { it.id == id } }
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var amount by rememberSaveable(existing) { mutableStateOf(existing?.let { paiseToField(it.amountPaise) } ?: "") }
    var accountId by rememberSaveable(existing) { mutableStateOf(existing?.accountId ?: "") }
    var catId by rememberSaveable(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var subId by rememberSaveable(existing) { mutableStateOf(existing?.subcategoryId ?: "") }
    var due by rememberSaveable(existing) { mutableStateOf(existing?.dueMillis ?: todayMillis()) }
    var every by rememberSaveable(existing) { mutableStateOf(existing?.every ?: "none") }
    var remind by rememberSaveable(existing) { mutableStateOf(existing?.remindDays ?: 1) }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    LaunchedEffect(accounts) { if (accountId.isBlank()) accountId = accounts.firstOrNull { !it.archived }?.id ?: "" }
    entry.Picked<String>("pickedAccount") { accountId = it }
    entry.Picked<String>("pickedCategory") {
        val (cat, sub) = splitPick(it)
        catId = cat
        subId = sub
    }
    entry.Picked<Long>("pickedDate") { due = it }
    val paise = parseRupees(amount) ?: 0L
    val c = look.category(catId)
    val sub = if (subId.isBlank()) null else look.category(subId)
    val a = look.account(accountId)

    ScreenScaffold(
        if (existing == null) str(R.string.new_bill) else str(R.string.edit_bill),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, name.isNotBlank() && paise > 0 && accountId.isNotBlank(), onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "bills",
                        id,
                        Bill(
                            name = name.trim(),
                            amountPaise = paise,
                            accountId = accountId,
                            categoryId = catId,
                            subcategoryId = subId,
                            dueMillis = due,
                            every = every,
                            anchorDay = toLocalDate(due).dayOfMonth,
                            remindDays = remind,
                            // Editing a paid one-off bill to a new date makes it due again.
                            paid = existing?.paid == true && existing.dueMillis == due && every == "none",
                            paidMillis = existing?.paidMillis ?: 0L,
                        ),
                    )
                }
            })
        },
    ) {
        AppTextField(name, { name = it }, str(R.string.bill_name_hint))
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 6.dp), color = Danger)
        SectionTitle(str(R.string.repeats))
        SegmentedControl(everyKeys.map { everyLabel(it) }, everyKeys.indexOf(every).coerceAtLeast(0), { every = everyKeys[it] })
        SectionTitle(str(R.string.remind_me))
        SegmentedControl(
            remindKeys.map {
                when (it) {
                    0 -> str(R.string.remind_same_day)
                    1 -> str(R.string.remind_one_day_before)
                    else -> str(R.string.remind_days_before, it)
                }
            },
            remindKeys.indexOf(remind).let { if (it < 0) 1 else it },
            { remind = remindKeys[it] },
        )
        Card(padding = 12.dp) {
            PickRowLeading(str(R.string.category), if (sub != null) "${c?.name.orEmpty()} · ${sub.name}" else c?.name ?: "", {
                IconCircle(sub?.icon ?: c?.icon ?: "other", c?.colorKey ?: "slate", 38.dp)
            }, { nav.navigate(Routes.categoryPicker("expense", "recurring")) })
            PickRowLeading(str(R.string.account), a?.name ?: "", {
                IconCircle(a?.icon ?: "bank", a?.colorKey ?: "blue", 38.dp)
            }, { nav.navigate(Routes.accountPicker("account")) })
            PickRow(
                str(R.string.due_date),
                fullDate(due),
                Icons.Filled.CalendarMonth,
                Muted,
                { nav.navigate(Routes.datePicker(due)) },
                divider = false,
            )
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_bill), str(R.string.delete_bill_hint), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("bills", id) }
        }, { confirm = false }, danger = true)
    }
}
