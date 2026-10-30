package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.Card
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate

/** Looks up names and icons from the live lists. */
class Lookup(val accounts: List<Account>, val categories: List<Category>) {
    fun account(id: String) = accounts.firstOrNull { it.id == id }

    fun category(id: String) = categories.firstOrNull { it.id == id }

    fun accountName(id: String) = account(id)?.name ?: str(R.string.account)

    fun categoryName(id: String) = category(id)?.name ?: str(R.string.uncategorised)

    fun subs(parentId: String) = categories.filter { it.parentId == parentId }

    fun parents(kind: String) = categories.filter { it.parentId.isBlank() && it.kind == kind }
}

@Composable
fun rememberLookup(): Lookup {
    val a by Repo.accounts.collectAsState()
    val c by Repo.categories.collectAsState()
    return remember(a, c) { Lookup(a, c) }
}

/**
 * One transaction. Layout: icon, then the category and amount on the first line, a small line with the
 * parent category, date and time, the note in its own block, and the account at the bottom.
 * Pass `selected` (true/false) to show a tick box for select mode.
 */
@Composable
fun TxnRow(
    t: Txn,
    look: Lookup,
    onClick: () -> Unit,
    selected: Boolean? = null,
    showDate: Boolean = false,
) {
    val cat = look.category(t.subcategoryId.ifBlank { t.categoryId })
    val parent = look.category(t.categoryId)
    val isTransfer = t.kind == "transfer"
    val (title, icon, color) =
        if (isTransfer) {
            Triple(str(R.string.transfer), "invest", "slate")
        } else {
            Triple(
                cat?.name ?: str(R.string.uncategorised),
                cat?.icon ?: parent?.icon ?: "other",
                parent?.colorKey ?: cat?.colorKey ?: "slate",
            )
        }
    val subtitle =
        listOfNotNull(
            when {
                isTransfer -> null
                t.subcategoryId.isNotBlank() && parent != null -> parent.name
                t.kind == "income" -> str(R.string.income)
                else -> str(R.string.expense)
            },
            if (showDate) fullDate(t.dateMillis) else null,
            if (hasTime(t.dateMillis)) timeOf(t.dateMillis) else null,
        ).joinToString(" · ")
    val accountLabel =
        if (isTransfer) {
            str(R.string.to, look.accountName(t.accountId), look.accountName(t.toAccountId))
        } else {
            look.accountName(t.accountId)
        }
    val accountIcon = if (isTransfer) Icons.Filled.SwapHoriz else iconFor(look.account(t.accountId)?.icon ?: "bank")
    val amt =
        when (t.kind) {
            "income" -> formatPaise(t.amountPaise, withSign = true) to Success
            "expense" -> "-" + formatPaise(t.amountPaise) to Ink
            else -> formatPaise(t.amountPaise) to Muted
        }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selected != null) Box(Modifier.padding(top = 9.dp)) { TickBox(selected) }
        IconCircle(icon, color, 42.dp)
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(amt.first, color = amt.second, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
            }
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (t.note.isNotBlank()) {
                Text(
                    t.note,
                    modifier =
                        Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Bg)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(accountIcon, null, tint = Muted, modifier = Modifier.size(14.dp))
                Text(
                    accountLabel,
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
fun DayGroups(
    list: List<Txn>,
    look: Lookup,
    onClick: (Txn) -> Unit,
    selection: Set<String>? = null,
) {
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

fun inRange(
    list: List<Txn>,
    from: Long,
    to: Long,
) = list.filter { it.dateMillis in from until to }

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

fun periodLabel(): String =
    when (ReportFilter.period) {
        "last30" -> str(R.string.last_30_days)
        "year" -> str(R.string.this_year)
        "custom" -> str(R.string.to, fullDate(ReportFilter.customFrom), fullDate(ReportFilter.customTo))
        else -> str(R.string.this_month)
    }

/** Money text that never wraps awkwardly. */
@Composable
fun Money(
    paise: Long,
    color: Color = Ink,
    size: Int = 15,
    withSign: Boolean = false,
) {
    Text(formatPaise(paise, withSign), color = color, fontWeight = FontWeight.ExtraBold, fontSize = size.sp)
}

@Composable
fun LabeledBar(
    label: String,
    value: Long,
    max: Long,
    color: Color,
    right: String,
) {
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
