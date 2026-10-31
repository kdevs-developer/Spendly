package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Goals

@Composable
fun GoalsScreen(nav: NavController) {
    val goals by Repo.goals.collectAsState()
    ScreenScaffold(
        str(R.string.savings_goals),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.new_goal), { nav.navigate(Routes.goalEdit("")) }) },
    ) {
        if (goals.isEmpty()) EmptyState(Icons.Filled.Flag, str(R.string.no_goals_yet), str(R.string.save_towards_a_trip_a_phone))
        goals.forEach { g ->
            val pct = if (g.targetPaise > 0) (g.savedPaise * 100 / g.targetPaise).coerceAtMost(100) else 0
            Card(onClick = { nav.navigate(Routes.goalEdit(g.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconCircle(g.icon, g.colorKey, 46.dp)
                    Column(Modifier.weight(1f)) {
                        Text(g.name, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text(
                            if (g.deadlineMillis > 0) str(R.string.by, fullDate(g.deadlineMillis)) else str(R.string.no_deadline),
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                    Text(str(R.string.text__5, pct), fontWeight = FontWeight.ExtraBold, color = colorSpec(g.colorKey).solid)
                }
                LabeledBar(
                    "",
                    g.savedPaise,
                    g.targetPaise,
                    colorSpec(g.colorKey).solid,
                    str(R.string.of_2, formatPaise(g.savedPaise), formatPaise(g.targetPaise)),
                )
                if (g.savedPaise < g.targetPaise) {
                    SecondaryButton(str(R.string.add_savings), { nav.navigate(Routes.goalAdd(g.id)) })
                } else {
                    Text(str(R.string.goal_reached), color = Success, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun GoalEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val goals by Repo.goals.collectAsState()
    val existing = remember(id, goals.isEmpty()) { goals.firstOrNull { it.id == id } }
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var target by rememberSaveable(existing) { mutableStateOf(existing?.let { paiseToField(it.targetPaise) } ?: "") }
    var icon by rememberSaveable(existing) { mutableStateOf(existing?.icon ?: "goal") }
    var color by rememberSaveable(existing) { mutableStateOf(existing?.colorKey ?: "green") }
    var deadline by rememberSaveable(existing) { mutableStateOf(existing?.deadlineMillis ?: 0L) }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    entry.Picked<String>("pickedColor") { color = it }
    entry.Picked<Long>("pickedDate") { deadline = it }
    val paise = parseRupees(target) ?: 0L
    ScreenScaffold(
        if (existing == null) str(R.string.new_goal) else str(R.string.edit_goal),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save_goal), saver, name.isNotBlank() && paise > 0, onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "goals",
                        id,
                        Goal(
                            name = name.trim(),
                            icon = icon,
                            colorKey = color,
                            targetPaise = paise,
                            savedPaise = existing?.savedPaise ?: 0,
                            deadlineMillis = deadline,
                        ),
                    )
                }
            })
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, str(R.string.goal_name))
        AppTextField(
            target,
            { v -> if (v.all { it.isDigit() || it == '.' }) target = v },
            str(R.string.target_amount),
            keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal,
            leading = { Text(str(R.string.rupee_symbol), fontWeight = FontWeight.Bold) },
        )
        Card(padding = 12.dp) {
            PickRow(
                str(
                    R.string.deadline,
                ),
                if (deadline > 0) fullDate(deadline) else str(R.string.none),
                Icons.Filled.CalendarMonth,
                Muted,
                {
                    nav.navigate(Routes.datePicker(if (deadline > 0) deadline else todayMillis()))
                },
            )
            PickRowLeading(str(R.string.icon), iconLabel(icon), {
                IconCircle(icon, color, 38.dp)
            }, { nav.navigate(Routes.iconPicker("category", icon)) })
            PickRowLeading(
                str(R.string.colour),
                colorSpec(color).label,
                { Box(Modifier.size(38.dp).clip(CircleShape).background(colorSpec(color).brush)) },
                { nav.navigate(Routes.colorPicker(color)) },
                divider = false,
            )
        }
        if (deadline > 0) {
            Text(
                str(
                    R.string.remove_deadline,
                ),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.clip(CircleShape).clickable {
                        deadline = 0
                    }.padding(8.dp),
            )
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_goal), str(R.string.the_saved_amount_is_only_a), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("goals", id) }
        }, { confirm = false }, danger = true)
    }
}

@Composable
fun GoalAddScreen(
    nav: NavController,
    id: String,
) {
    val goals by Repo.goals.collectAsState()
    val g = goals.firstOrNull { it.id == id }
    var amount by rememberSaveable { mutableStateOf("") }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val paise = parseRupees(amount) ?: 0L
    ScreenScaffold(str(R.string.add_savings), { nav.popBackStack() }, bottomBar = {
        SavedBar(str(R.string.add_to_goal), saver, paise > 0 && g != null, onSave = {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.put("goals", id, g!!.copy(savedPaise = g.savedPaise + paise)) }
        })
    }) {
        if (g == null) return@ScreenScaffold
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(g.icon, g.colorKey, 72.dp) }
        Text(
            g.name,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Hint(str(R.string.saved_of, formatPaise(g.savedPaise), formatPaise(g.targetPaise)), center = true)
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 10.dp), color = colorSpec(g.colorKey).solid)
        if (paise > 0) Hint(str(R.string.after_this, formatPaise(g.savedPaise + paise)), center = true)
    }
}
