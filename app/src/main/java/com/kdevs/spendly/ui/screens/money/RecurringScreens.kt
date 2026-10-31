package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.kdevs.spendly.ui.screens.common.Lookup
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

// "Every 2 weeks" (always plural-aware, used by the custom panel)
@Composable
private fun everyPlural(
    every: String,
    n: Int,
): String {
    val res =
        when (every) {
            "daily" -> R.plurals.every_n_days
            "weekly" -> R.plurals.every_n_weeks
            "monthly" -> R.plurals.every_n_months
            else -> R.plurals.every_n_years
        }
    return pluralStringResource(res, n, n)
}

// "Monthly" for interval 1, "Every 2 weeks" otherwise (used by the list)
@Composable
private fun everyText(
    every: String,
    interval: Int,
): String =
    if (interval <= 1) {
        everyLabels.firstOrNull { it.first == every }?.second ?: every
    } else {
        everyPlural(every, interval)
    }

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
        // Active items first, soonest due at the top; paused ones sink to the bottom.
        items.sortedWith(compareBy({ !it.active }, { it.nextMillis })).forEach { r ->
            Card {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecurringRow(
                        r,
                        look,
                        Modifier.weight(1f).clickable { nav.navigate(Routes.recurringEdit(r.id)) },
                    )
                    Switch(r.active, { v -> scope.launch { runCatching { Repo.put("recurring", r.id, r.copy(active = v)) } } })
                }
            }
        }
    }
}

