package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.ui.components.Card
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate

/** Looks up names and icons from the live lists. */
class Lookup(val accounts: List<Account>, val categories: List<Category>) {
    fun account(id: String) = accounts.firstOrNull { it.id == id }
    fun category(id: String) = categories.firstOrNull { it.id == id }
    fun accountName(id: String) = account(id)?.name ?: "Account"
    fun categoryName(id: String) = category(id)?.name ?: "Uncategorised"
    fun subs(parentId: String) = categories.filter { it.parentId == parentId }
    fun parents(kind: String) = categories.filter { it.parentId.isBlank() && it.kind == kind }
}

@Composable
fun rememberLookup(): Lookup {
    val a by Repo.accounts.collectAsState()
    val c by Repo.categories.collectAsState()
    return remember(a, c) { Lookup(a, c) }
}

/** One transaction line. Pass `selected` (true/false) to show a tick box for select mode. */
@Composable
fun TxnRow(t: Txn, look: Lookup, onClick: () -> Unit, selected: Boolean? = null, showDate: Boolean = false) {
    val cat = look.category(t.subcategoryId.ifBlank { t.categoryId })
    val parent = look.category(t.categoryId)
    val (title, icon, color) = when (t.kind) {
        "transfer" -> Triple("Transfer", "invest", "slate")
        else -> Triple(cat?.name ?: "Uncategorised", cat?.icon ?: parent?.icon ?: "other", parent?.colorKey ?: cat?.colorKey ?: "slate")
    }
    val sub = buildString {
        if (t.kind == "transfer") append("${look.accountName(t.accountId)} to ${look.accountName(t.toAccountId)}")
        else append(look.accountName(t.accountId))
        if (t.note.isNotBlank()) append(" · ${t.note}")
        if (showDate) append(" · ${fullDate(t.dateMillis)}")
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selected != null) TickBox(selected)
        IconCircle(icon, color, 42.dp)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp, maxLines = 1)
            Text(sub, color = Muted, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        }
        val amt = when (t.kind) {
            "income" -> formatPaise(t.amountPaise, withSign = true) to Success
            "expense" -> "-" + formatPaise(t.amountPaise) to Ink
            else -> formatPaise(t.amountPaise) to Muted
        }
        Text(amt.first, color = amt.second, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
fun TickBox(on: Boolean) {
    Box(
        Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(if (on) Primary else Color.Transparent)
            .let { if (on) it else it.background(Track) },
        contentAlignment = Alignment.Center,
    ) { if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
}

/** Transactions grouped under day headings inside one card. */
@Composable
fun DayGroups(list: List<Txn>, look: Lookup, onClick: (Txn) -> Unit, selection: Set<String>? = null) {
    val groups = list.groupBy { toLocalDate(it.dateMillis) }.toSortedMap(compareByDescending { it })
    groups.forEach { (day, items) ->
        val sum = items.filter { it.kind == "expense" }.sumOf { it.amountPaise }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dayLabel(day.startMillis()), color = Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (sum > 0) Text("-" + formatPaise(sum), color = Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Card(padding = 12.dp) {
            items.forEachIndexed { i, t ->
                TxnRow(t, look, { onClick(t) }, selected = selection?.let { t.id in it })
                if (i < items.lastIndex) HorizontalDivider(color = Line)
            }
        }
    }
}

fun spent(list: List<Txn>) = list.filter { it.kind == "expense" }.sumOf { it.amountPaise }
fun earned(list: List<Txn>) = list.filter { it.kind == "income" }.sumOf { it.amountPaise }
fun inRange(list: List<Txn>, from: Long, to: Long) = list.filter { it.dateMillis in from until to }

/** [from, to) in millis for the period chosen on the Report filter screen. */
fun reportRange(): Pair<Long, Long> {
    val today = LocalDate.now()
    return when (ReportFilter.period) {
        "last30" -> today.minusDays(29).startMillis() to today.plusDays(1).startMillis()
        "year" -> today.withDayOfYear(1).startMillis() to today.withDayOfYear(1).plusYears(1).startMillis()
        "custom" -> ReportFilter.customFrom to (ReportFilter.customTo + 86_400_000L)
        else -> monthStart() to nextMonthStart()
    }
}

fun periodLabel(): String = when (ReportFilter.period) {
    "last30" -> "Last 30 days"
    "year" -> "This year"
    "custom" -> "${fullDate(ReportFilter.customFrom)} to ${fullDate(ReportFilter.customTo)}"
    else -> "This month"
}

/** Money text that never wraps awkwardly. */
@Composable
fun Money(paise: Long, color: Color = Ink, size: Int = 15, withSign: Boolean = false) {
    Text(formatPaise(paise, withSign), color = color, fontWeight = FontWeight.ExtraBold, fontSize = size.sp)
}

@Composable
fun LabeledBar(label: String, value: Long, max: Long, color: Color, right: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
            Text(right, fontWeight = FontWeight.Bold, color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Track)) {
            val f = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}
