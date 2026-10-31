package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.kdevs.spendly.ui.screens.common.LabeledBar
import com.kdevs.spendly.ui.screens.common.inRange
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Budgets

@Composable
fun BudgetsScreen(nav: NavController) {
    val budgets by Repo.budgets.collectAsState()
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val month = inRange(txns, monthStart(), nextMonthStart()).filter { it.kind == "expense" }
    ScreenScaffold(
        str(R.string.budgets),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.add_budget), { nav.navigate(Routes.budgetEdit("")) }) },
    ) {
        Hint(str(R.string.budgets_are_monthly_they_reset_on))
        if (budgets.isEmpty()) EmptyState(Icons.Filled.DonutLarge, str(R.string.no_budgets_yet), str(R.string.set_an_overall_limit_or_one))
        budgets.sortedBy { it.categoryId.isNotBlank() }.forEach { b ->
            val used =
                if (b.categoryId.isBlank()) {
                    month.sumOf {
                        it.amountPaise
                    }
                } else {
                    month.filter { it.categoryId == b.categoryId }.sumOf { it.amountPaise }
                }
            val c = look.category(b.categoryId)
            val over = used > b.limitPaise
            Card(onClick = { nav.navigate(Routes.budgetEdit(b.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconCircle(c?.icon ?: "bills", c?.colorKey ?: "blue", 42.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (b.categoryId.isBlank()) str(R.string.overall) else c?.name ?: str(R.string.category),
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink,
                        )
                        Text(str(R.string.of_2, formatPaise(used), formatPaise(b.limitPaise)), color = Muted, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Faint)
                }
                LabeledBar(
                    "",
                    used,
                    b.limitPaise,
                    if (over) Danger else colorSpec(c?.colorKey ?: "blue").solid,
                    if (over) str(R.string.over_by, formatPaise(used - b.limitPaise)) else "${formatPaise(b.limitPaise - used)} left",
                )
            }
        }
    }
}

@Composable
fun BudgetEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val budgets by Repo.budgets.collectAsState()
    val look = rememberLookup()
    val existing = remember(id, budgets.isEmpty()) { budgets.firstOrNull { it.id == id } }
    var catId by rememberSaveable(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var limit by rememberSaveable(existing) { mutableStateOf(existing?.let { paiseToField(it.limitPaise) } ?: "") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedCategory") { catId = splitPick(it).first }
    val c = look.category(catId)
    val paise = parseRupees(limit) ?: 0L
    ScreenScaffold(
        if (existing == null) str(R.string.new_budget) else str(R.string.edit_budget),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, paise > 0, onSave = {
                if (budgets.any { it.categoryId == catId && it.id != id }) {
                    saver.error = str(R.string.budget_already_exists)
                    return@SavedBar
                }
                saver.run(
                    scope,
                    onDone = { nav.popBackStack() },
                ) { Repo.put("budgets", id, Budget(categoryId = catId, limitPaise = paise)) }
            })
        },
    ) {
        Text(
            str(R.string.monthly_limit),
            color = Muted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        AmountField(limit, { limit = it }, Modifier.padding(vertical = 6.dp), color = Primary)
        Card(padding = 12.dp) {
            PickRowLeading(
                str(R.string.applies_to),
                if (catId.isBlank()) str(R.string.overall_all_spending) else c?.name.orEmpty(),
                { IconCircle(c?.icon ?: "bills", c?.colorKey ?: "blue", 38.dp) },
                { nav.navigate(Routes.categoryPicker("expense", "budget")) },
                divider = false,
            )
        }
        if (catId.isNotBlank()) {
            Text(
                str(R.string.use_overall_instead),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable { catId = "" }.padding(8.dp),
            )
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_budget), str(R.string.your_transactions_are_not_affected), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("budgets", id) }
        }, { confirm = false }, danger = true)
    }
}
