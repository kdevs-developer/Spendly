package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.state.Selection
import com.kdevs.spendly.ui.state.TxnFilter
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ------------------------------------------------------------------ Filter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterScreen(nav: NavController, entry: NavBackStackEntry) {
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    var query by remember { mutableStateOf(TxnFilter.query) }
    var kinds by remember { mutableStateOf(TxnFilter.kinds) }
    var accs by remember { mutableStateOf(TxnFilter.accountIds) }
    var cats by remember { mutableStateOf(TxnFilter.categoryIds) }
    var from by remember { mutableLongStateOf(TxnFilter.fromMillis) }
    var to by remember { mutableLongStateOf(TxnFilter.toMillis) }
    var min by remember { mutableStateOf(if (TxnFilter.minPaise == 0L) "" else paiseToField(TxnFilter.minPaise)) }
    var max by remember { mutableStateOf(if (TxnFilter.maxPaise == 0L) "" else paiseToField(TxnFilter.maxPaise)) }
    var target by rememberSaveable { mutableStateOf("from") }
    entry.Picked<Long>("pickedDate") { if (target == "from") from = it else to = it + 86_400_000L }

    fun <T> Set<T>.flip(v: T) = if (v in this) this - v else this + v

    ScreenScaffold("Filter", { nav.popBackStack() },
        actions = { Text("Reset", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable {
            query = ""; kinds = emptySet(); accs = emptySet(); cats = emptySet(); from = 0; to = 0; min = ""; max = ""
        }.padding(10.dp)) },
        bottomBar = {
            BottomActionBar {
                PrimaryButton("Apply filter", {
                    TxnFilter.query = query; TxnFilter.kinds = kinds; TxnFilter.accountIds = accs; TxnFilter.categoryIds = cats
                    TxnFilter.fromMillis = from; TxnFilter.toMillis = to
                    TxnFilter.minPaise = parseRupees(min) ?: 0L; TxnFilter.maxPaise = parseRupees(max) ?: 0L
                    nav.popBackStack()
                })
            }
        }) {
        AppTextField(query, { query = it }, "Search notes and categories", leading = { Icon(Icons.Filled.Search, null) })
        SectionTitle("Type")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("expense" to "Expense", "income" to "Income", "transfer" to "Transfer").forEach { (k, l) -> SelectChip(l, k in kinds) { kinds = kinds.flip(k) } }
        }
        SectionTitle("Accounts")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.filter { !it.archived }.forEach { a -> SelectChip(a.name, a.id in accs) { accs = accs.flip(a.id) } }
        }
        SectionTitle("Categories")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            look.categories.filter { it.parentId.isBlank() }.forEach { c -> SelectChip(c.name, c.id in cats) { cats = cats.flip(c.id) } }
        }
        SectionTitle("Dates")
        Card(padding = 12.dp) {
            PickRow("From", if (from > 0) fullDate(from) else "Any", Icons.Filled.CalendarMonth, Muted, { target = "from"; nav.navigate(Routes.datePicker(if (from > 0) from else todayMillis())) })
            PickRow("To", if (to > 0) fullDate(to - 86_400_000L) else "Any", Icons.Filled.CalendarMonth, Muted, { target = "to"; nav.navigate(Routes.datePicker(if (to > 0) to - 86_400_000L else todayMillis())) }, divider = false)
        }
        SectionTitle("Amount")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { AppTextField(min, { v -> if (v.all { it.isDigit() || it == '.' }) min = v }, "Min ₹", keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal) }
            Box(Modifier.weight(1f)) { AppTextField(max, { v -> if (v.all { it.isDigit() || it == '.' }) max = v }, "Max ₹", keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal) }
        }
    }
}

// ------------------------------------------------------------------ Select and bulk

