package com.kdevs.spendly.ui.screens.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.money.PickRowLeading
import com.kdevs.spendly.ui.screens.money.SavedBar
import com.kdevs.spendly.ui.screens.money.iconLabel
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Category picker and editors

@Composable
fun CategoryPickerScreen(
    nav: NavController,
    kind: String,
    slot: String,
) {
    val look = rememberLookup()
    // Transactions and recurring items can carry a sub-category; budgets, filters and bulk edits pick the main category only.
    val withSubs = slot == "category" || slot == "recurring"
    val manage = slot == "manage"
    // In Settings one screen holds both lists; everywhere else the kind is fixed by the caller.
    var shownKind by rememberSaveable { mutableStateOf(kind) }
    val activeKind = if (manage) shownKind else kind
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(setOf<String>()) }
    val parents =
        look.parents(activeKind).filter { p ->
            // The starter "Other" bucket is hidden when choosing; it stays in Settings so it can be managed or deleted.
            (manage || activeKind != "expense" || p.name != str(R.string.other)) &&
                (query.isBlank() || p.name.contains(query, true) || look.subs(p.id).any { it.name.contains(query, true) })
        }
    ScreenScaffold(
        if (slot == "manage") str(R.string.categories) else str(R.string.select_category),
        { nav.popBackStack() },
        // Adding and editing categories happens only in Settings. While choosing for a transaction, it is pick-only.
        actions = {
            if (manage) RoundIconButton(Icons.Filled.Add, str(R.string.new_category), { nav.navigate(Routes.categoryEdit("", activeKind)) })
        },
    ) {
        if (manage) {
            SegmentedControl(listOf(str(R.string.expense), str(R.string.income)), if (activeKind == "income") 1 else 0, {
                shownKind = if (it == 1) "income" else "expense"
                query = ""
                open = emptySet()
            })
        }
        AppTextField(query, { query = it }, str(R.string.search), leading = { Icon(Icons.Filled.Search, null) })
        if (parents.isEmpty()) {
            EmptyState(
                Icons.Filled.Category,
                str(R.string.no_categories),
                if (slot == "manage") str(R.string.tap_the_plus_button_to_create) else "Add categories in Settings > Categories.",
            )
        }
        parents.forEach { p ->
            val subs = look.subs(p.id)
            Card(padding = 12.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (slot == "manage") {
                                nav.navigate(Routes.categoryEdit(p.id, activeKind))
                            } else {
                                nav.setPicked("pickedCategory", "${p.id}|")
                                nav.popBackStack()
                            }
                        }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconCircle(p.icon, p.colorKey, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.ExtraBold, color = Ink)
                        if (withSubs && subs.isNotEmpty()) Text(str(R.string.sub_categories_2, subs.size), color = Muted, fontSize = 12.sp)
                    }
                    if (slot == "manage") {
                        RoundIconButton(
                            Icons.Filled.Edit,
                            str(R.string.edit_2, p.name),
                            { nav.navigate(Routes.categoryEdit(p.id, activeKind)) },
                            bg = Bg,
                        )
                    }
                    if (withSubs && subs.isNotEmpty()) {
                        Icon(
                            if (p.id in open || query.isNotBlank()) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            str(R.string.show_sub_categories),
                            tint = Muted,
                            modifier =
                                Modifier
                                    .clip(
                                        CircleShape,
                                    ).clickable { open = if (p.id in open) open - p.id else open + p.id }
                                    .padding(6.dp),
                        )
                    }
                }
                if (withSubs && (p.id in open || query.isNotBlank())) {
                    subs.filter { query.isBlank() || it.name.contains(query, true) || p.name.contains(query, true) }.forEach { s ->
                        HorizontalDivider(color = Line)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    nav.setPicked("pickedCategory", "${p.id}|${s.id}")
                                    nav.popBackStack()
                                }.padding(start = 24.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconCircle(s.icon, p.colorKey, 34.dp)
                            Text(s.name, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
    kind: String,
) {
    val look = rememberLookup()
    val existing = remember(id, look.categories.isEmpty()) { look.category(id) }
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable(existing) { mutableStateOf(existing?.icon ?: "other") }
    var color by rememberSaveable(existing) { mutableStateOf(existing?.colorKey ?: "blue") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    entry.Picked<String>("pickedColor") { color = it }
    val subs = look.subs(id)
    ScreenScaffold(
        if (existing == null) str(R.string.new_category) else str(R.string.edit_category),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, name.isNotBlank(), onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put("categories", id, Category(name = name.trim(), icon = icon, colorKey = color, kind = existing?.kind ?: kind))
                    // Sub-categories follow the parent colour.
                    if (existing != null && existing.colorKey != color) {
                        subs.forEach {
                            Repo.put(
                                "categories",
                                it.id,
                                it.copy(colorKey = color),
                            )
                        }
                    }
                }
            })
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, str(R.string.category_name))
        Card(padding = 12.dp) {
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
        SectionTitle(str(R.string.sub_categories))
        if (existing == null) {
            Hint(str(R.string.save_the_category_first_then_open))
        } else {
            Card(padding = 12.dp) {
                subs.forEach { s ->
                    SettingRow(s.name, leading = { IconCircle(s.icon, color, 38.dp) }, onClick = { nav.navigate(Routes.subEdit(s.id, id)) })
                }
                SettingRow(str(R.string.add_sub_category), icon = Icons.Filled.Add, iconColor = Primary, onClick = {
                    nav.navigate(Routes.subEdit("", id))
                }, divider = false)
            }
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_category), str(R.string.its_sub_categories_are_deleted_too), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.deleteCategory(id) }
        }, { confirm = false }, danger = true)
    }
}

@Composable
fun SubcategoryEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
    parentId: String,
) {
    val look = rememberLookup()
    val parent = look.category(parentId)
    val existing = remember(id, look.categories.isEmpty()) { look.category(id) }
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable(existing) { mutableStateOf(existing?.icon ?: parent?.icon ?: "other") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    val color = parent?.colorKey ?: "slate"
    ScreenScaffold(
        if (existing == null) str(R.string.new_sub_category) else str(R.string.edit_sub_category),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save), saver, name.isNotBlank() && parent != null, onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "categories",
                        id,
                        Category(name = name.trim(), icon = icon, colorKey = color, parentId = parentId, kind = parent!!.kind),
                    )
                }
            })
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, str(R.string.sub_category_name))
        Card(padding = 12.dp) {
            PickRowLeading(str(R.string.icon), iconLabel(icon), {
                IconCircle(icon, color, 38.dp)
            }, { nav.navigate(Routes.iconPicker("category", icon)) })
            SettingRow(
                str(R.string.colour),
                str(R.string.same_as_inherited, parent?.name.orEmpty()),
                leading = { Box(Modifier.size(38.dp).clip(CircleShape).background(colorSpec(color).brush)) },
                trailing = { Icon(Icons.Filled.Lock, null, tint = Faint) },
                divider = false,
            )
        }
        Hint(str(R.string.sub_categories_always_use_their_catego))
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_sub_category), str(R.string.past_transactions_keep_their_category), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("categories", id) }
        }, { confirm = false }, danger = true)
    }
}
