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
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Templates

@Composable
fun TemplatesScreen(nav: NavController) {
    val templates by Repo.templates.collectAsState()
    val look = rememberLookup()
    val accounts by Repo.accounts.collectAsState()
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    ScreenScaffold(
        str(R.string.templates),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.new_template), { nav.navigate(Routes.templateEdit("")) }) },
    ) {
        Hint(str(R.string.tap_use_to_add_the_transaction))
        saver.error?.let { Text(it, color = Danger) }
        if (templates.isEmpty()) {
            EmptyState(
                Icons.Filled.ContentCopy,
                str(R.string.no_templates_yet),
                str(R.string.save_the_things_you_buy_often),
            )
        }
        templates.forEach { t ->
            val c = look.category(t.categoryId)
            Card {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.weight(1f).clickable {
                            nav.navigate(Routes.templateEdit(t.id))
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 42.dp)
                        Column {
                            Text(t.name, fontWeight = FontWeight.ExtraBold, color = Ink)
                            Text(
                                str(R.string.text__6, formatPaise(t.amountPaise), look.accountName(t.accountId)),
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(PrimaryContainer)
                            .clickable {
                                val acc = t.accountId.ifBlank { accounts.firstOrNull { !it.archived }?.id ?: "" }
                                if (acc.isBlank()) {
                                    saver.error = str(R.string.add_an_account_first)
                                    return@clickable
                                }
                                saver.run(scope, onDone = { nav.popBackStack() }) {
                                    Repo.put(
                                        "txns",
                                        "",
                                        Txn(
                                            kind = t.kind,
                                            amountPaise = t.amountPaise,
                                            accountId = acc,
                                            categoryId = t.categoryId,
                                            note =
                                                t.note.ifBlank {
                                                    t.name
                                                },
                                            dateMillis = withNowTime(todayMillis()),
                                        ),
                                    )
                                }
                            }.padding(horizontal = 16.dp, vertical = 9.dp),
                    ) { Text(str(R.string.use), color = Primary, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}

@Composable
fun TemplateEditScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val templates by Repo.templates.collectAsState()
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val existing = remember(id, templates.isEmpty()) { templates.firstOrNull { it.id == id } }
    var name by rememberSaveable(existing) { mutableStateOf(existing?.name ?: "") }
    var kind by rememberSaveable(existing) { mutableStateOf(existing?.kind ?: "expense") }
    var amount by rememberSaveable(existing) { mutableStateOf(existing?.let { paiseToField(it.amountPaise) } ?: "") }
    var accountId by rememberSaveable(existing) { mutableStateOf(existing?.accountId ?: "") }
    var catId by rememberSaveable(existing) { mutableStateOf(existing?.categoryId ?: "") }
    var note by rememberSaveable(existing) { mutableStateOf(existing?.note ?: "") }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    entry.Picked<String>("pickedAccount") { accountId = it }
    entry.Picked<String>("pickedCategory") { catId = splitPick(it).first }
    val paise = parseRupees(amount) ?: 0L
    val c = look.category(catId)
    val a = look.account(accountId)
    ScreenScaffold(
        if (existing == null) str(R.string.new_template) else str(R.string.edit_template),
        { nav.popBackStack() },
        actions = { if (existing != null) RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger) },
        bottomBar = {
            SavedBar(str(R.string.save_template), saver, name.isNotBlank() && paise > 0, onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.put(
                        "templates",
                        id,
                        Template(
                            name = name.trim(),
                            kind = kind,
                            amountPaise = paise,
                            accountId = accountId,
                            categoryId = catId,
                            note = note.trim(),
                        ),
                    )
                }
            })
        },
    ) {
        SegmentedControl(listOf(str(R.string.expense), str(R.string.income)), if (kind == "income") 1 else 0, {
            kind = if (it == 1) "income" else "expense"
            catId = ""
        })
        AppTextField(name, { name = it }, str(R.string.template_name))
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 6.dp), color = if (kind == "income") Success else Danger)
        Card(padding = 12.dp) {
            PickRowLeading(str(R.string.category), c?.name ?: "", {
                IconCircle(c?.icon ?: "other", c?.colorKey ?: "slate", 38.dp)
            }, { nav.navigate(Routes.categoryPicker(kind, "template")) })
            PickRowLeading(str(R.string.account), a?.name ?: str(R.string.ask_each_time), {
                IconCircle(a?.icon ?: "bank", a?.colorKey ?: "blue", 38.dp)
            }, { nav.navigate(Routes.accountPicker("account")) }, divider = false)
        }
        AppTextField(note, { note = it }, str(R.string.note_optional))
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_template), str(R.string.transactions_already_made_from_it_stay), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("templates", id) }
        }, { confirm = false }, danger = true)
    }
}