@Composable
fun SelectScreen(nav: NavController) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val list = remember(txns, look) { txns.filter { TxnFilter.matches(it) { id -> look.categoryName(id) } } }
    LaunchedEffect(Unit) { Selection.clear() }
    val chosen = Selection.ids.toSet()
    ScreenScaffold("Select items", { Selection.clear(); nav.popBackStack() },
        actions = {
            Text(if (chosen.size == list.size && list.isNotEmpty()) "Clear" else "Select all", color = Primary, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable {
                    if (chosen.size == list.size) Selection.clear() else { Selection.clear(); Selection.ids.addAll(list.map { it.id }) }
                }.padding(10.dp))
        },
        bottomBar = {
            BottomActionBar {
                PrimaryButton(if (chosen.isEmpty()) "Select items to continue" else "Actions for ${chosen.size} selected", { nav.navigate(Routes.BULK) }, enabled = chosen.isNotEmpty())
            }
        }) {
        if (list.isEmpty()) EmptyState(Icons.Filled.SearchOff, "Nothing to select", "No transactions match the current filter.")
        else Card(padding = 12.dp) {
            list.forEachIndexed { i, t ->
                TxnRow(t, look, { Selection.toggle(t.id) }, selected = t.id in chosen, showDate = true)
                if (i < list.lastIndex) HorizontalDivider(color = Line)
            }
        }
    }
}

@Composable
fun BulkScreen(nav: NavController, entry: NavBackStackEntry) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val picked = txns.filter { it.id in Selection.ids }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()

    fun finish(msg: String) { Selection.clear(); nav.popBackStack(Routes.ACTIVITY, false) }
    suspend fun updateAll(f: (Txn) -> Txn) { picked.forEach { Repo.put("txns", it.id, f(it)) } }

    entry.Picked<String>("pickedCategory") { v ->
        val (c, s) = splitPick(v)
        saver.run(scope, onDone = { finish("") }) { updateAll { if (it.kind == "transfer") it else it.copy(categoryId = c, subcategoryId = s) } }
    }
    entry.Picked<String>("pickedAccount") { a ->
        saver.run(scope, onDone = { finish("") }) { updateAll { if (it.kind == "transfer") it else it.copy(accountId = a) } }
    }
    entry.Picked<Long>("pickedDate") { d ->
        saver.run(scope, onDone = { finish("") }) { updateAll { it.copy(dateMillis = d) } }
    }

    ScreenScaffold("Bulk actions", { nav.popBackStack() }) {
        HeroCard {
            Text("${picked.size} selected", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            Text("-" + formatPaise(spent(picked)), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
            if (earned(picked) > 0) Text("+" + formatPaise(earned(picked)) + " income", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
        }
        saver.error?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold) }
        Card(padding = 12.dp) {
            SettingRow("Change category", icon = Icons.Filled.Category, iconColor = Color(0xFF7E22CE), onClick = { nav.navigate(Routes.categoryPicker("expense", "bulk")) })
            SettingRow("Change account", icon = Icons.Filled.AccountBalanceWallet, iconColor = Primary, onClick = { nav.navigate(Routes.accountPicker("account")) })
            SettingRow("Change date", icon = Icons.Filled.CalendarMonth, iconColor = Color(0xFF0F766E), onClick = { nav.navigate(Routes.datePicker(todayMillis())) })
            SettingRow("Delete all", icon = Icons.Filled.Delete, iconColor = Danger, onClick = { confirm = true }, divider = false)
        }
        Hint("Transfers keep their accounts. Category and account changes skip them.")
        SectionTitle("Selected items")
        Card(padding = 12.dp) {
            picked.forEachIndexed { i, t -> TxnRow(t, look, {}, showDate = true); if (i < picked.lastIndex) HorizontalDivider(color = Line) }
        }
    }
    if (confirm) ConfirmDialog("Delete ${picked.size} transactions?", "This cannot be undone.", "Delete", {
        saver.run(scope, onDone = { finish("") }) { Repo.removeMany("txns", picked.map { it.id }) }
    }, { confirm = false }, danger = true)
}

