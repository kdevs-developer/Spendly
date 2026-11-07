package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.theme.*

/** Everything you pay on repeat (active expense items from Recurring), with what it costs a month and a year. */
@Composable
fun SubscriptionsScreen(nav: NavController) {
    val items by Repo.recurring.collectAsState()
    val look = rememberLookup()
    val active = remember(items) { items.filter { it.active && it.kind == "expense" }.sortedByDescending { Insights.monthlyCost(it) } }
    val paused = remember(items) { items.filter { !it.active && it.kind == "expense" } }
    val month = active.sumOf { Insights.monthlyCost(it) }
    val year = active.sumOf { Insights.yearlyCost(it) }

    ScreenScaffold(str(R.string.subscriptions), { nav.popBackStack() }) {
        if (active.isEmpty() && paused.isEmpty()) {
            EmptyState(Icons.Filled.Autorenew, str(R.string.no_subscriptions_yet), str(R.string.subscriptions_empty_hint))
            SecondaryButton(str(R.string.add_recurring_item), { nav.navigate(Routes.recurringEdit("")) })
            return@ScreenScaffold
        }
        HeroCard {
            Text(str(R.string.you_pay_every_month), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            Text(formatPaise(month), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
            Spacer(Modifier.height(4.dp))
            Text(str(R.string.about_x_a_year, formatPaise(year)), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        SectionTitle(str(R.string.active_count, active.size))
        Card(padding = 12.dp) {
            active.forEachIndexed { i, r ->
                val c = look.category(r.subcategoryId.ifBlank { r.categoryId })
                SettingRow(
                    r.name,
                    str(R.string.per_year_line, formatPaise(Insights.yearlyCost(r))),
                    leading = { IconCircle(c?.icon ?: "other", look.category(r.categoryId)?.colorKey ?: "slate", 42.dp) },
                    trailing = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(formatPaise(Insights.monthlyCost(r)), fontWeight = FontWeight.ExtraBold, color = Ink)
                            Text(str(R.string.per_month), color = Muted, fontSize = 11.sp)
                        }
                    },
                    onClick = { nav.navigate(Routes.recurringEdit(r.id)) },
                    divider = i < active.lastIndex,
                )
            }
        }
        if (paused.isNotEmpty()) {
            SectionTitle(str(R.string.paused_count, paused.size))
            Card(padding = 12.dp) {
                paused.forEachIndexed { i, r ->
                    SettingRow(
                        r.name,
                        formatPaise(r.amountPaise),
                        icon = Icons.Filled.PauseCircle,
                        iconColor = Muted,
                        onClick = { nav.navigate(Routes.recurringEdit(r.id)) },
                        divider = i < paused.lastIndex,
                    )
                }
            }
        }
        Hint(str(R.string.subscriptions_note))
    }
}
