package com.kdevs.spendly.ui.screens.pickers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.money.PickRow
import com.kdevs.spendly.ui.state.TxnFilter
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate

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

    val periods =
        listOf(
            "any" to str(R.string.any),
            "month" to str(R.string.this_month),
            "lastmonth" to str(R.string.last_month),
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
    val accountSummary = summary(live.filter { it.id in accs }.map { it.name }, str(R.string.all_accounts))
    val categorySummary = summary(parents.filter { it.id in cats }.map { it.name }, str(R.string.all_categories))

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
                        FilterOption(str(R.string.all_accounts), selected = accs.isEmpty()) { accs = emptySet() }
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
                        FilterOption(str(R.string.all_categories), selected = cats.isEmpty()) { cats = emptySet() }
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
                Text(str(R.string.to_lower), color = Muted, fontSize = 12.sp)
                Box(Modifier.weight(1f)) {
                    AppTextField(max, { v -> if (v.all { it.isDigit() || it == '.' }) max = v }, str(R.string.max), keyboard = KeyboardType.Decimal)
                }
            }
            if (badRange) {
                Spacer(Modifier.height(6.dp))
                Text(str(R.string.min_higher_than_max), color = Danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
        if (selected) Icon(Icons.Filled.Check, contentDescription = str(R.string.selected_2), tint = Primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FilterChevron(open: Boolean) {
    Icon(
        if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
        contentDescription = if (open) str(R.string.collapse) else str(R.string.expand),
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
