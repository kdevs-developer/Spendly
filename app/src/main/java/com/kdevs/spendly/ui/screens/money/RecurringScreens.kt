package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

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
                                    everyLabels
                                        .first {
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
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var kind by rememberSaveable(existing) { mutableStateOf(existing?.kind ?: "expense") }
    var amount by rememberSaveable(existing) { mutableStateOf(existing?.let { paiseToField(it.amountPaise) } ?: "") }
    var accountId by rememberSaveable(existing) { mutableStateOf(existing?.accountId ?: "") }
    var catId by rememberSaveable(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var every by rememberSaveable(existing) { mutableStateOf(existing?.every ?: "monthly") }
    var next by rememberSaveable(existing) { mutableStateOf(existing?.nextMillis ?: todayMillis()) }
    var active by rememberSaveable(existing) { mutableStateOf(existing?.active ?: true) }
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
                            anchorDay = toLocalDate(next).dayOfMonth,
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
