package com.kdevs.spendly.ui.screens.home

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
import androidx.compose.ui.graphics.Color
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
import com.kdevs.spendly.ui.screens.common.Lookup
import com.kdevs.spendly.ui.screens.common.TxnRow
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.common.spent
import com.kdevs.spendly.ui.state.TxnFilter
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ------------------------------------------------------------------ Activity

private const val PAGE = 100

@Composable
fun ActivityScreen(nav: NavController) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    var view by remember { mutableIntStateOf(0) } // 0 list, 1 calendar
    // The list shows 100 transactions at a time so a long history opens fast; "Show more" adds the next 100.
    var limit by remember(TxnFilter.query, TxnFilter.activeCount) { mutableIntStateOf(PAGE) }
    val filtered =
        remember(
            txns,
            look,
            TxnFilter.query,
            TxnFilter.kinds,
            TxnFilter.accountIds,
            TxnFilter.categoryIds,
            TxnFilter.fromMillis,
            TxnFilter.toMillis,
            TxnFilter.minPaise,
            TxnFilter.maxPaise,
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
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Danger),
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
                val page = remember(filtered, limit) { filtered.take(limit) }
                DayGroups(page, look, { nav.navigate(Routes.txnDetail(it.id)) })
                if (filtered.size > limit) {
                    SecondaryButton(str(R.string.show_more_count, minOf(PAGE, filtered.size - limit)), { limit += PAGE })
                }
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
                Modifier
                    .clip(CircleShape)
                    .clickable {
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
                Modifier
                    .clip(CircleShape)
                    .clickable {
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
            list.filter { it.kind == "expense" }.groupBy { toLocalDate(it.dateMillis) }.mapValues { e ->
                e.value.sumOf { it.amountPaise }
            }
        val rows = (lead + days + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val date = ym.atDay(day)
                            val on = date == picked
                            val total = byDay[date] ?: 0L
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(1.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (on) Primary else Color.Transparent)
                                    .clickable { picked = date },
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
