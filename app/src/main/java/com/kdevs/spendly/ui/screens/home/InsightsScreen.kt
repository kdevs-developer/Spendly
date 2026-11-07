package com.kdevs.spendly.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.theme.*

/** Plain-words notes about your spending: what changed against last month and any unusually big expenses. */
@Composable
fun InsightsScreen(nav: NavController) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val changes = remember(txns) { Insights.categoryChanges(txns) }
    val unusual = remember(txns) { Insights.recentUnusual(txns) }
    val up = changes.filter { it.delta > 0 && it.lastMonth > 0 && (it.percent ?: 0) >= 20 }.take(4)
    val down = changes.filter { it.delta < 0 && (it.percent ?: 0) <= -20 }.take(3)
    val fresh = changes.filter { it.lastMonth == 0L && it.thisMonth >= 50_000 }.take(3)
    val thisTotal = changes.sumOf { it.thisMonth }
    val lastTotal = changes.sumOf { it.lastMonth }

    ScreenScaffold(str(R.string.insights), { nav.popBackStack() }) {
        if (txns.none { it.kind == "expense" }) {
            EmptyState(Icons.Filled.Lightbulb, str(R.string.no_insights_yet), str(R.string.insights_empty_hint))
            return@ScreenScaffold
        }
        HeroCard {
            Text(str(R.string.this_month_so_far), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            Text(formatPaise(thisTotal), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
            Spacer(Modifier.height(4.dp))
            Text(str(R.string.same_days_last_month, formatPaise(lastTotal)), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        if (up.isEmpty() && down.isEmpty() && fresh.isEmpty()) {
            Hint(str(R.string.spending_looks_steady))
        } else {
            SectionTitle(str(R.string.changes_this_month))
            Card(padding = 12.dp) {
                val rows = up.map { Triple(it, true, "") } + down.map { Triple(it, false, "") } + fresh.map { Triple(it, true, "new") }
                rows.forEachIndexed { i, (ch, isUp, tag) ->
                    val c = look.category(ch.categoryId)
                    val name = if (ch.categoryId.isBlank()) str(R.string.uncategorised) else look.categoryName(ch.categoryId)
                    SettingRow(
                        if (tag == "new") {
                            str(R.string.insight_new, name)
                        } else if (isUp) {
                            str(R.string.insight_up, name, ch.percent ?: 0)
                        } else {
                            str(R.string.insight_down, name, -(ch.percent ?: 0))
                        },
                        formatPaise(ch.lastMonth) + " → " + formatPaise(ch.thisMonth),
                        leading = { IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 38.dp) },
                        icon = null,
                        trailing = {
                            Icon(
                                if (isUp) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                null,
                                tint = if (isUp) Danger else Success,
                            )
                        },
                        divider = i < rows.lastIndex,
                    )
                }
            }
        }
        if (unusual.isNotEmpty()) {
            SectionTitle(str(R.string.unusual_spends))
            Hint(str(R.string.unusual_hint))
            Card(padding = 12.dp) {
                unusual.take(6).forEachIndexed { i, (t, u) ->
                    SettingRow(
                        if (t.note.isNotBlank()) t.note else look.categoryName(t.categoryId),
                        str(R.string.unusual_line, formatPaise(u.usualPaise), look.categoryName(t.categoryId), fullDate(t.dateMillis)),
                        icon = Icons.Filled.Warning,
                        iconColor = Danger,
                        trailing = { Text(formatPaise(t.amountPaise), fontWeight = FontWeight.ExtraBold, color = Danger) },
                        onClick = { nav.navigate(Routes.txnDetail(t.id)) },
                        divider = i < minOf(unusual.size, 6) - 1,
                    )
                }
            }
        }
    }
}
