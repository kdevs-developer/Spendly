package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.theme.*

@Composable
fun TxnDetailScreen(
    nav: NavController,
    id: String,
) {
    val txns by Repo.txns.collectAsState()
    val look = rememberLookup()
    val t = txns.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    ScreenScaffold(
        str(R.string.transaction),
        { nav.popBackStack() },
        actions = { if (t != null) RoundIconButton(Icons.Filled.Edit, str(R.string.edit), { nav.navigate(Routes.txnEdit(id)) }) },
    ) {
        if (t == null) {
            EmptyState(Icons.Filled.SearchOff, str(R.string.not_found), str(R.string.this_transaction_was_deleted))
            return@ScreenScaffold
        }
        val cat = look.category(t.subcategoryId.ifBlank { t.categoryId })
        val parent = look.category(t.categoryId)
        val color = if (t.kind == "transfer") "slate" else parent?.colorKey ?: cat?.colorKey ?: "slate"
        HeroCard(brush = colorSpec(color).brush) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconFor(if (t.kind == "transfer") "invest" else cat?.icon ?: "other"), null, tint = Color.White)
                }
                Column {
                    Text(
                        if (t.kind == "transfer") str(R.string.transfer) else cat?.name ?: str(R.string.uncategorised),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(t.kind.replaceFirstChar { it.uppercase() }, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                (
                        if (t.kind == "expense") {
                            "-"
                        } else if (t.kind == "income") {
                            "+"
                        } else {
                            ""
                        }
                        ) + formatPaise(t.amountPaise),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
            )
        }
        Card(padding = 12.dp) {
            if (t.kind == "transfer") {
                PickRow(str(R.string.from), look.accountName(t.accountId), Icons.Filled.AccountBalanceWallet, Primary, {}, divider = true)
                PickRow(str(R.string.to_2), look.accountName(t.toAccountId), Icons.Filled.AccountBalance, Success, {}, divider = true)
            } else {
                PickRow(str(R.string.account), look.accountName(t.accountId), Icons.Filled.AccountBalanceWallet, Primary, {
                    nav.navigate(Routes.accountDetail(t.accountId))
                })
                if (parent != null) {
                    PickRow(
                        str(
                            R.string.category,
                        ),
                        parent.name + if (t.subcategoryId.isNotBlank()) " · ${look.categoryName(t.subcategoryId)}" else "",
                        Icons.Filled.Category,
                        clr(
                            R.color.swatch_purple,
                        ),
                        {
                        },
                    )
                }
            }
            PickRow(str(R.string.date), fullDate(t.dateMillis) + if (hasTime(t.dateMillis)) ", " + timeOf(t.dateMillis) else "", Icons.Filled.CalendarMonth, Muted, {}, divider = t.note.isNotBlank())
            if (t.note.isNotBlank()) PickRow(str(R.string.note), t.note, Icons.Filled.Notes, Muted, {}, divider = false)
        }
        if (t.kind != "transfer") {
            SecondaryButton(str(R.string.duplicate), {
                saver.run(scope, onDone = { nav.popBackStack() }) { Repo.put("txns", "", t.copy(id = "", dateMillis = withNowTime(todayMillis()))) }
            })
        }
        SecondaryButton(str(R.string.delete_transaction_2), { confirm = true }, danger = true)
    }
    if (confirm) {
        ConfirmDialog(str(R.string.delete_transaction), str(R.string.this_cannot_be_undone), str(R.string.delete), {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.remove("txns", id) }
        }, { confirm = false }, danger = true)
    }
}
