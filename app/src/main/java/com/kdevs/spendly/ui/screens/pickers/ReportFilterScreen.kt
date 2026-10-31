package com.kdevs.spendly.ui.screens.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
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
import com.kdevs.spendly.ui.screens.money.PickRow
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate

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

    val today = LocalDate.now()
    val periodDates =
        when (period) {
            "last30" -> fullDate(today.minusDays(29).startMillis()) + " – " + fullDate(todayMillis())
            "year" -> fullDate(today.withDayOfYear(1).startMillis()) + " – " + fullDate(todayMillis())
            "custom" -> fullDate(minOf(from, to)) + " – " + fullDate(maxOf(from, to))
            else -> fullDate(monthStart()) + " – " + fullDate(todayMillis())
        }
    val live = accounts.filter { !it.archived }

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
        // Period: four equal buttons in a 2 x 2 grid, with the dates it covers underneath.
        Card {
            Text(str(R.string.period), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            val options =
                listOf(
                    "month" to str(R.string.this_month),
                    "last30" to str(R.string.last_30_days),
                    "year" to str(R.string.this_year),
                    "custom" to str(R.string.custom),
                )
            options.chunked(2).forEachIndexed { i, pair ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (k, l) -> Box(Modifier.weight(1f)) { PeriodButton(l, period == k) { period = k } } }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(periodDates, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (period == "custom") {
                Spacer(Modifier.height(6.dp))
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

        SegmentedControl(listOf(str(R.string.spending), str(R.string.income)), if (kind == "income") 1 else 0, {
            kind = if (it == 1) "income" else "expense"
        })

        // Accounts: a checklist. Nothing ticked means every account.
        Card {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    str(R.string.accounts),
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (accs.isEmpty()) str(R.string.select_all) else str(R.string.clear),
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                accs = if (accs.isEmpty()) live.map { it.id }.toSet() else emptySet()
                            }.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            live.forEachIndexed { i, a ->
                val on = a.id in accs
                Row(
                    Modifier.fillMaxWidth().clickable { accs = if (on) accs - a.id else accs + a.id }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconCircle(a.icon, a.colorKey, 34.dp)
                    Text(a.name, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                    CheckBubble(on)
                }
                if (i < live.lastIndex) HorizontalDivider(color = Line)
            }
            Spacer(Modifier.height(4.dp))
            Hint(str(R.string.leave_empty_to_include_all_accounts))
        }
    }
}

/** One period button: equal width, same height, selected = tinted with a coloured outline. */
@Composable
private fun PeriodButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(if (selected) PrimaryContainer else Surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Primary else FieldBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Primary else Muted,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

/** Round tick used in checklists. */
@Composable
private fun CheckBubble(on: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (on) Primary else Color.Transparent)
            .border(1.5.dp, if (on) Primary else FieldBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}
