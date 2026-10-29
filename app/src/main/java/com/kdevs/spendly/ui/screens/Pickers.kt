package com.kdevs.spendly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.state.Selection
import com.kdevs.spendly.ui.state.TxnFilter
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val DAY = 86_400_000L

// ------------------------------------------------------------------ Filter

/**
 * Filter for the Activity list.
 *
 * Layout: search box, type tiles, then collapsible rows for Date, Accounts and Categories,
 * a Min / Max amount card, and an Apply button that shows how many filters are active.
 *
 * Nothing is written to [TxnFilter] until Apply is pressed. Reset only clears this form.
 * "Any" is stored the same way as before: empty sets, from/to = 0, min/max = 0.
 * [TxnFilter.toMillis] stays end-exclusive (picked day + 1 day).
 */
@Composable
fun FilterScreen(
    nav: NavController,
    entry: NavBackStackEntry,
) {
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val live = accounts.filter { !it.archived }
    val parents = look.categories.filter { it.parentId.isBlank() }

    var query by rememberSaveable { mutableStateOf(TxnFilter.query) }
    var kinds by rememberSaveable { mutableStateOf(TxnFilter.kinds) }
    var accs by rememberSaveable { mutableStateOf(TxnFilter.accountIds) }
    var cats by rememberSaveable { mutableStateOf(TxnFilter.categoryIds) }
    var from by rememberSaveable { mutableStateOf(TxnFilter.fromMillis) }
    var to by rememberSaveable { mutableStateOf(TxnFilter.toMillis) }
    var min by rememberSaveable { mutableStateOf(if (TxnFilter.minPaise == 0L) "" else paiseToField(TxnFilter.minPaise)) }
    var max by rememberSaveable { mutableStateOf(if (TxnFilter.maxPaise == 0L) "" else paiseToField(TxnFilter.maxPaise)) }
    // A saved range can't be mapped back to a preset, so it opens as "custom".
    var period by rememberSaveable { mutableStateOf(if (from == 0L && to == 0L) "any" else "custom") }
    var target by rememberSaveable { mutableStateOf("from") }
    var periodOpen by remember { mutableStateOf(false) }
    var accountsOpen by remember { mutableStateOf(false) }
    var categoriesOpen by remember { mutableStateOf(false) }

    entry.Picked<Long>("pickedDate") {
        if (target == "from") from = it else to = it + DAY
        period = "custom"
    }

    fun <T> Set<T>.flip(v: T) = if (v in this) this - v else this + v

    // TODO: move the hard-coded labels below to strings.xml.
    val periods =
        listOf(
            "any" to str(R.string.any),
            "month" to str(R.string.this_month),
            "lastmonth" to "Last month",
            "last30" to str(R.string.last_30_days),
            "year" to str(R.string.this_year),
            "custom" to str(R.string.custom),
        )

    fun choosePeriod(k: String) {
        val today = LocalDate.now()
        period = k
        when (k) {
            "any" -> {
                from = 0L
                to = 0L
            }
            "month" -> {
                from = monthStart()
                to = nextMonthStart()
            }
            "lastmonth" -> {
                from = today.minusMonths(1).withDayOfMonth(1).startMillis()
                to = monthStart()
            }
            "last30" -> {
                from = today.minusDays(29).startMillis()
                to = today.plusDays(1).startMillis()
            }
            "year" -> {
                from = today.withDayOfYear(1).startMillis()
                to = today.withDayOfYear(1).plusYears(1).startMillis()
            }
            "custom" ->
                if (from == 0L && to == 0L) {
                    from = monthStart()
                    to = todayMillis() + DAY
                }
        }
    }

    fun summary(
        names: List<String>,
        all: String,
    ) = when {
        names.isEmpty() -> all
        names.size <= 2 -> names.joinToString(", ")
        else -> names.take(2).joinToString(", ") + " +${names.size - 2}"
    }

    val periodName = periods.firstOrNull { it.first == period }?.second ?: str(R.string.any)
    val periodDates =
        if (from == 0L && to == 0L) {
            null
        } else {
            val a = if (from > 0L) fullDate(from) else str(R.string.any)
            val b = if (to > 0L) fullDate(to - DAY) else str(R.string.any)
            "$a – $b"
        }
    val accountSummary = summary(live.filter { it.id in accs }.map { it.name }, "All accounts")
    val categorySummary = summary(parents.filter { it.id in cats }.map { it.name }, "All categories")

    val minPaise = parseRupees(min) ?: 0L
    val maxPaise = parseRupees(max) ?: 0L
    val badRange = minPaise > 0L && maxPaise > 0L && minPaise > maxPaise

    val activeCount =
        (if (query.isNotBlank()) 1 else 0) +
                (if (kinds.isNotEmpty()) 1 else 0) +
                (if (from != 0L || to != 0L) 1 else 0) +
                (if (accs.isNotEmpty()) 1 else 0) +
                (if (cats.isNotEmpty()) 1 else 0) +
                (if (min.isNotBlank()) 1 else 0) +
                (if (max.isNotBlank()) 1 else 0)

    ScreenScaffold(
        str(R.string.filter),
        { nav.popBackStack() },
        actions = {
            Text(
                str(R.string.reset),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.clip(CircleShape).clickable {
                        query = ""
                        kinds = emptySet()
                        accs = emptySet()
                        cats = emptySet()
                        from = 0L
                        to = 0L
                        period = "any"
                        min = ""
                        max = ""
                        periodOpen = false
                        accountsOpen = false
                        categoriesOpen = false
                    }.padding(10.dp),
            )
        },
        bottomBar = {
            BottomActionBar {
                PrimaryButton(
                    if (activeCount == 0) str(R.string.apply_filter) else "Apply $activeCount filter${if (activeCount > 1) "s" else ""}",
                    {
                        TxnFilter.query = query
                        TxnFilter.kinds = kinds
                        TxnFilter.accountIds = accs
                        TxnFilter.categoryIds = cats
                        TxnFilter.fromMillis = from
                        TxnFilter.toMillis = to
                        TxnFilter.minPaise = minPaise
                        TxnFilter.maxPaise = maxPaise
                        nav.popBackStack()
                    },
                    enabled = !badRange,
                )
            }
        },
    ) {
        AppTextField(query, { query = it }, str(R.string.search_notes_and_categories), leading = { Icon(Icons.Filled.Search, null) })

        // ── Type (none selected = all types) ──────────────────────────────────
        Card {
            TypeTiles(selected = kinds, onToggle = { kinds = kinds.flip(it) }, emptyMeansAll = true)
        }

        // ── Date ──────────────────────────────────────────────────────────────
        Card(padding = 0.dp) {
            FilterRow(
                icon = Icons.Filled.CalendarMonth,
                overline = str(R.string.dates),
                title = periodName,
                subtitle = if (period == "any") null else periodDates,
                onClick = { periodOpen = !periodOpen },
            ) { FilterChevron(periodOpen) }
            AnimatedVisibility(periodOpen) {
                Column {
                    periods.forEach { (k, v) ->
                        FilterHairline()
                        FilterOption(v, selected = period == k) {
                            choosePeriod(k)
                            periodOpen = false
                        }
                    }
                }
            }
            if (period == "custom") {
                FilterHairline()
                Column(Modifier.padding(horizontal = 12.dp)) {
                    PickRow(str(R.string.from), if (from > 0L) fullDate(from) else str(R.string.any), Icons.Filled.CalendarMonth, Muted, {
                        target = "from"
                        nav.navigate(Routes.datePicker(if (from > 0L) from else todayMillis()))
                    })
                    PickRow(str(R.string.to_2), if (to > 0L) fullDate(to - DAY) else str(R.string.any), Icons.Filled.CalendarMonth, Muted, {
                        target = "to"
                        nav.navigate(Routes.datePicker(if (to > 0L) to - DAY else todayMillis()))
                    }, divider = false)
                }
            }
        }

        // ── Accounts ──────────────────────────────────────────────────────────
        if (live.size > 1) {
            Card(padding = 0.dp) {
                FilterRow(
                    icon = Icons.Filled.AccountBalanceWallet,
                    overline = str(R.string.accounts),
                    title = accountSummary,
                    onClick = { accountsOpen = !accountsOpen },
                ) { FilterChevron(accountsOpen) }
                AnimatedVisibility(accountsOpen) {
                    Column {
                        FilterHairline()
                        FilterOption("All accounts", selected = accs.isEmpty()) { accs = emptySet() }
                        live.forEach { a ->
                            FilterHairline()
                            FilterOption(a.name, selected = a.id in accs) {
                                val next = accs.flip(a.id)
                                // Picking every account is the same as "All accounts".
                                accs = if (next.size == live.size) emptySet() else next
                            }
                        }
                    }
                }
            }
        }

        // ── Categories (main categories only, as before) ──────────────────────
        if (parents.isNotEmpty()) {
            Card(padding = 0.dp) {
                FilterRow(
                    icon = Icons.Filled.Category,
                    overline = str(R.string.categories),
                    title = categorySummary,
                    onClick = { categoriesOpen = !categoriesOpen },
                ) { FilterChevron(categoriesOpen) }
                AnimatedVisibility(categoriesOpen) {
                    Column {
                        FilterHairline()
                        FilterOption("All categories", selected = cats.isEmpty()) { cats = emptySet() }
                        parents.forEach { c ->
                            FilterHairline()
                            FilterOption(c.name, selected = c.id in cats) {
                                val next = cats.flip(c.id)
                                cats = if (next.size == parents.size) emptySet() else next
                            }
                        }
                    }
                }
            }
        }

        // ── Amount ────────────────────────────────────────────────────────────
        Card {
            Text(str(R.string.amount), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    AppTextField(min, { v -> if (v.all { it.isDigit() || it == '.' }) min = v }, str(R.string.min), keyboard = KeyboardType.Decimal)
                }
                Text("to", color = Muted, fontSize = 12.sp)
                Box(Modifier.weight(1f)) {
                    AppTextField(max, { v -> if (v.all { it.isDigit() || it == '.' }) max = v }, str(R.string.max), keyboard = KeyboardType.Decimal)
                }
            }
            if (badRange) {
                Spacer(Modifier.height(6.dp))
                Text("Min can't be higher than max", color = Danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Settings-style row: tinted icon badge, small label above the title, optional subtitle, trailing slot. */
@Composable
private fun FilterRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    overline: String? = null,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            if (overline != null) Text(overline, color = Muted, fontSize = 12.sp)
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        trailing()
    }
}

/** One selectable line inside an expanded list, indented to line up with the row titles. */
@Composable
private fun FilterOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 58.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            Modifier.weight(1f),
            color = Ink,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FilterChevron(open: Boolean) {
    Icon(
        if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
        contentDescription = if (open) "Collapse" else "Expand",
        tint = Muted,
    )
}

@Composable
private fun FilterHairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

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
                    Modifier.clip(CircleShape).clickable {
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

    ScreenScaffold(str(R.string.bulk_actions), { nav.popBackStack() }) {
        HeroCard {
            Text(str(R.string.selected, picked.size), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            Text("-" + formatPaise(spent(picked)), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
            if (earned(picked) > 0) {
                Text(
                    "+" + formatPaise(earned(picked)) + " income",
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

// ------------------------------------------------------------------ Report filter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportFilterScreen(
    nav: NavController,
    entry: NavBackStackEntry,
) {
    val accounts by Repo.accounts.collectAsState()
    var period by rememberSaveable { mutableStateOf(ReportFilter.period) }
    var from by rememberSaveable { mutableStateOf(ReportFilter.customFrom.takeIf { it > 0 } ?: monthStart()) }
    var to by rememberSaveable { mutableStateOf(ReportFilter.customTo.takeIf { it > 0 } ?: todayMillis()) }
    var kind by rememberSaveable { mutableStateOf(ReportFilter.kind) }
    var accs by rememberSaveable { mutableStateOf(ReportFilter.accountIds) }
    var target by rememberSaveable { mutableStateOf("from") }
    entry.Picked<Long>("pickedDate") { if (target == "from") from = it else to = it }

    ScreenScaffold(str(R.string.report_filter), { nav.popBackStack() }, bottomBar = {
        BottomActionBar {
            PrimaryButton(str(R.string.apply), {
                ReportFilter.period = period
                ReportFilter.customFrom = from
                ReportFilter.customTo = maxOf(from, to)
                ReportFilter.kind = kind
                ReportFilter.accountIds = accs
                nav.popBackStack()
            })
        }
    }) {
        SectionTitle(str(R.string.period))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "month" to str(R.string.this_month),
                "last30" to str(R.string.last_30_days),
                "year" to str(R.string.this_year),
                "custom" to str(R.string.custom),
            ).forEach {
                    (k, l) ->
                SelectChip(l, period == k) { period = k }
            }
        }
        if (period == "custom") {
            Card(padding = 12.dp) {
                PickRow(str(R.string.from), fullDate(from), Icons.Filled.CalendarMonth, Muted, {
                    target = "from"
                    nav.navigate(Routes.datePicker(from))
                })
                PickRow(str(R.string.to_2), fullDate(to), Icons.Filled.CalendarMonth, Muted, {
                    target = "to"
                    nav.navigate(Routes.datePicker(to))
                }, divider = false)
            }
        }
        SectionTitle(str(R.string.show))
        SegmentedControl(listOf(str(R.string.spending), str(R.string.income)), if (kind == "income") 1 else 0, {
            kind = if (it == 1) "income" else "expense"
        })
        SectionTitle(str(R.string.accounts))
        Hint(str(R.string.leave_empty_to_include_all_accounts))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.filter { !it.archived }.forEach {
                    a ->
                SelectChip(a.name, a.id in accs) { accs = if (a.id in accs) accs - a.id else accs + a.id }
            }
        }
    }
}

// ------------------------------------------------------------------ Category picker and editors

@Composable
fun CategoryPickerScreen(
    nav: NavController,
    kind: String,
    slot: String,
) {
    val look = rememberLookup()
    val withSubs = slot == "category" // budgets, templates, filters and bulk edits pick the main category only
    val manage = slot == "manage"
    // In Settings one screen holds both lists; everywhere else the kind is fixed by the caller.
    var shownKind by rememberSaveable { mutableStateOf(kind) }
    val activeKind = if (manage) shownKind else kind
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(setOf<String>()) }
    val parents =
        look.parents(activeKind).filter {
                p ->
            query.isBlank() || p.name.contains(query, true) || look.subs(p.id).any { it.name.contains(query, true) }
        }
    ScreenScaffold(
        if (slot == "manage") str(R.string.categories) else str(R.string.select_category),
        { nav.popBackStack() },
        // Adding and editing categories happens only in Settings. While choosing for a transaction, it is pick-only.
        actions = {
            if (manage) RoundIconButton(Icons.Filled.Add, str(R.string.new_category), { nav.navigate(Routes.categoryEdit("", activeKind)) })
        },
    ) {
        if (manage) {
            SegmentedControl(listOf(str(R.string.expense), str(R.string.income)), if (activeKind == "income") 1 else 0, {
                shownKind = if (it == 1) "income" else "expense"
                query = ""
                open = emptySet()
            })
        }
        AppTextField(query, { query = it }, str(R.string.search), leading = { Icon(Icons.Filled.Search, null) })
        if (parents.isEmpty()) {
            EmptyState(
                Icons.Filled.Category,
                str(R.string.no_categories),
                if (slot == "manage") str(R.string.tap_the_plus_button_to_create) else "Add categories in Settings > Categories.",
            )
        }
        parents.forEach { p ->
            val subs = look.subs(p.id)
            Card(padding = 12.dp) {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (slot == "manage") {
                            nav.navigate(Routes.categoryEdit(p.id, activeKind))
                        } else {
                            nav.setPicked("pickedCategory", "${p.id}|")
                            nav.popBackStack()
                        }
                    }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconCircle(p.icon, p.colorKey, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.ExtraBold, color = Ink)
                        if (withSubs && subs.isNotEmpty()) Text(str(R.string.sub_categories_2, subs.size), color = Muted, fontSize = 12.sp)
                    }
                    if (slot == "manage") {
                        RoundIconButton(
                            Icons.Filled.Edit,
                            str(R.string.edit_2, p.name),
                            { nav.navigate(Routes.categoryEdit(p.id, activeKind)) },
                            bg = Bg,
                        )
                    }
                    if (withSubs && subs.isNotEmpty()) {
                        Icon(
                            if (p.id in open || query.isNotBlank()) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            str(R.string.show_sub_categories),
                            tint = Muted,
                            modifier =
                                Modifier.clip(
                                    CircleShape,
                                ).clickable { open = if (p.id in open) open - p.id else open + p.id }.padding(6.dp),
                        )
                    }
                }
                if (withSubs && (p.id in open || query.isNotBlank())) {
                    subs.filter { query.isBlank() || it.name.contains(query, true) || p.name.contains(query, true) }.forEach { s ->
                        HorizontalDivider(color = Line)
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                nav.setPicked("pickedCategory", "${p.id}|${s.id}")
                                nav.popBackStack()
                            }.padding(start = 24.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconCircle(s.icon, p.colorKey, 34.dp)
                            Text(s.name, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
    kind: String,
) {
    val look = rememberLookup()
    val existing = remember(id, look.categories.isEmpty()) { look.category(id) }
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable(existing) { mutableStateOf(existing?.icon ?: "other") }
    var color by rememberSaveable(existing) { mutableStateOf(existing?.colorKey ?: "blue") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    entry.Picked<String>("pickedColor") { color = it }
    val subs = look.subs(id)
    ScreenScaffold(
        if (existing == null) str(R.string.new_category) else str(R.string.edit_category),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, name.isNotBlank(), onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put("categories", id, Category(name = name.trim(), icon = icon, colorKey = color, kind = existing?.kind ?: kind))
                    // Sub-categories follow the parent colour.
                    if (existing != null && existing.colorKey != color) {
                        subs.forEach {
                            Repo.put(
                                "categories",
                                it.id,
                                it.copy(colorKey = color),
                            )
                        }
                    }
                }
            })
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, str(R.string.category_name))
        Card(padding = 12.dp) {
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
        SectionTitle(str(R.string.sub_categories))
        if (existing == null) {
            Hint(str(R.string.save_the_category_first_then_open))
        } else {
            Card(padding = 12.dp) {
                subs.forEach { s ->
                    SettingRow(s.name, leading = { IconCircle(s.icon, color, 38.dp) }, onClick = { nav.navigate(Routes.subEdit(s.id, id)) })
                }
                SettingRow(str(R.string.add_sub_category), icon = Icons.Filled.Add, iconColor = Primary, onClick = {
                    nav.navigate(Routes.subEdit("", id))
                }, divider = false)
            }
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_category), str(R.string.its_sub_categories_are_deleted_too), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.deleteCategory(id) }
        }, { confirm = false }, danger = true)
    }
}

@Composable
fun SubcategoryEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
    parentId: String,
) {
    val look = rememberLookup()
    val parent = look.category(parentId)
    val existing = remember(id, look.categories.isEmpty()) { look.category(id) }
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable(existing) { mutableStateOf(existing?.icon ?: parent?.icon ?: "other") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    val color = parent?.colorKey ?: "slate"
    ScreenScaffold(
        if (existing == null) str(R.string.new_sub_category) else str(R.string.edit_sub_category),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, name.isNotBlank() && parent != null, onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "categories",
                        id,
                        Category(name = name.trim(), icon = icon, colorKey = color, parentId = parentId, kind = parent!!.kind),
                    )
                }
            })
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, str(R.string.sub_category_name))
        Card(padding = 12.dp) {
            PickRowLeading(str(R.string.icon), iconLabel(icon), {
                IconCircle(icon, color, 38.dp)
            }, { nav.navigate(Routes.iconPicker("category", icon)) })
            SettingRow(
                str(R.string.colour),
                str(R.string.same_as_inherited, parent?.name.orEmpty()),
                leading = { Box(Modifier.size(38.dp).clip(CircleShape).background(colorSpec(color).brush)) },
                trailing = { Icon(Icons.Filled.Lock, null, tint = Faint) },
                divider = false,
            )
        }
        Hint(str(R.string.sub_categories_always_use_their_catego))
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_sub_category), str(R.string.past_transactions_keep_their_category), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("categories", id) }
        }, { confirm = false }, danger = true)
    }
}

