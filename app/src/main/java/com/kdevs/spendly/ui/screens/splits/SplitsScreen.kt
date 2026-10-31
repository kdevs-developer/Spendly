package com.kdevs.spendly.ui.screens.splits

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.kdevs.spendly.ui.theme.*

@Composable
fun SplitsScreen(
    nav: NavController,
    id: String,
) {
    val me = Repo.uid
    val chat = rememberChat(id)
    val entries = rememberSplits(id)
    val saver = rememberSaver()
    val ctx = LocalContext.current
    val balances = SplitMath.balances(entries)
    val mine = balances[me] ?: 0L
    val plan = SplitMath.simplify(balances)

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(chat?.name ?: str(R.string.splits), { nav.popBackStack() }) {
            HeroCard(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(clr(R.color.swatch_teal), clr(R.color.swatch_teal_dark))),
            ) {
                Text(
                    if (mine == 0L) {
                        str(R.string.all_settled_up)
                    } else if (mine > 0) {
                        str(R.string.you_are_owed)
                    } else {
                        str(R.string.you_owe)
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(formatPaise(kotlin.math.abs(mine)), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
                Spacer(Modifier.height(10.dp))
                chat?.members?.forEach { u ->
                    val b = balances[u] ?: 0L
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            chat?.nameOf(u, me) ?: "",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(formatPaise(b, withSign = true), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            if (plan.isNotEmpty()) {
                SectionTitle(str(R.string.to_settle_up))
                Card(padding = 12.dp) {
                    plan.forEachIndexed { i, t ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Avatar(chat?.nameOf(t.from, me) ?: "?", colorKeyFor(t.from), 34.dp)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Faint, modifier = Modifier.size(18.dp))
                            Avatar(chat?.nameOf(t.to, me) ?: "?", colorKeyFor(t.to), 34.dp)
                            Text(
                                str(R.string.pays, chat?.nameOf(t.from, me), chat?.nameOf(t.to, me)),
                                Modifier.weight(1f),
                                color = Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(formatPaise(t.paise), fontWeight = FontWeight.ExtraBold, color = Ink)
                        }
                        if (i < plan.lastIndex) HorizontalDivider(color = Line)
                    }
                }
                SecondaryButton(str(R.string.settle_up), { nav.navigate(Routes.settle(id)) })
            }
            SectionTitle(str(R.string.expenses))
            saver.error?.let { Text(it, color = Danger) }
            if (entries.isEmpty()) {
                EmptyState(Icons.Filled.Receipt, str(R.string.no_expenses_yet), str(R.string.add_the_first_shared_expense))
            } else {
                Card(padding = 12.dp) {
                    entries.forEachIndexed { i, e ->
                        val settle = e.kind == "settlement"
                        SettingRow(
                            if (settle) str(R.string.person_paid_person, chat?.nameOf(e.paidBy, me), chat?.nameOf(e.settleTo, me)) else e.description,
                            str(R.string.text__2, fullDate(e.dateMillis)) + if (!settle) " · paid by ${chat?.nameOf(e.paidBy, me)}" else "",
                            icon = if (settle) Icons.Filled.CheckCircle else Icons.Filled.Receipt,
                            iconColor = if (settle) Success else clr(R.color.swatch_teal),
                            trailing = { Text(formatPaise(e.totalPaise), fontWeight = FontWeight.ExtraBold, color = Ink) },
                            onClick = { nav.navigate(Routes.splitEntry(id, e.id)) },
                            divider = i < entries.lastIndex,
                        )
                    }
                }
            }
            if (entries.isNotEmpty()) {
                Hint(str(R.string.tap_entry_hint))
                SecondaryButton(str(R.string.share_summary), {
                    val text =
                        buildString {
                            append(chat?.name?.ifBlank { null } ?: str(R.string.spendly_split)).append("\n")
                            if (plan.isEmpty()) {
                                append(str(R.string.all_settled_up))
                            } else {
                                plan.forEach { t -> append(str(R.string.pays_with_amount, chat?.nameOf(t.from, me), chat?.nameOf(t.to, me), formatPaise(t.paise))).append("\n") }
                            }
                        }.trim()
                    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                    ctx.startActivity(Intent.createChooser(i, "Share summary"))
                })
            }
            Spacer(Modifier.height(80.dp))
        }
        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) {
            Fab(Icons.Filled.Add, str(R.string.add_expense)) { nav.navigate(Routes.addSplit(id)) }
        }
    }
}

/** One expense or payment: who paid, the split type, and what each person owes. The person who added it can delete it. */
@Composable
fun SplitEntryScreen(
    nav: NavController,
    chatId: String,
    entryId: String,
) {
    val me = Repo.uid
    val chat = rememberChat(chatId)
    val entries = rememberSplits(chatId)
    val e = entries.firstOrNull { it.id == entryId }
    var confirm by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    ScreenScaffold(if (e?.kind == "settlement") str(R.string.payment) else str(R.string.expense), { nav.popBackStack() }) {
        if (e == null) {
            EmptyState(Icons.Filled.SearchOff, str(R.string.not_found), "This entry was deleted.")
            return@ScreenScaffold
        }
        val settle = e.kind == "settlement"
        HeroCard(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(clr(R.color.swatch_teal), clr(R.color.swatch_teal_dark)))) {
            Text(
                if (settle) str(R.string.person_paid_person, chat?.nameOf(e.paidBy, me), chat?.nameOf(e.settleTo, me)) else e.description,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(formatPaise(e.totalPaise), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
        }
        Card(padding = 12.dp) {
            if (!settle) SettingRow(str(R.string.paid_by), chat?.nameOf(e.paidBy, me) ?: "", icon = Icons.Filled.Person, iconColor = Primary)
            SettingRow(
                str(R.string.date),
                fullDate(e.dateMillis),
                icon = Icons.Filled.CalendarMonth,
                iconColor = Muted,
                divider = !settle,
            )
            if (!settle) {
                SettingRow(
                    str(R.string.split_word),
                    when (e.splitType) {
                        "exact" -> str(R.string.exact_amounts)
                        "percent" -> str(R.string.by_percentage)
                        "shares" -> str(R.string.by_shares)
                        else -> str(R.string.equally)
                    },
                    icon = Icons.Filled.CallSplit,
                    iconColor = clr(R.color.swatch_teal),
                    divider = false,
                )
            }
        }
        if (!settle && e.owed.isNotEmpty()) {
            SectionTitle(str(R.string.who_owes_what))
            Card(padding = 12.dp) {
                val list = e.owed.entries.toList()
                list.forEachIndexed { i, (u, p) ->
                    SettingRow(
                        chat?.nameOf(u, me) ?: "",
                        leading = { Avatar(chat?.nameOf(u, me) ?: "?", colorKeyFor(u), 38.dp) },
                        trailing = { Text(formatPaise(p), fontWeight = FontWeight.ExtraBold, color = Ink) },
                        divider = i < list.lastIndex,
                    )
                }
            }
        }
        saver.error?.let { Text(it, color = Danger) }
        if (e.createdBy == me) {
            SecondaryButton(str(R.string.delete), { confirm = true }, danger = true)
        } else {
            Hint(str(R.string.only_adder_can_delete), center = true)
        }
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_this_entry), str(R.string.balances_will_be_recalculated), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.deleteSplit(chatId, entryId) }
        }, { confirm = false }, danger = true)
    }
}