/** One recurring item: icon, name + amount, "Category · Sub-category", then "Monthly · next 5 Nov". Shared by the list and Home. */
@Composable
fun RecurringRow(
    r: Recurring,
    look: Lookup,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = look.category(r.categoryId)
    val sub = if (r.subcategoryId.isBlank()) null else look.category(r.subcategoryId)
    val where = if (sub != null) "${c?.name.orEmpty()} · ${sub.name}" else c?.name.orEmpty()
    val alpha = if (r.active) 1f else 0.5f
    Row(
        modifier.alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconCircle(sub?.icon ?: c?.icon ?: "other", c?.colorKey ?: "slate", 42.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Repeat, null, tint = Muted, modifier = Modifier.size(14.dp))
                Text(r.name, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (where.isNotBlank()) Text(where, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                str(
                    R.string.next_2,
                    formatPaise(r.amountPaise),
                    everyText(r.every, r.interval),
                    fullDate(r.nextMillis) + if (hasTime(r.nextMillis)) ", " + timeOf(r.nextMillis) else "",
                ),
                color = if (r.kind == "income") Success else Muted,
                fontSize = 12.sp,
            )
        }
        trailing?.invoke()
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
    var subId by rememberSaveable(existing) { mutableStateOf(existing?.subcategoryId ?: "") }
    var every by rememberSaveable(existing) { mutableStateOf(existing?.every ?: "monthly") }
    var interval by rememberSaveable(existing) { mutableStateOf(existing?.interval ?: 1) }
    var custom by rememberSaveable(existing) { mutableStateOf((existing?.interval ?: 1) > 1) }
    var next by rememberSaveable(existing) { mutableStateOf(existing?.nextMillis ?: nowMillis()) }
    var active by rememberSaveable(existing) { mutableStateOf(existing?.active ?: true) }
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
    entry.Picked<Long>("pickedDate") { next = withTimeOf(it, next) }
    entry.Picked<Int>("pickedTime") { next = withMinutes(next, it) }
    val paise = parseRupees(amount) ?: 0L
    val c = look.category(catId)
    val sub = if (subId.isBlank()) null else look.category(subId)
    val a = look.account(accountId)
    ScreenScaffold(
        if (existing == null) str(R.string.new_recurring_item) else str(R.string.edit_recurring_item),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, name.isNotBlank() && paise > 0 && accountId.isNotBlank(), onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    val rec =
                        Recurring(
                            name = name.trim(),
                            kind = kind,
                            amountPaise = paise,
                            accountId = accountId,
                            categoryId = catId,
                            subcategoryId = subId,
                            every = every,
                            interval = if (custom) interval else 1,
                            nextMillis = next,
                            anchorDay = toLocalDate(next).dayOfMonth,
                            active = active,
                        )
                    val savedId = Repo.put("recurring", id, rec)
                    // A start date of today or earlier is already due: add those transactions now, not at the next app launch.
                    Repo.runRecurring(rec.copy(id = savedId))
                }
            })
        },
    ) {
        SegmentedControl(listOf(str(R.string.expense), str(R.string.income)), if (kind == "income") 1 else 0, {
            kind = if (it == 1) "income" else "expense"
            catId = ""
            subId = ""
        })
        AppTextField(name, { name = it }, str(R.string.name_for_example_rent))
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 6.dp), color = if (kind == "income") Success else Danger)
        SectionTitle(str(R.string.repeats))
        RepeatsSection(
            every = every,
            interval = interval,
            custom = custom,
            next = next,
            onPreset = {
                every = it
                interval = 1
                custom = false
            },
            onCustom = {
                custom = true
                if (interval < 2) interval = 2
            },
            onInterval = { interval = it.coerceIn(1, 99) },
            onUnit = { every = it },
        )
        Card(padding = 12.dp) {
            PickRowLeading(str(R.string.category), if (sub != null) "${c?.name.orEmpty()} · ${sub.name}" else c?.name ?: "", {
                IconCircle(sub?.icon ?: c?.icon ?: "other", c?.colorKey ?: "slate", 38.dp)
            }, { nav.navigate(Routes.categoryPicker(kind, "recurring")) })
            PickRowLeading(str(R.string.account), a?.name ?: "", {
                IconCircle(a?.icon ?: "bank", a?.colorKey ?: "blue", 38.dp)
            }, { nav.navigate(Routes.accountPicker("account")) })
            PickRow(str(R.string.next_date), fullDate(next), Icons.Filled.CalendarMonth, Muted, { nav.navigate(Routes.datePicker(next)) })
            PickRow(
                str(R.string.time),
                if (hasTime(next)) timeOf(next) else str(R.string.add_time),
                Icons.Filled.Schedule,
                Muted,
                { nav.navigate(Routes.timePicker(minutesOfDay(next))) },
            )
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

// ------------------------------------------------------------------ Repeats UI

private class Preset(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val sub: String,
)

@Composable
private fun RepeatsSection(
    every: String,
    interval: Int,
    custom: Boolean,
    next: Long,
    onPreset: (String) -> Unit,
    onCustom: () -> Unit,
    onInterval: (Int) -> Unit,
    onUnit: (String) -> Unit,
) {
    val presets =
        listOf(
            Preset("daily", Icons.Filled.WbSunny, str(R.string.daily), str(R.string.every_day)),
            Preset("weekly", Icons.Filled.CalendarViewWeek, str(R.string.weekly), str(R.string.every_week)),
            Preset("monthly", Icons.Filled.CalendarMonth, str(R.string.monthly), str(R.string.every_month)),
            Preset("yearly", Icons.Filled.Event, str(R.string.yearly), str(R.string.every_year)),
        )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    RepeatTile(
                        icon = p.icon,
                        title = p.title,
                        sub = p.sub,
                        selected = !custom && every == p.key,
                        modifier = Modifier.weight(1f),
                        onClick = { onPreset(p.key) },
                    )
                }
            }
        }
        RepeatTile(
            icon = Icons.Filled.Tune,
            title = str(R.string.custom),
            sub = if (custom) everyPlural(every, interval) else str(R.string.pick_your_own_interval),
            selected = custom,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCustom,
        )
        if (custom) {
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, Muted.copy(alpha = 0.3f), shape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(str(R.string.every), color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StepButton(Icons.Filled.Remove, str(R.string.decrease), interval > 1) { onInterval(interval - 1) }
                        Text(
                            interval.toString(),
                            color = Ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 44.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        StepButton(Icons.Filled.Add, str(R.string.increase), interval < 99) { onInterval(interval + 1) }
                    }
                }
                UnitSelector(
                    selected = every,
                    options =
                        listOf(
                            "daily" to str(R.string.days),
                            "weekly" to str(R.string.weeks),
                            "monthly" to str(R.string.months),
                            "yearly" to str(R.string.years),
                        ),
                    onSelect = onUnit,
                )
                Text(
                    str(R.string.repeat_summary, fullDate(next), everyPlural(every, interval)),
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun RepeatTile(
    icon: ImageVector,
    title: String,
    sub: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
            .border(if (selected) 1.5.dp else 0.5.dp, if (selected) accent else Muted.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (selected) accent else Muted, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selected) accent else Ink)
            Text(sub, fontSize = 12.sp, color = if (selected) accent else Muted)
        }
        if (selected) Icon(Icons.Filled.Check, null, tint = accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    desc: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = if (enabled) Ink else Muted.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun UnitSelector(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (on) Ink else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) MaterialTheme.colorScheme.surface else Muted,
                )
            }
        }
    }
}