// ------------------------------------------------------------------ Colour, icon, account, date pickers

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerScreen(
    nav: NavController,
    current: String,
    locked: String,
) {
    var chosen by remember { mutableStateOf(current.ifBlank { "blue" }) }

    @Composable fun Swatch(c: ColorSpec) {
        val on = c.key == chosen
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(c.brush).let {
                if (on) it.border(3.dp, Ink, CircleShape) else it
            }.clickable { chosen = c.key },
            contentAlignment = Alignment.Center,
        ) { if (on) Icon(Icons.Filled.Check, str(R.string.selected_2), tint = Color.White) }
    }
    ScreenScaffold(str(R.string.choose_colour), { nav.popBackStack() }, bottomBar = {
        BottomActionBar {
            PrimaryButton(str(R.string.use_2, colorSpec(chosen).label), {
                nav.setPicked("pickedColor", chosen)
                nav.popBackStack()
            })
        }
    }) {
        Box(
            Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(28.dp)).background(colorSpec(chosen).brush),
            contentAlignment = Alignment.Center,
        ) {
            Text(colorSpec(chosen).label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
        SectionTitle(str(R.string.solid))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SolidColors.forEach { Swatch(it) }
        }
        SectionTitle(str(R.string.gradients))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GradientColors.forEach { Swatch(it) }
        }
        Hint(str(R.string.the_same_colours_and_gradients_are))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerScreen(
    nav: NavController,
    group: String,
    current: String,
) {
    val groups = if (group == "account") AccountIconGroups else CategoryIconGroups
    var chosen by remember { mutableStateOf(current.ifBlank { "other" }) }
    ScreenScaffold(if (group == "account") str(R.string.choose_account_icon) else str(R.string.choose_category_icon), {
        nav.popBackStack()
    }, bottomBar = {
        BottomActionBar {
            PrimaryButton(str(R.string.use_this_icon), {
                nav.setPicked("pickedIcon", chosen)
                nav.popBackStack()
            })
        }
    }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(chosen, "blue", 84.dp) }
        groups.forEach { g ->
            SectionTitle(g.title)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                g.keys.forEach { k ->
                    val on = k == chosen
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(if (on) PrimaryContainer else Surface)
                            .let { if (on) it.border(2.dp, Primary, RoundedCornerShape(20.dp)) else it }.clickable { chosen = k },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(iconFor(k), iconLabel(k), tint = if (on) Primary else Muted, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AccountPickerScreen(
    nav: NavController,
    slot: String,
    exclude: String,
) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val list = accounts.filter { !it.archived && it.id != exclude }
    ScreenScaffold(
        str(R.string.select_account),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.new_account), { nav.navigate(Routes.ACCOUNT_TYPE) }) },
    ) {
        if (list.isEmpty()) {
            EmptyState(Icons.Filled.AccountBalance, str(R.string.no_accounts_yet), str(R.string.add_a_bank_wallet_cash_or_2))
            PrimaryButton(str(R.string.add_account), { nav.navigate(Routes.ACCOUNT_TYPE) })
        } else {
            Card(padding = 12.dp) {
                list.forEachIndexed { i, a ->
                    val bal = Repo.balanceOf(a.id, accounts, txns)
                    SettingRow(
                        a.name,
                        leading = { IconCircle(a.icon, a.colorKey, 44.dp) },
                        onClick = {
                            nav.setPicked(if (slot == "to") "pickedAccount2" else "pickedAccount", a.id)
                            nav.popBackStack()
                        },
                        trailing = { Text(formatPaise(bal), fontWeight = FontWeight.ExtraBold, color = if (bal < 0) Danger else Ink) },
                        divider = i < list.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
fun DatePickerScreen(
    nav: NavController,
    millis: Long,
) {
    val start = if (millis > 0) toLocalDate(millis) else LocalDate.now()
    var ym by remember { mutableStateOf(YearMonth.from(start)) }
    var picked by remember { mutableStateOf(start) }
    ScreenScaffold(
        str(R.string.select_date),
        { nav.popBackStack() },
        actions = {
            Text(
                str(R.string.today),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.clip(CircleShape).clickable {
                        picked = LocalDate.now()
                        ym = YearMonth.now()
                    }.padding(10.dp),
            )
        },
        bottomBar = {
            BottomActionBar {
                PrimaryButton(str(R.string.use_2, fullDate(picked.startMillis())), {
                    nav.setPicked("pickedDate", picked.startMillis())
                    nav.popBackStack()
                })
            }
        },
    ) {
        Card {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    str(R.string.previous_month),
                    Modifier.clip(CircleShape).clickable {
                        ym = ym.minusMonths(1)
                    }.padding(8.dp),
                )
                Text(
                    str(R.string.text__4, ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()), ym.year),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    fontSize = 16.sp,
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    str(R.string.next_month),
                    Modifier.clip(CircleShape).clickable {
                        ym = ym.plusMonths(1)
                    }.padding(8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(
                        it,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
            val lead = ym.atDay(1).dayOfWeek.value - 1
            val days = ym.lengthOfMonth()
            for (r in 0 until (lead + days + 6) / 7) Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val d = ym.atDay(day)
                            val on = d == picked
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(if (on) Primary else Color.Transparent)
                                    .let {
                                        if (d == LocalDate.now() && !on) {
                                            it.border(
                                                1.5.dp,
                                                Primary,
                                                CircleShape,
                                            )
                                        } else {
                                            it
                                        }
                                    }.clickable { picked = d },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(str(R.string.text__2, day), color = if (on) Color.White else Ink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}