// ------------------------------------------------------------------ Report filter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportFilterScreen(nav: NavController, entry: NavBackStackEntry) {
    val accounts by Repo.accounts.collectAsState()
    var period by remember { mutableStateOf(ReportFilter.period) }
    var from by remember { mutableLongStateOf(ReportFilter.customFrom.takeIf { it > 0 } ?: monthStart()) }
    var to by remember { mutableLongStateOf(ReportFilter.customTo.takeIf { it > 0 } ?: todayMillis()) }
    var kind by remember { mutableStateOf(ReportFilter.kind) }
    var accs by remember { mutableStateOf(ReportFilter.accountIds) }
    var target by rememberSaveable { mutableStateOf("from") }
    entry.Picked<Long>("pickedDate") { if (target == "from") from = it else to = it }

    ScreenScaffold("Report filter", { nav.popBackStack() }, bottomBar = {
        BottomActionBar {
            PrimaryButton("Apply", {
                ReportFilter.period = period; ReportFilter.customFrom = from; ReportFilter.customTo = maxOf(from, to)
                ReportFilter.kind = kind; ReportFilter.accountIds = accs
                nav.popBackStack()
            })
        }
    }) {
        SectionTitle("Period")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("month" to "This month", "last30" to "Last 30 days", "year" to "This year", "custom" to "Custom").forEach { (k, l) -> SelectChip(l, period == k) { period = k } }
        }
        if (period == "custom") Card(padding = 12.dp) {
            PickRow("From", fullDate(from), Icons.Filled.CalendarMonth, Muted, { target = "from"; nav.navigate(Routes.datePicker(from)) })
            PickRow("To", fullDate(to), Icons.Filled.CalendarMonth, Muted, { target = "to"; nav.navigate(Routes.datePicker(to)) }, divider = false)
        }
        SectionTitle("Show")
        SegmentedControl(listOf("Spending", "Income"), if (kind == "income") 1 else 0, { kind = if (it == 1) "income" else "expense" })
        SectionTitle("Accounts")
        Hint("Leave empty to include all accounts.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.filter { !it.archived }.forEach { a -> SelectChip(a.name, a.id in accs) { accs = if (a.id in accs) accs - a.id else accs + a.id } }
        }
    }
}

// ------------------------------------------------------------------ Category picker and editors

