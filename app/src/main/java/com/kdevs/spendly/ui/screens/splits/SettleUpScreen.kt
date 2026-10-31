package com.kdevs.spendly.ui.screens.splits

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.money.PickRow
import com.kdevs.spendly.ui.screens.money.PickRowLeading
import com.kdevs.spendly.ui.screens.money.SavedBar
import com.kdevs.spendly.ui.theme.*

@Composable
fun SettleUpScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val me = Repo.uid
    val chat = rememberChat(id)
    val entries = rememberSplits(id)
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val plan = SplitMath.simplify(SplitMath.balances(entries)).filter { it.from == me || it.to == me }
    var sel by remember(plan.size) { mutableIntStateOf(0) }
    val t = plan.getOrNull(sel)
    var amount by rememberSaveable(t) { mutableStateOf(t?.let { paiseToField(it.paise) } ?: "") }
    var accountId by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(todayMillis()) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    LaunchedEffect(accounts) { if (accountId.isBlank()) accountId = accounts.firstOrNull { !it.archived }?.id ?: "" }
    entry.Picked<String>("pickedAccount") { accountId = it }
    entry.Picked<Long>("pickedDate") { date = it }

    val paise = parseRupees(amount) ?: 0L
    val iPay = t?.from == me
    val other = t?.let { if (iPay) it.to else it.from }
    val acc = look.account(accountId)

    ScreenScaffold(str(R.string.settle_up), { nav.popBackStack() }, bottomBar = {
        SavedBar(
            if (t == null) {
                str(R.string.nothing_to_settle)
            } else if (iPay) {
                str(R.string.record_paid, formatPaise(paise))
            } else {
                str(R.string.record_received, formatPaise(paise))
            },
            saver,
            t != null && paise > 0 && paise <= t.paise && accountId.isNotBlank(),
            onSave = {
                saver.run(scope, onDone = { nav.popBackStack() }) {
                    Repo.addSplit(
                        id,
                        SplitEntry(
                            kind = "settlement",
                            description =
                                note.trim().ifBlank {
                                    str(R.string.settle_up)
                                },
                            totalPaise = paise,
                            paidBy = t!!.from,
                            settleTo = t.to,
                            dateMillis = date,
                        ),
                    )
                    // Also note it in your own accounts so your balance matches reality.
                    Repo.put(
                        "txns",
                        "",
                        Txn(
                            kind = if (iPay) "expense" else "income",
                            amountPaise = paise,
                            accountId = accountId,
                            note = str(R.string.settled_with, chat?.nameOf(other!!, me)),
                            dateMillis = withNowTime(date),
                        ),
                    )
                }
            },
        )
    }) {
        if (t == null) {
            EmptyState(Icons.Filled.CheckCircle, str(R.string.all_settled), str(R.string.no_one_owes_you_and_you))
            return@ScreenScaffold
        }
        if (plan.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.forEachIndexed {
                    i,
                    p,
                    ->
                    SelectChip(chat?.nameOf(if (p.from == me) p.to else p.from, me) ?: "?", i == sel) { sel = i }
                }
            }
        }
        HeroCard(
            brush =
                androidx.compose.ui.graphics.Brush
                    .linearGradient(listOf(clr(R.color.swatch_teal), clr(R.color.swatch_teal_dark))),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Avatar(chat?.nameOf(t.from, me) ?: "?", colorKeyFor(t.from), 56.dp)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp).size(28.dp),
                )
                Avatar(chat?.nameOf(t.to, me) ?: "?", colorKeyFor(t.to), 56.dp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (iPay) str(R.string.you_pay, chat?.nameOf(t.to, me)) else "${chat?.nameOf(t.from, me)} pays you",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                formatPaise(paise),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                str(R.string.suggested, formatPaise(t.paise)),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        AmountField(amount, { amount = it }, color = Ink)
        if (paise > t.paise) {
            Text(
                str(R.string.that_is_more_than_the_owed, formatPaise(t.paise)),
                color = Danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Card(padding = 12.dp) {
            PickRowLeading(if (iPay) str(R.string.money_comes_out_of) else str(R.string.money_goes_into), acc?.name ?: "", {
                IconCircle(acc?.icon ?: "bank", acc?.colorKey ?: "blue", 38.dp)
            }, { nav.navigate(Routes.accountPicker("account")) })
            PickRow(
                str(R.string.date),
                fullDate(date),
                Icons.Filled.CalendarMonth,
                Muted,
                { nav.navigate(Routes.datePicker(date)) },
                divider = false,
            )
        }
        AppTextField(note, { note = it }, str(R.string.note_optional))
        Hint(str(R.string.spendly_only_records_this_payment_it))
        saver.error?.let { Text(it, color = Danger) }
    }
}
