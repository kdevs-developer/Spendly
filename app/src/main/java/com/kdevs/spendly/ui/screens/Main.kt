package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.state.TxnFilter
import com.kdevs.spendly.ui.theme.*
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ------------------------------------------------------------------ Home

@Composable
fun HomeScreen(nav: NavController) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val budgets by Repo.budgets.collectAsState()
    val profile by Repo.profile.collectAsState()
    val look = rememberLookup()

    val live = accounts.filter { !it.archived }
    val total = live.sumOf { Repo.balanceOf(it.id, accounts, txns) }
    val month = inRange(txns, monthStart(), nextMonthStart())
    val spentNow = spent(month)
    val overall = budgets.firstOrNull { it.categoryId.isBlank() }

    TabScreen(
        greeting(profile.name),
        Routes.HOME,
        nav,
        actions = { RoundIconButton(Icons.Filled.Settings, str(R.string.settings), { nav.navigate(Routes.SETTINGS) }) },
        fab = { Fab(Icons.Filled.Add, str(R.string.add_transaction)) { nav.navigate(Routes.txnAdd()) } },
    ) {
        HeroCard {
            Text(str(R.string.total_balance), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(formatPaise(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroStat(str(R.string.spent_this_month), "-" + formatPaise(spentNow), Modifier.weight(1f))
                HeroStat(str(R.string.earned_this_month), formatPaise(earned(month)), Modifier.weight(1f))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(
                str(R.string.expense),
                Icons.Filled.RemoveCircle,
                Danger,
                Modifier.weight(1f),
            ) { nav.navigate(Routes.txnAdd("expense")) }
            QuickAction(
                str(R.string.income),
                Icons.Filled.AddCircle,
                Success,
                Modifier.weight(1f),
            ) { nav.navigate(Routes.txnAdd("income")) }
            QuickAction(str(R.string.transfer), Icons.Filled.SwapHoriz, Primary, Modifier.weight(1f)) { nav.navigate(Routes.TRANSFER) }
        }

        if (overall != null) {
            Card(onClick = { nav.navigate(Routes.BUDGETS) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(str(R.string.monthly_budget), fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        str(R.string.of_2, formatPaise(spentNow), formatPaise(overall.limitPaise)),
                        color = Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                val over = spentNow > overall.limitPaise
                LabeledBar(
                    "",
                    spentNow,
                    overall.limitPaise,
                    if (over) Danger else Primary,
                    if (over) {
                        str(
                            R.string.over_by,
                            formatPaise(spentNow - overall.limitPaise),
                        )
                    } else {
                        str(R.string.amount_left, formatPaise(overall.limitPaise - spentNow))
                    },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(str(R.string.recent))
            Text(
                str(R.string.see_all),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.clip(CircleShape).clickable {
                        nav.navigate(Routes.ACTIVITY)
                    }.padding(8.dp),
            )
        }
        val txnsLoaded = "txns" in Repo.loaded.collectAsState().value
        if (txns.isEmpty() && !txnsLoaded) {
            InlineLoader()
        } else if (txns.isEmpty()) {
            EmptyState(Icons.Filled.Receipt, str(R.string.no_transactions_yet), str(R.string.tap_the_plus_button_to_add))
        } else {
            Card(padding = 12.dp) {
                val recent = txns.sortedByDescending { it.dateMillis }.take(5)
                recent.forEachIndexed { i, t ->
                    TxnRow(t, look, { nav.navigate(Routes.txnDetail(t.id)) }, showDate = true)
                    if (i < recent.lastIndex) HorizontalDivider(color = Line)
                }
            }
        }

        SectionTitle(str(R.string.plan_ahead))
        Card(padding = 12.dp) {
            SettingRow(str(R.string.budgets), icon = Icons.Filled.DonutLarge, iconColor = clr(R.color.swatch_purple), onClick = {
                nav.navigate(Routes.BUDGETS)
            })
            SettingRow(str(R.string.savings_goals), icon = Icons.Filled.Flag, iconColor = clr(R.color.swatch_green), onClick = {
                nav.navigate(Routes.GOALS)
            })
            SettingRow(str(R.string.templates), icon = Icons.Filled.ContentCopy, iconColor = clr(R.color.swatch_teal), onClick = {
                nav.navigate(Routes.TEMPLATES)
            })
            SettingRow(str(R.string.recurring), icon = Icons.Filled.Repeat, iconColor = clr(R.color.swatch_orange), onClick = {
                nav.navigate(Routes.RECURRING)
            }, divider = false)
        }
        Spacer(Modifier.height(70.dp))
    }
}

/** "Good evening, Krish" - first name only, by the hour. Falls back to a plain hello. */
private fun greeting(fullName: String): String {
    val first = fullName.trim().substringBefore(' ')
    val hour = java.time.LocalTime.now().hour
    val part =
        when {
            hour < 5 -> str(R.string.hello)
            hour < 12 -> str(R.string.good_morning)
            hour < 17 -> str(R.string.good_afternoon)
            hour < 22 -> str(R.string.good_evening)
            else -> str(R.string.hello)
        }
    return if (first.isBlank() || first == str(R.string.guest)) part else "$part, $first"
}

@Composable
private fun HeroStat(
    label: String,
    value: String,
    modifier: Modifier,
) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.14f)).padding(12.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Surface).clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        Text(label, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
    }
}

// ------------------------------------------------------------------ Activity

@Composable
fun ActivityScreen(nav: NavController) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    var view by remember { mutableIntStateOf(0) } // 0 list, 1 calendar
    val filtered =
        remember(
            txns, look, TxnFilter.query, TxnFilter.kinds, TxnFilter.accountIds, TxnFilter.categoryIds,
            TxnFilter.fromMillis, TxnFilter.toMillis, TxnFilter.minPaise, TxnFilter.maxPaise,
        ) {
            txns.filter { TxnFilter.matches(it) { id -> look.categoryName(id) } }
        }

    TabScreen(
        str(R.string.activity),
        Routes.ACTIVITY,
        nav,
        actions = {
            Box {
                RoundIconButton(Icons.Filled.FilterList, str(R.string.filter), { nav.navigate(Routes.FILTER) })
                if (TxnFilter.activeCount > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(Danger),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            str(R.string.text__2, TxnFilter.activeCount),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            RoundIconButton(Icons.Filled.FileDownload, "Export", { nav.navigate(Routes.EXPORT) })
            Spacer(Modifier.width(8.dp))
            RoundIconButton(Icons.Filled.Checklist, str(R.string.select), { nav.navigate(Routes.SELECT) })
        },
        fab = { Fab(Icons.Filled.Add, str(R.string.add_transaction)) { nav.navigate(Routes.txnAdd()) } },
    ) {
        SegmentedControl(listOf(str(R.string.list), str(R.string.calendar)), view, { view = it })
        if (view == 0) {
            if (filtered.isEmpty() && txns.isEmpty() && "txns" !in Repo.loaded.collectAsState().value) {
                InlineLoader()
            } else if (filtered.isEmpty()) {
                EmptyState(
                    Icons.Filled.SearchOff,
                    str(R.string.nothing_here),
                    if (TxnFilter.activeCount > 0) {
                        str(
                            R.string.no_transactions_match_your_filter,
                        )
                    } else {
                        str(R.string.add_a_transaction_to_see_it)
                    },
                )
            } else {
                DayGroups(filtered, look, { nav.navigate(Routes.txnDetail(it.id)) })
            }
        } else {
            CalendarView(filtered, look, nav)
        }
        Spacer(Modifier.height(70.dp))
    }
}

@Composable
private fun CalendarView(
    list: List<Txn>,
    look: Lookup,
    nav: NavController,
) {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var picked by remember { mutableStateOf(LocalDate.now()) }
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
        val lead = ym.atDay(1).dayOfWeek.value - 1
        val days = ym.lengthOfMonth()
        val byDay =
            list.filter { it.kind == "expense" }.groupBy { toLocalDate(it.dateMillis) }.mapValues {
                    e ->
                e.value.sumOf { it.amountPaise }
            }
        val rows = (lead + days + 6) / 7
        for (r in 0 until rows) Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            for (c in 0 until 7) {
                val day = r * 7 + c - lead + 1
                Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                    if (day in 1..days) {
                        val date = ym.atDay(day)
                        val on = date == picked
                        val total = byDay[date] ?: 0L
                        Column(
                            Modifier.fillMaxSize().padding(1.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (on) Primary else Color.Transparent).clickable { picked = date },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                str(R.string.text__2, day),
                                color = if (on) Color.White else Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                            if (total > 0) {
                                Text(
                                    compact(total),
                                    color = if (on) Color.White.copy(alpha = 0.85f) else Danger,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    val dayList = list.filter { toLocalDate(it.dateMillis) == picked }.sortedByDescending { it.dateMillis }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionTitle(dayLabel(picked.startMillis()))
        if (dayList.isNotEmpty()) Text("-" + formatPaise(spent(dayList)), color = Muted, fontWeight = FontWeight.Bold)
    }
    if (dayList.isEmpty()) {
        Hint(str(R.string.nothing_on_this_day))
    } else {
        Card(padding = 12.dp) {
            dayList.forEachIndexed { i, t ->
                TxnRow(t, look, { nav.navigate(Routes.txnDetail(t.id)) })
                if (i < dayList.lastIndex) HorizontalDivider(color = Line)
            }
        }
    }
}

/** 12,400 paise-rupees shown as "12k" so it fits a calendar cell. */
private fun compact(paise: Long): String {
    val r = paise / 100
    return when {
        r >= 100000 -> "${r / 100000}L"
        r >= 1000 -> "${r / 1000}k"
        else -> "$r"
    }
}

// ------------------------------------------------------------------ Reports

@Composable
fun ReportsScreen(nav: NavController) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    var view by remember { mutableIntStateOf(0) }
    val (from, to) = reportRange()
    val scoped =
        remember(txns, from, to, ReportFilter.accountIds, ReportFilter.kind) {
            inRange(txns, from, to).filter { t ->
                t.kind == ReportFilter.kind && (ReportFilter.accountIds.isEmpty() || t.accountId in ReportFilter.accountIds)
            }
        }

    TabScreen(
        str(R.string.reports),
        Routes.REPORTS,
        nav,
        actions = { RoundIconButton(Icons.Filled.Tune, str(R.string.report_filter), { nav.navigate(Routes.REPORT_FILTER) }) },
    ) {
        SegmentedControl(listOf(str(R.string.category), str(R.string.trends)), view, { view = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(periodLabel(), color = Muted, fontWeight = FontWeight.Bold)
            Text(
                if (ReportFilter.kind == "expense") str(R.string.spending) else str(R.string.income),
                color = Primary,
                fontWeight = FontWeight.Bold,
            )
        }
        if (view == 0) CategoryReport(scoped, look, nav) else TrendReport(txns)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryReport(
    list: List<Txn>,
    look: Lookup,
    nav: NavController,
) {
    val total = list.sumOf { it.amountPaise }
    val byCat = list.groupBy { it.categoryId }.map { (id, l) -> id to l.sumOf { it.amountPaise } }.sortedByDescending { it.second }
    if (total == 0L) {
        EmptyState(Icons.Filled.PieChart, str(R.string.no_data), str(R.string.nothing_recorded_for_this_period_and))
        return
    }
    Card {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(180.dp)) {
                var start = -90f
                val stroke = 34.dp.toPx()
                byCat.forEach { (id, v) ->
                    val sweep = 360f * v / total
                    drawArc(
                        colorSpec(look.category(id)?.colorKey ?: "slate").solid,
                        start,
                        (sweep - 1.5f).coerceAtLeast(0.5f),
                        false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(str(R.string.total), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(formatPaise(total), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        }
    }
    Card(padding = 12.dp) {
        byCat.forEachIndexed { i, (id, v) ->
            val c = look.category(id)
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate(Routes.reportCategory(id)) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(c?.name ?: str(R.string.uncategorised), fontWeight = FontWeight.Bold, color = Ink)
                    Text(str(R.string.of_total, (v * 100 / total)), color = Muted, fontSize = 12.sp)
                }
                Text(formatPaise(v), fontWeight = FontWeight.ExtraBold, color = Ink)
                Icon(Icons.Filled.ChevronRight, null, tint = Faint)
            }
            if (i < byCat.lastIndex) HorizontalDivider(color = Line)
        }
    }
}

/** One bar group in the trend chart: a day, a week, a month or a year. */
private class TrendBucket(val short: String, val long: String, val from: Long, val to: Long)

private fun trendBuckets(gran: Int): List<TrendBucket> {
    val today = LocalDate.now()
    val loc = Locale.getDefault()
    return when (gran) {
        0 ->
            (13 downTo 0).map {
                val d = today.minusDays(it.toLong())
                TrendBucket(
                    d.dayOfMonth.toString(),
                    d.format(DateTimeFormatter.ofPattern("EEE, d MMM", loc)),
                    d.startMillis(),
                    d.plusDays(1).startMillis(),
                )
            }
        1 -> {
            val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (7 downTo 0).map {
                val w = thisWeek.minusWeeks(it.toLong())
                val f = DateTimeFormatter.ofPattern("d MMM", loc)
                TrendBucket(
                    w.format(f),
                    w.format(f) + " – " + w.plusDays(6).format(f),
                    w.startMillis(),
                    w.plusWeeks(1).startMillis(),
                )
            }
        }
        3 ->
            (4 downTo 0).map {
                val y = today.withDayOfYear(1).minusYears(it.toLong())
                TrendBucket(y.year.toString(), y.year.toString(), y.startMillis(), y.plusYears(1).startMillis())
            }
        else ->
            (5 downTo 0).map {
                val m = today.withDayOfMonth(1).minusMonths(it.toLong())
                TrendBucket(
                    m.month.getDisplayName(TextStyle.SHORT, loc),
                    m.format(DateTimeFormatter.ofPattern("MMM yyyy", loc)),
                    m.startMillis(),
                    m.plusMonths(1).startMillis(),
                )
            }
    }
}

@Composable
private fun TrendReport(allTxns: List<Txn>) {
    var gran by rememberSaveable { mutableIntStateOf(2) } // 0 day, 1 week, 2 month, 3 year
    val txns = allTxns.filter { ReportFilter.accountIds.isEmpty() || it.accountId in ReportFilter.accountIds }
    val buckets = remember(gran) { trendBuckets(gran) }
    val data =
        buckets.map { b ->
            val l = inRange(txns, b.from, b.to)
            Triple(b, spent(l), earned(l))
        }
    val max = (data.maxOfOrNull { maxOf(it.second, it.third) } ?: 0L).coerceAtLeast(1L)
    val barWidth = when (gran) { 0 -> 7.dp; 1 -> 12.dp; 3 -> 18.dp; else -> 14.dp }
    val title =
        when (gran) {
            0 -> str(R.string.last_14_days)
            1 -> str(R.string.last_8_weeks)
            3 -> str(R.string.last_5_years)
            else -> str(R.string.last_6_months)
        }
    SegmentedControl(
        listOf(str(R.string.trend_day), str(R.string.trend_week), str(R.string.trend_month), str(R.string.trend_year)),
        gran,
        { gran = it },
    )
    Card {
        Text(title, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEach { (b, s, e) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        Modifier.fillMaxWidth().height(170.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                    ) {
                        Box(
                            Modifier.width(barWidth).fillMaxHeight((s.toFloat() / max).coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(4.dp)).background(Danger),
                        )
                        Box(
                            Modifier.width(barWidth).fillMaxHeight((e.toFloat() / max).coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(4.dp)).background(Success),
                        )
                    }
                    Text(
                        b.short,
                        modifier = Modifier.padding(top = 8.dp),
                        color = Muted,
                        fontSize = if (gran == 1) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(Danger, str(R.string.spent))
            Legend(Success, str(R.string.earned))
        }
    }
    Card(padding = 12.dp) {
        data.reversed().forEachIndexed { i, (b, s, e) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(b.long, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f), maxLines = 1)
                Text("-" + formatPaise(s), color = Danger, fontWeight = FontWeight.Bold)
                Text(
                    formatPaise(e, true).let { if (e == 0L) "₹0" else it },
                    color = Success,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 64.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            if (i < data.lastIndex) HorizontalDivider(color = Line)
        }
    }
}

@Composable
private fun Legend(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Drill-down: one category's transactions in the report period, split by sub-category. */
@Composable
fun ReportCategoryScreen(
    nav: NavController,
    id: String,
) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val (from, to) = reportRange()
    val list = inRange(txns, from, to).filter { it.kind == ReportFilter.kind && it.categoryId == id }
    val total = list.sumOf { it.amountPaise }
    val c = look.category(id)
    ScreenScaffold(c?.name ?: str(R.string.uncategorised), { nav.popBackStack() }) {
        HeroCard(brush = colorSpec(c?.colorKey ?: "slate").brush) {
            Text(periodLabel(), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(formatPaise(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
            Text(str(R.string.transactions, list.size), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        val subs = list.groupBy { it.subcategoryId }.map { (s, l) -> s to l.sumOf { it.amountPaise } }.sortedByDescending { it.second }
        if (subs.size > 1 || subs.firstOrNull()?.first?.isNotBlank() == true) {
            SectionTitle(str(R.string.sub_categories))
            Card {
                subs.forEach {
                        (s, v) ->
                    LabeledBar(
                        if (s.isBlank()) str(R.string.no_sub_category) else look.categoryName(s),
                        v,
                        total,
                        colorSpec(c?.colorKey ?: "slate").solid,
                        formatPaise(v),
                    )
                }
            }
        }
        SectionTitle(str(R.string.transactions_2))
        if (list.isEmpty()) Hint(str(R.string.nothing_in_this_period)) else DayGroups(list, look, { nav.navigate(Routes.txnDetail(it.id)) })
    }
}

// ------------------------------------------------------------------ Accounts

@Composable
fun AccountsScreen(nav: NavController) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val live = accounts.filter { !it.archived }
    val total = live.sumOf { Repo.balanceOf(it.id, accounts, txns) }

    TabScreen(
        str(R.string.accounts),
        Routes.ACCOUNTS,
        nav,
        fab = { Fab(Icons.Filled.Add, str(R.string.add_account)) { nav.navigate(Routes.ACCOUNT_TYPE) } },
    ) {
        HeroCard {
            Text(str(R.string.net_worth), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(formatPaise(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
            Text(str(R.string.accounts_2, live.size), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
        if (live.isEmpty()) EmptyState(Icons.Filled.AccountBalance, str(R.string.no_accounts_yet), str(R.string.add_a_bank_wallet_cash_or))
        AccountTypes.forEach { type ->
            val group = live.filter { it.type == type.key }
            if (group.isNotEmpty()) {
                SectionTitle(type.label)
                Card(padding = 12.dp) {
                    group.forEachIndexed { i, a ->
                        val bal = Repo.balanceOf(a.id, accounts, txns)
                        SettingRow(
                            a.name,
                            leading = { IconCircle(a.icon, a.colorKey, 44.dp) },
                            onClick = { nav.navigate(Routes.accountDetail(a.id)) },
                            trailing = { Text(formatPaise(bal), fontWeight = FontWeight.ExtraBold, color = if (bal < 0) Danger else Ink) },
                            divider = i < group.lastIndex,
                        )
                    }
                }
            }
        }
        val archived = accounts.filter { it.archived }
        if (archived.isNotEmpty()) {
            SectionTitle(str(R.string.archived))
            Card(padding = 12.dp) {
                archived.forEachIndexed { i, a ->
                    SettingRow(a.name, leading = {
                        IconCircle(a.icon, "slate", 44.dp)
                    }, onClick = { nav.navigate(Routes.accountDetail(a.id)) }, divider = i < archived.lastIndex)
                }
            }
        }
        Spacer(Modifier.height(70.dp))
    }
}