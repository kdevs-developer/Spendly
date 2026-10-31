package com.kdevs.spendly.ui.screens.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AccountPickerScreen(
    nav: NavController,
    slot: String,
    exclude: String,
) {
    val accounts by Repo.accounts.collectAsState()
    val txns by Repo.txns.collectAsState()
    val list = accounts.filter { !it.archived && it.id != exclude }
    ScreenScaffold(
        str(R.string.select_account),
        { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.Add, str(R.string.new_account), { nav.navigate(Routes.ACCOUNT_TYPE) }) },
    ) {
        if (list.isEmpty()) {
            EmptyState(Icons.Filled.AccountBalance, str(R.string.no_accounts_yet), str(R.string.add_a_bank_wallet_cash_or_2))
            PrimaryButton(str(R.string.add_account), { nav.navigate(Routes.ACCOUNT_TYPE) })
        } else {
            Card(padding = 12.dp) {
                list.forEachIndexed { i, a ->
                    val bal = Repo.balanceOf(a.id, accounts, txns)
                    SettingRow(
                        a.name,
                        leading = { IconCircle(a.icon, a.colorKey, 44.dp) },
                        onClick = {
                            nav.setPicked(if (slot == "to") "pickedAccount2" else "pickedAccount", a.id)
                            nav.popBackStack()
                        },
                        trailing = { Text(formatPaise(bal), fontWeight = FontWeight.ExtraBold, color = if (bal < 0) Danger else Ink) },
                        divider = i < list.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
fun DatePickerScreen(
    nav: NavController,
    millis: Long,
) {
    val start = if (millis > 0) toLocalDate(millis) else LocalDate.now()
    var ym by remember { mutableStateOf(YearMonth.from(start)) }
    var picked by remember { mutableStateOf(start) }
    ScreenScaffold(
        str(R.string.select_date),
        { nav.popBackStack() },
        actions = {
            Text(
                str(R.string.today),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .clickable {
                            picked = LocalDate.now()
                            ym = YearMonth.now()
                        }.padding(10.dp),
            )
        },
        bottomBar = {
            BottomActionBar {
                PrimaryButton(str(R.string.use_2, fullDate(picked.startMillis())), {
                    nav.setPicked("pickedDate", picked.startMillis())
                    nav.popBackStack()
                })
            }
        },
    ) {
        Card {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    str(R.string.previous_month),
                    Modifier
                        .clip(CircleShape)
                        .clickable {
                            ym = ym.minusMonths(1)
                        }.padding(8.dp),
                )
                Text(
                    str(R.string.text__4, ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()), ym.year),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    fontSize = 16.sp,
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    str(R.string.next_month),
                    Modifier
                        .clip(CircleShape)
                        .clickable {
                            ym = ym.plusMonths(1)
                        }.padding(8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(
                        it,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
            val lead = ym.atDay(1).dayOfWeek.value - 1
            val days = ym.lengthOfMonth()
            for (r in 0 until (lead + days + 6) / 7) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c - lead + 1
                        Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                            if (day in 1..days) {
                                val d = ym.atDay(day)
                                val on = d == picked
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (on) Primary else Color.Transparent)
                                        .let {
                                            if (d == LocalDate.now() && !on) {
                                                it.border(
                                                    1.5.dp,
                                                    Primary,
                                                    CircleShape,
                                                )
                                            } else {
                                                it
                                            }
                                        }.clickable { picked = d },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(str(R.string.text__2, day), color = if (on) Color.White else Ink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