@Composable
fun CategoryPickerScreen(nav: NavController, kind: String, slot: String) {
    val look = rememberLookup()
    val withSubs = slot == "category"          // budgets, templates, filters and bulk edits pick the main category only
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(setOf<String>()) }
    val parents = look.parents(kind).filter { p -> query.isBlank() || p.name.contains(query, true) || look.subs(p.id).any { it.name.contains(query, true) } }
    ScreenScaffold(if (slot == "manage") "Categories" else "Select category", { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, "New category", { nav.navigate(Routes.categoryEdit("", kind)) }) }) {
        AppTextField(query, { query = it }, "Search", leading = { Icon(Icons.Filled.Search, null) })
        if (parents.isEmpty()) EmptyState(Icons.Filled.Category, "No categories", "Tap the plus button to create one.")
        parents.forEach { p ->
            val subs = look.subs(p.id)
            Card(padding = 12.dp) {
                Row(Modifier.fillMaxWidth().clickable {
                    if (slot == "manage") nav.navigate(Routes.categoryEdit(p.id, kind)) else { nav.setPicked("pickedCategory", "${p.id}|"); nav.popBackStack() }
                }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconCircle(p.icon, p.colorKey, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.ExtraBold, color = Ink)
                        if (withSubs && subs.isNotEmpty()) Text("${subs.size} sub-categories", color = Muted, fontSize = 12.sp)
                    }
                    RoundIconButton(Icons.Filled.Edit, "Edit ${p.name}", { nav.navigate(Routes.categoryEdit(p.id, kind)) }, bg = Bg)
                    if (withSubs && subs.isNotEmpty()) Icon(if (p.id in open || query.isNotBlank()) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, "Show sub-categories",
                        tint = Muted, modifier = Modifier.clip(CircleShape).clickable { open = if (p.id in open) open - p.id else open + p.id }.padding(6.dp))
                }
                if (withSubs && (p.id in open || query.isNotBlank())) subs.filter { query.isBlank() || it.name.contains(query, true) || p.name.contains(query, true) }.forEach { s ->
                    HorizontalDivider(color = Line)
                    Row(Modifier.fillMaxWidth().clickable { nav.setPicked("pickedCategory", "${p.id}|${s.id}"); nav.popBackStack() }.padding(start = 24.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconCircle(s.icon, p.colorKey, 34.dp)
                        Text(s.name, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryEditScreen(nav: NavController, entry: NavBackStackEntry, id: String, kind: String) {
    val look = rememberLookup()
    val existing = remember(id, look.categories.isEmpty()) { look.category(id) }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var icon by remember(existing) { mutableStateOf(existing?.icon ?: "other") }
    var color by remember(existing) { mutableStateOf(existing?.colorKey ?: "blue") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    entry.Picked<String>("pickedColor") { color = it }
    val subs = look.subs(id)
    ScreenScaffold(if (existing == null) "New category" else "Edit category", { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, "Delete", { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar("Save", saver, name.isNotBlank(), onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put("categories", id, Category(name = name.trim(), icon = icon, colorKey = color, kind = existing?.kind ?: kind))
                    // Sub-categories follow the parent colour.
                    if (existing != null && existing.colorKey != color) subs.forEach { Repo.put("categories", it.id, it.copy(colorKey = color)) }
                }
            })
        }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, "Category name")
        Card(padding = 12.dp) {
            PickRowLeading("Icon", iconLabel(icon), { IconCircle(icon, color, 38.dp) }, { nav.navigate(Routes.iconPicker("category", icon)) })
            PickRowLeading("Colour", colorSpec(color).label, { Box(Modifier.size(38.dp).clip(CircleShape).background(colorSpec(color).brush)) },
                { nav.navigate(Routes.colorPicker(color)) }, divider = false)
        }
        SectionTitle("Sub-categories")
        if (existing == null) Hint("Save the category first, then open it again to add sub-categories.")
        else Card(padding = 12.dp) {
            subs.forEach { s ->
                SettingRow(s.name, leading = { IconCircle(s.icon, color, 38.dp) }, onClick = { nav.navigate(Routes.subEdit(s.id, id)) })
            }
            SettingRow("Add sub-category", icon = Icons.Filled.Add, iconColor = Primary, onClick = { nav.navigate(Routes.subEdit("", id)) }, divider = false)
        }
    }
    if (confirm) ConfirmDialog("Delete category?", "Its sub-categories are deleted too. Past transactions keep their amounts but show as uncategorised.", "Delete", {
        saver.run(scope, onDone = { nav.popBackStack() }) { Repo.deleteCategory(id) }
    }, { confirm = false }, danger = true)
}

@Composable
fun SubcategoryEditScreen(nav: NavController, entry: NavBackStackEntry, id: String, parentId: String) {
    val look = rememberLookup()
    val parent = look.category(parentId)
    val existing = remember(id, look.categories.isEmpty()) { look.category(id) }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var icon by remember(existing) { mutableStateOf(existing?.icon ?: parent?.icon ?: "other") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedIcon") { icon = it }
    val color = parent?.colorKey ?: "slate"
    ScreenScaffold(if (existing == null) "New sub-category" else "Edit sub-category", { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, "Delete", { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar("Save", saver, name.isNotBlank() && parent != null, onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put("categories", id, Category(name = name.trim(), icon = icon, colorKey = color, parentId = parentId, kind = parent!!.kind))
                }
            })
        }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(icon, color, 84.dp) }
        AppTextField(name, { name = it }, "Sub-category name")
        Card(padding = 12.dp) {
            PickRowLeading("Icon", iconLabel(icon), { IconCircle(icon, color, 38.dp) }, { nav.navigate(Routes.iconPicker("category", icon)) })
            SettingRow("Colour", "Same as ${parent?.name.orEmpty()}  ·  Inherited",
                leading = { Box(Modifier.size(38.dp).clip(CircleShape).background(colorSpec(color).brush)) },
                trailing = { Icon(Icons.Filled.Lock, null, tint = Faint) }, divider = false)
        }
        Hint("Sub-categories always use their category's colour. Change the colour on the category itself.")
    }
    if (confirm) ConfirmDialog("Delete sub-category?", "Past transactions keep their category.", "Delete", {
        saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("categories", id) }
    }, { confirm = false }, danger = true)
}

// ------------------------------------------------------------------ Colour, icon, account, date pickers

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerScreen(nav: NavController, current: String, locked: String) {
    var chosen by remember { mutableStateOf(current.ifBlank { "blue" }) }
    @Composable fun Swatch(c: ColorSpec) {
        val on = c.key == chosen
        Box(Modifier.size(64.dp).clip(CircleShape).background(c.brush).let { if (on) it.border(3.dp, Ink, CircleShape) else it }.clickable { chosen = c.key },
            contentAlignment = Alignment.Center) { if (on) Icon(Icons.Filled.Check, "Selected", tint = Color.White) }
    }
    ScreenScaffold("Choose colour", { nav.popBackStack() }, bottomBar = {
        BottomActionBar { PrimaryButton("Use ${colorSpec(chosen).label}", { nav.setPicked("pickedColor", chosen); nav.popBackStack() }) }
    }) {
        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(28.dp)).background(colorSpec(chosen).brush), contentAlignment = Alignment.Center) {
            Text(colorSpec(chosen).label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
        SectionTitle("Solid")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { SolidColors.forEach { Swatch(it) } }
        SectionTitle("Gradients")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { GradientColors.forEach { Swatch(it) } }
        Hint("The same colours and gradients are used for accounts and categories. Sub-categories follow their parent.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerScreen(nav: NavController, group: String, current: String) {
    val groups = if (group == "account") AccountIconGroups else CategoryIconGroups
    var chosen by remember { mutableStateOf(current.ifBlank { "other" }) }
    ScreenScaffold(if (group == "account") "Choose account icon" else "Choose category icon", { nav.popBackStack() }, bottomBar = {
        BottomActionBar { PrimaryButton("Use this icon", { nav.setPicked("pickedIcon", chosen); nav.popBackStack() }) }
    }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(chosen, "blue", 84.dp) }
        groups.forEach { g ->
            SectionTitle(g.title)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                g.keys.forEach { k ->
                    val on = k == chosen
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(if (on) PrimaryContainer else Surface)
                        .let { if (on) it.border(2.dp, Primary, RoundedCornerShape(20.dp)) else it }.clickable { chosen = k }, contentAlignment = Alignment.Center) {
                        Icon(iconFor(k), iconLabel(k), tint = if (on) Primary else Muted, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AccountPickerScreen(nav: NavController, slot: String, exclude: String) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val list = accounts.filter { !it.archived && it.id != exclude }
    ScreenScaffold("Select account", { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, "New account", { nav.navigate(Routes.ACCOUNT_TYPE) }) }) {
        if (list.isEmpty()) {
            EmptyState(Icons.Filled.AccountBalance, "No accounts yet", "Add a bank, wallet, cash or card to choose it here.")
            PrimaryButton("Add account", { nav.navigate(Routes.ACCOUNT_TYPE) })
        } else Card(padding = 12.dp) {
            list.forEachIndexed { i, a ->
                val bal = Repo.balanceOf(a.id, accounts, txns)
                SettingRow(a.name, leading = { IconCircle(a.icon, a.colorKey, 44.dp) },
                    onClick = { nav.setPicked(if (slot == "to") "pickedAccount2" else "pickedAccount", a.id); nav.popBackStack() },
                    trailing = { Text(formatPaise(bal), fontWeight = FontWeight.ExtraBold, color = if (bal < 0) Danger else Ink) }, divider = i < list.lastIndex)
            }
        }
    }
}

@Composable
fun DatePickerScreen(nav: NavController, millis: Long) {
    val start = if (millis > 0) toLocalDate(millis) else LocalDate.now()
    var ym by remember { mutableStateOf(YearMonth.from(start)) }
    var picked by remember { mutableStateOf(start) }
    ScreenScaffold("Select date", { nav.popBackStack() },
        actions = { Text("Today", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable { picked = LocalDate.now(); ym = YearMonth.now() }.padding(10.dp)) },
        bottomBar = { BottomActionBar { PrimaryButton("Use ${fullDate(picked.startMillis())}", { nav.setPicked("pickedDate", picked.startMillis()); nav.popBackStack() }) } }) {
        Card {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month", Modifier.clip(CircleShape).clickable { ym = ym.minusMonths(1) }.padding(8.dp))
                Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 16.sp)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month", Modifier.clip(CircleShape).clickable { ym = ym.plusMonths(1) }.padding(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted, fontWeight = FontWeight.Bold, fontSize = 12.sp) } }
            val lead = ym.atDay(1).dayOfWeek.value - 1
            val days = ym.lengthOfMonth()
            for (r in 0 until (lead + days + 6) / 7) Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val d = ym.atDay(day)
                            val on = d == picked
                            Box(Modifier.size(40.dp).clip(CircleShape).background(if (on) Primary else Color.Transparent)
                                .let { if (d == LocalDate.now() && !on) it.border(1.5.dp, Primary, CircleShape) else it }.clickable { picked = d }, contentAlignment = Alignment.Center) {
                                Text("$day", color = if (on) Color.White else Ink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
