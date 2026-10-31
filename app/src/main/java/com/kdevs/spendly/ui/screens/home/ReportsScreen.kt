package com.kdevs.spendly.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.DayGroups
import com.kdevs.spendly.ui.screens.common.LabeledBar
import com.kdevs.spendly.ui.screens.common.Lookup
import com.kdevs.spendly.ui.screens.common.earned
import com.kdevs.spendly.ui.screens.common.inRange
import com.kdevs.spendly.ui.screens.common.periodLabel
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.common.reportRange
import com.kdevs.spendly.ui.screens.common.spent
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

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
private class TrendBucket(
    val short: String,
    val long: String,
    val from: Long,
    val to: Long,
)

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
    val barWidth =
        when (gran) {
            0 -> 7.dp
            1 -> 12.dp
            3 -> 18.dp
            else -> 14.dp
        }
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
                            Modifier
                                .width(barWidth)
                                .fillMaxHeight((s.toFloat() / max).coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(Danger),
                        )
                        Box(
                            Modifier
                                .width(barWidth)
                                .fillMaxHeight((e.toFloat() / max).coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(Success),
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
                subs.forEach { (s, v) ->
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
