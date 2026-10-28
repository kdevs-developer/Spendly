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
import androidx.compose.material3.MaterialTheme
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
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.state.TxnFilter
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
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

    TabScreen("Hi, ${profile.name.ifBlank { "there" }}", Routes.HOME, nav,
        actions = { RoundIconButton(Icons.Filled.Settings, "Settings", { nav.navigate(Routes.SETTINGS) }) },
        fab = { Fab(Icons.Filled.Add, "Add transaction") { nav.navigate(Routes.txnAdd()) } },
    ) {
        HeroCard {
            Text("Total balance", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(formatPaise(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroStat("Spent this month", "-" + formatPaise(spentNow), Modifier.weight(1f))
                HeroStat("Earned this month", formatPaise(earned(month)), Modifier.weight(1f))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Expense", Icons.Filled.RemoveCircle, Danger, Modifier.weight(1f)) { nav.navigate(Routes.txnAdd("expense")) }
            QuickAction("Income", Icons.Filled.AddCircle, Success, Modifier.weight(1f)) { nav.navigate(Routes.txnAdd("income")) }
            QuickAction("Transfer", Icons.Filled.SwapHoriz, Primary, Modifier.weight(1f)) { nav.navigate(Routes.TRANSFER) }
        }

        if (overall != null) {
            Card(Modifier.clickable { nav.navigate(Routes.BUDGETS) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Monthly budget", fontWeight = FontWeight.Bold, color = Ink)
                    Text("${formatPaise(spentNow)} of ${formatPaise(overall.limitPaise)}", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                val over = spentNow > overall.limitPaise
                LabeledBar("", spentNow, overall.limitPaise, if (over) Danger else Primary,
                    if (over) "Over by ${formatPaise(spentNow - overall.limitPaise)}" else "${formatPaise(overall.limitPaise - spentNow)} left")
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Recent")
            Text("See all", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable { nav.navigate(Routes.ACTIVITY) }.padding(8.dp))
        }
        if (txns.isEmpty()) EmptyState(Icons.Filled.Receipt, "No transactions yet", "Tap the plus button to add your first one.")
        else Card(padding = 12.dp) {
            val recent = txns.take(5)
            recent.forEachIndexed { i, t ->
                TxnRow(t, look, { nav.navigate(Routes.txnDetail(t.id)) }, showDate = true)
                if (i < recent.lastIndex) HorizontalDivider(color = Line)
            }
        }

        SectionTitle("Plan ahead")
        Card(padding = 12.dp) {
            SettingRow("Budgets", icon = Icons.Filled.DonutLarge, iconColor = Color(0xFF7E22CE), onClick = { nav.navigate(Routes.BUDGETS) })
            SettingRow("Savings goals", icon = Icons.Filled.Flag, iconColor = Color(0xFF15803D), onClick = { nav.navigate(Routes.GOALS) })
            SettingRow("Templates", icon = Icons.Filled.ContentCopy, iconColor = Color(0xFF0F766E), onClick = { nav.navigate(Routes.TEMPLATES) })
            SettingRow("Recurring", icon = Icons.Filled.Repeat, iconColor = Color(0xFFC2410C), onClick = { nav.navigate(Routes.RECURRING) }, divider = false)
        }
        Spacer(Modifier.height(70.dp))
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.14f)).padding(12.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Surface).clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
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
    var view by remember { mutableIntStateOf(0) }       // 0 list, 1 calendar
    val filtered = remember(txns, look, TxnFilter.query, TxnFilter.kinds, TxnFilter.accountIds, TxnFilter.categoryIds,
        TxnFilter.fromMillis, TxnFilter.toMillis, TxnFilter.minPaise, TxnFilter.maxPaise) {
        txns.filter { TxnFilter.matches(it) { id -> look.categoryName(id) } }
    }

    TabScreen("Activity", Routes.ACTIVITY, nav,
        actions = {
            Box {
                RoundIconButton(Icons.Filled.FilterList, "Filter", { nav.navigate(Routes.FILTER) })
                if (TxnFilter.activeCount > 0) Box(Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(Danger), contentAlignment = Alignment.Center) {
                    Text("${TxnFilter.activeCount}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.width(8.dp))
            RoundIconButton(Icons.Filled.Checklist, "Select", { nav.navigate(Routes.SELECT) })
        },
        fab = { Fab(Icons.Filled.Add, "Add transaction") { nav.navigate(Routes.txnAdd()) } },
    ) {
        SegmentedControl(listOf("List", "Calendar"), view, { view = it })
        if (view == 0) {
            if (filtered.isEmpty()) EmptyState(Icons.Filled.SearchOff, "Nothing here", if (TxnFilter.activeCount > 0) "No transactions match your filter." else "Add a transaction to see it here.")
            else DayGroups(filtered, look, { nav.navigate(Routes.txnDetail(it.id)) })
        } else CalendarView(filtered, look, nav)
        Spacer(Modifier.height(70.dp))
    }
}

@Composable
private fun CalendarView(list: List<Txn>, look: Lookup, nav: NavController) {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var picked by remember { mutableStateOf(LocalDate.now()) }
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month", Modifier.clip(CircleShape).clickable { ym = ym.minusMonths(1) }.padding(8.dp))
            Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 16.sp)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month", Modifier.clip(CircleShape).clickable { ym = ym.plusMonths(1) }.padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        val lead = ym.atDay(1).dayOfWeek.value - 1
        val days = ym.lengthOfMonth()
        val byDay = list.filter { it.kind == "expense" }.groupBy { toLocalDate(it.dateMillis) }.mapValues { e -> e.value.sumOf { it.amountPaise } }
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
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Text("$day", color = if (on) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (total > 0) Text(compact(total), color = if (on) Color.White.copy(alpha = 0.85f) else Danger, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    val dayList = list.filter { toLocalDate(it.dateMillis) == picked }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionTitle(dayLabel(picked.startMillis()))
        if (dayList.isNotEmpty()) Text("-" + formatPaise(spent(dayList)), color = Muted, fontWeight = FontWeight.Bold)
    }
    if (dayList.isEmpty()) Hint("Nothing on this day.")
    else Card(padding = 12.dp) {
        dayList.forEachIndexed { i, t ->
            TxnRow(t, look, { nav.navigate(Routes.txnDetail(t.id)) })
            if (i < dayList.lastIndex) HorizontalDivider(color = Line)
        }
    }
}

/** 12,400 paise-rupees shown as "12k" so it fits a calendar cell. */
private fun compact(paise: Long): String {
    val r = paise / 100
    return when { r >= 100000 -> "${r / 100000}L"; r >= 1000 -> "${r / 1000}k"; else -> "$r" }
}

// ------------------------------------------------------------------ Reports

@Composable
fun ReportsScreen(nav: NavController) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    var view by remember { mutableIntStateOf(0) }
    val (from, to) = reportRange()
    val scoped = remember(txns, from, to, ReportFilter.accountIds, ReportFilter.kind) {
        inRange(txns, from, to).filter { t ->
            t.kind == ReportFilter.kind && (ReportFilter.accountIds.isEmpty() || t.accountId in ReportFilter.accountIds)
        }
    }

    TabScreen("Reports", Routes.REPORTS, nav,
        actions = { RoundIconButton(Icons.Filled.Tune, "Report filter", { nav.navigate(Routes.REPORT_FILTER) }) },
    ) {
        SegmentedControl(listOf("Category", "Trends"), view, { view = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(periodLabel(), color = Muted, fontWeight = FontWeight.Bold)
            Text(if (ReportFilter.kind == "expense") "Spending" else "Income", color = Primary, fontWeight = FontWeight.Bold)
        }
        if (view == 0) CategoryReport(scoped, look, nav) else TrendReport(txns)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryReport(list: List<Txn>, look: Lookup, nav: NavController) {
    val total = list.sumOf { it.amountPaise }
    val byCat = list.groupBy { it.categoryId }.map { (id, l) -> id to l.sumOf { it.amountPaise } }.sortedByDescending { it.second }
    if (total == 0L) { EmptyState(Icons.Filled.PieChart, "No data", "Nothing recorded for this period and filter."); return }
    Card {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(180.dp)) {
                var start = -90f
                val stroke = 34.dp.toPx()
                byCat.forEach { (id, v) ->
                    val sweep = 360f * v / total
                    drawArc(colorSpec(look.category(id)?.colorKey ?: "slate").solid, start, (sweep - 1.5f).coerceAtLeast(0.5f), false,
                        topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Butt))
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(formatPaise(total), color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        }
    }
    Card(padding = 12.dp) {
        byCat.forEachIndexed { i, (id, v) ->
            val c = look.category(id)
            Row(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.reportCategory(id)) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(c?.name ?: "Uncategorised", fontWeight = FontWeight.Bold, color = Ink)
                    Text("${(v * 100 / total)}% of total", color = Muted, fontSize = 12.sp)
                }
                Text(formatPaise(v), fontWeight = FontWeight.ExtraBold, color = Ink)
                Icon(Icons.Filled.ChevronRight, null, tint = Faint)
            }
            if (i < byCat.lastIndex) HorizontalDivider(color = Line)
        }
    }
}

@Composable
private fun TrendReport(txns: List<Txn>) {
    val today = LocalDate.now()
    val months = (5 downTo 0).map { today.withDayOfMonth(1).minusMonths(it.toLong()) }
    val data = months.map { m ->
        val l = inRange(txns, m.startMillis(), m.plusMonths(1).startMillis())
        Triple(m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), spent(l), earned(l))
    }
    val max = (data.maxOfOrNull { maxOf(it.second, it.third) } ?: 0L).coerceAtLeast(1L)
    Card {
        Text("Last 6 months", fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            data.forEach { (_, s, e) ->
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.width(14.dp).fillMaxHeight((s.toFloat() / max).coerceIn(0.02f, 1f)).clip(RoundedCornerShape(5.dp)).background(Danger))
                    Box(Modifier.width(14.dp).fillMaxHeight((e.toFloat() / max).coerceIn(0.02f, 1f)).clip(RoundedCornerShape(5.dp)).background(Success))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            data.forEach { Text(it.first, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(Danger, "Spent"); Legend(Success, "Earned")
        }
    }
    Card(padding = 12.dp) {
        data.reversed().forEachIndexed { i, (m, s, e) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(m, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.width(50.dp))
                Text("-" + formatPaise(s), color = Danger, fontWeight = FontWeight.Bold)
                Text(formatPaise(e, true).let { if (e == 0L) "₹0" else it }, color = Success, fontWeight = FontWeight.Bold)
            }
            if (i < data.lastIndex) HorizontalDivider(color = Line)
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Drill-down: one category's transactions in the report period, split by sub-category. */
@Composable
fun ReportCategoryScreen(nav: NavController, id: String) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val (from, to) = reportRange()
    val list = inRange(txns, from, to).filter { it.kind == ReportFilter.kind && it.categoryId == id }
    val total = list.sumOf { it.amountPaise }
    val c = look.category(id)
    ScreenScaffold(c?.name ?: "Uncategorised", { nav.popBackStack() }) {
        HeroCard(brush = colorSpec(c?.colorKey ?: "slate").brush) {
            Text(periodLabel(), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(formatPaise(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
            Text("${list.size} transactions", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        val subs = list.groupBy { it.subcategoryId }.map { (s, l) -> s to l.sumOf { it.amountPaise } }.sortedByDescending { it.second }
        if (subs.size > 1 || subs.firstOrNull()?.first?.isNotBlank() == true) {
            SectionTitle("Sub-categories")
            Card { subs.forEach { (s, v) -> LabeledBar(if (s.isBlank()) "No sub-category" else look.categoryName(s), v, total, colorSpec(c?.colorKey ?: "slate").solid, formatPaise(v)) } }
        }
        SectionTitle("Transactions")
        if (list.isEmpty()) Hint("Nothing in this period.") else DayGroups(list, look, { nav.navigate(Routes.txnDetail(it.id)) })
    }
}

// ------------------------------------------------------------------ Accounts

@Composable
fun AccountsScreen(nav: NavController) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val live = accounts.filter { !it.archived }
    val total = live.sumOf { Repo.balanceOf(it.id, accounts, txns) }

    TabScreen("Accounts", Routes.ACCOUNTS, nav,
        fab = { Fab(Icons.Filled.Add, "Add account") { nav.navigate(Routes.ACCOUNT_TYPE) } },
    ) {
        HeroCard {
            Text("Net worth", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(formatPaise(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
            Text("${live.size} accounts", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
        if (live.isEmpty()) EmptyState(Icons.Filled.AccountBalance, "No accounts yet", "Add a bank, wallet, cash or card to start tracking.")
        AccountTypes.forEach { type ->
            val group = live.filter { it.type == type.key }
            if (group.isNotEmpty()) {
                SectionTitle(type.label)
                Card(padding = 12.dp) {
                    group.forEachIndexed { i, a ->
                        val bal = Repo.balanceOf(a.id, accounts, txns)
                        SettingRow(a.name, leading = { IconCircle(a.icon, a.colorKey, 44.dp) },
                            onClick = { nav.navigate(Routes.accountDetail(a.id)) },
                            trailing = { Text(formatPaise(bal), fontWeight = FontWeight.ExtraBold, color = if (bal < 0) Danger else Ink) },
                            divider = i < group.lastIndex)
                    }
                }
            }
        }
        val archived = accounts.filter { it.archived }
        if (archived.isNotEmpty()) {
            SectionTitle("Archived")
            Card(padding = 12.dp) {
                archived.forEachIndexed { i, a ->
                    SettingRow(a.name, leading = { IconCircle(a.icon, "slate", 44.dp) }, onClick = { nav.navigate(Routes.accountDetail(a.id)) }, divider = i < archived.lastIndex)
                }
            }
        }
        Spacer(Modifier.height(70.dp))
    }
}
