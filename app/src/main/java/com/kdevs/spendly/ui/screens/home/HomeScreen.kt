package com.kdevs.spendly.ui.screens.home

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
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.LabeledBar
import com.kdevs.spendly.ui.screens.common.TxnRow
import com.kdevs.spendly.ui.screens.common.earned
import com.kdevs.spendly.ui.screens.common.inRange
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.common.spent
import com.kdevs.spendly.ui.screens.money.RecurringRow
import com.kdevs.spendly.ui.theme.*

@Composable
fun HomeScreen(nav: NavController) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val budgets by Repo.budgets.collectAsState()
    val profile by Repo.profile.collectAsState()
    val recurring by Repo.recurring.collectAsState()
    val look = rememberLookup()

    val live = accounts.filter { !it.archived }
    val total = live.sumOf { Repo.balanceOf(it.id, accounts, txns) }
    val month = inRange(txns, monthStart(), nextMonthStart())
    val spentNow = spent(month)
    val overall = budgets.firstOrNull { it.categoryId.isBlank() }

    TabScreen(
        greeting(profile.name.ifBlank { Repo.authName }),
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
                    Modifier
                        .clip(CircleShape)
                        .clickable {
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

        val upcoming = recurring.filter { it.active }.sortedBy { it.nextMillis }.take(3)
        if (upcoming.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(str(R.string.upcoming_recurring), Modifier.weight(1f))
                Text(
                    str(R.string.see_all),
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.navigate(Routes.RECURRING) }.padding(6.dp),
                )
            }
            Card(padding = 12.dp) {
                upcoming.forEachIndexed { i, r ->
                    RecurringRow(
                        r,
                        look,
                        Modifier.fillMaxWidth().clickable { nav.navigate(Routes.recurringEdit(r.id)) }.padding(vertical = 8.dp),
                    )
                    if (i < upcoming.lastIndex) HorizontalDivider(color = Line)
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
    val hour =
        java.time.LocalTime
            .now()
            .hour
    val part =
        when {
            hour < 5 -> str(R.string.hello)
            hour < 12 -> str(R.string.good_morning)
            hour < 17 -> str(R.string.good_afternoon)
            hour < 22 -> str(R.string.good_evening)
            else -> str(R.string.hello)
        }
    return if (first.isBlank()) part else "$part, $first"
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
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        Text(label, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
    }
}
