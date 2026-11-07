package com.kdevs.spendly.ui.screens.export

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.Txn
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.earned
import com.kdevs.spendly.ui.screens.common.rememberLookup
import com.kdevs.spendly.ui.screens.common.spent
import com.kdevs.spendly.ui.screens.money.PickRow
import com.kdevs.spendly.ui.screens.money.SavedBar
import com.kdevs.spendly.ui.state.TxnFilter
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

private const val PDF = "application/pdf"

private val PERIODS =
    listOf(
        "month" to str(R.string.this_month),
        "lastmonth" to str(R.string.last_month),
        "last30" to str(R.string.last_30_days),
        "year" to str(R.string.this_year),
        "all" to str(R.string.all_time),
        "custom" to str(R.string.custom_range),
    )

/**
 * Export transactions to Excel (.xlsx) or PDF. The file is saved wherever the person chooses
 * (no storage permission needed).
 *
 * Layout:
 *  - Format toggle
 *  - "Use Activity filter" switch (when on, Period / Types / Accounts are ignored and dimmed)
 *  - Period row (collapsible list, inline From/To when Custom)
 *  - Transaction type tiles (multi-select, check icon when selected)
 *  - Accounts row (collapsible multi-select list)
 *  - Summary card, then the pinned export button
 */
@Composable
fun ExportScreen(
    nav: NavController,
    entry: NavBackStackEntry,
) {
    val txns by Repo.txns.collectAsState()
    val accounts by Repo.accounts.collectAsState()
    val look = rememberLookup()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()

    var format by remember { mutableIntStateOf(0) } // 0 Excel, 1 PDF
    var useFilter by rememberSaveable { mutableStateOf(false) }
    var period by rememberSaveable { mutableStateOf("month") }
    var from by rememberSaveable { mutableStateOf(monthStart()) }
    var to by rememberSaveable { mutableStateOf(todayMillis()) }
    var pickingFrom by rememberSaveable { mutableStateOf(true) }
    var kinds by rememberSaveable { mutableStateOf(setOf("expense", "income", "transfer")) }
    var accountIds by rememberSaveable { mutableStateOf(setOf<String>()) } // empty = every account
    var periodOpen by remember { mutableStateOf(false) }
    var accountsOpen by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf<Int?>(null) } // string id of the loader text, null = idle
    entry.Picked<Long>("pickedDate") { if (pickingFrom) from = it else to = it }

    val today = LocalDate.now()
    val range: Pair<Long, Long> =
        when (period) {
            "lastmonth" -> today.minusMonths(1).withDayOfMonth(1).startMillis() to monthStart()
            "last30" -> today.minusDays(29).startMillis() to today.plusDays(1).startMillis()
            "year" -> today.withDayOfYear(1).startMillis() to today.withDayOfYear(1).plusYears(1).startMillis()
            "all" -> 0L to Long.MAX_VALUE
            "custom" -> minOf(from, to) to (maxOf(from, to) + 86_400_000L)
            else -> monthStart() to nextMonthStart()
        }
    val periodName = PERIODS.firstOrNull { it.first == period }?.second ?: str(R.string.this_month)
    val periodDates =
        if (period == "all") str(R.string.every_transaction) else "${fullDate(range.first)} – ${fullDate(range.second - 1)}"
    val label =
        when {
            useFilter -> str(R.string.current_activity_filter)
            period == "custom" -> str(R.string.to, fullDate(minOf(from, to)), fullDate(maxOf(from, to)))
            else -> periodName
        }

    val live = accounts.filter { !it.archived }
    val accountSummary =
        when {
            accountIds.isEmpty() -> str(R.string.all_accounts)
            accountIds.size == 1 -> live.firstOrNull { it.id in accountIds }?.name ?: str(R.string.one_account)
            else -> str(R.string.accounts_2, accountIds.size)
        }

    val list: List<Txn> =
        remember(txns, useFilter, period, from, to, kinds, accountIds, look, TxnFilter.activeCount, TxnFilter.query) {
            txns.filter { t ->
                if (useFilter) {
                    TxnFilter.matches(t) { id -> look.categoryName(id) }
                } else {
                    t.dateMillis in range.first until range.second &&
                        t.kind in kinds &&
                        (accountIds.isEmpty() || t.accountId in accountIds || t.toAccountId in accountIds)
                }
            }
        }

    fun persist(
        uri: Uri?,
        mime: String,
    ) {
        val bytes = pending
        pending = null
        if (uri == null || bytes == null) {
            working = null
            return
        }
        working = R.string.saving_your_file
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error(str(R.string.could_not_save_the_file))
                }
                saved = uri to mime
            } catch (e: Exception) {
                note = e.message ?: str(R.string.could_not_save_the_file)
            } finally {
                working = null
            }
        }
    }

    val xlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(XLSX)) { persist(it, XLSX) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF)) { persist(it, PDF) }

    LaunchedEffect(saver.error) { if (saver.error != null) working = null }

    fun <T> Set<T>.flip(v: T) = if (v in this) this - v else this + v

    ScreenScaffold(str(R.string.export), { nav.popBackStack() }, busy = working != null, busyText = working?.let { str(it) }, bottomBar = {
        SavedBar(
            if (format == 0) str(R.string.export_to_excel) else str(R.string.export_to_pdf),
            saver,
            enabled = list.isNotEmpty(),
            onSave = {
                saved = null
                note = null
                working = R.string.preparing_your_file
                saver.run(scope, onDone = {}) {
                    val bytes =
                        withContext(Dispatchers.IO) {
                            if (format == 0) Exporter.xlsx(list, look, label) else Exporter.pdf(list, look, label)
                        }
                    pending = bytes
                    working = null
                    if (format == 0) xlsxLauncher.launch(Exporter.fileName("xlsx")) else pdfLauncher.launch(Exporter.fileName("pdf"))
                }
            },
        )
    }) {
        SegmentedControl(listOf(str(R.string.excel_xlsx), str(R.string.pdf)), format, { format = it })
        Hint(
            if (format == 0) {
                str(R.string.export_excel_hint)
            } else {
                str(R.string.export_pdf_hint)
            },
        )

        // ── Activity filter switch ────────────────────────────────────────────
        Card(padding = 0.dp) {
            ExportRow(
                icon = Icons.Filled.FilterList,
                title = str(R.string.use_activity_filter),
                subtitle = str(R.string.export_what_you_see),
                onClick = {
                    useFilter = !useFilter
                    periodOpen = false
                    accountsOpen = false
                },
            ) {
                Switch(checked = useFilter, onCheckedChange = {
                    useFilter = it
                    periodOpen = false
                    accountsOpen = false
                })
            }
        }
        if (useFilter && TxnFilter.activeCount == 0 && TxnFilter.query.isBlank()) {
            Hint(str(R.string.no_activity_filter_set))
        }

        // Everything below is ignored while the Activity filter is on.
        Column(
            Modifier.alpha(if (useFilter) 0.4f else 1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Period ────────────────────────────────────────────────────────
            Card(padding = 0.dp) {
                ExportRow(
                    icon = Icons.Filled.CalendarMonth,
                    overline = str(R.string.period),
                    title = periodName,
                    subtitle = periodDates,
                    enabled = !useFilter,
                    onClick = { periodOpen = !periodOpen },
                ) {
                    Chevron(periodOpen)
                }
                AnimatedVisibility(periodOpen && !useFilter) {
                    Column {
                        PERIODS.forEach { (k, v) ->
                            Hairline()
                            OptionRow(v, selected = period == k) {
                                period = k
                                periodOpen = false
                            }
                        }
                    }
                }
                if (period == "custom") {
                    Hairline()
                    Column(Modifier.padding(horizontal = 12.dp)) {
                        PickRow(str(R.string.from), fullDate(from), Icons.Filled.CalendarMonth, Primary, {
                            if (!useFilter) {
                                pickingFrom = true
                                nav.navigate(Routes.datePicker(from))
                            }
                        })
                        PickRow(str(R.string.to_2), fullDate(to), Icons.Filled.CalendarMonth, Primary, {
                            if (!useFilter) {
                                pickingFrom = false
                                nav.navigate(Routes.datePicker(to))
                            }
                        }, divider = false)
                    }
                }
            }

            // ── Transaction types ─────────────────────────────────────────────
            Card {
                TypeTiles(
                    selected = kinds,
                    // At least one kind stays selected.
                    onToggle = { k -> if (kinds.size > 1 || k !in kinds) kinds = kinds.flip(k) },
                    enabled = !useFilter,
                )
            }

            // ── Accounts ──────────────────────────────────────────────────────
            if (live.size > 1) {
                Card(padding = 0.dp) {
                    ExportRow(
                        icon = Icons.Filled.AccountBalanceWallet,
                        overline = str(R.string.accounts),
                        title = accountSummary,
                        enabled = !useFilter,
                        onClick = { accountsOpen = !accountsOpen },
                    ) {
                        Chevron(accountsOpen)
                    }
                    AnimatedVisibility(accountsOpen && !useFilter) {
                        Column {
                            Hairline()
                            OptionRow(str(R.string.all_accounts), selected = accountIds.isEmpty()) { accountIds = emptySet() }
                            live.forEach { a ->
                                Hairline()
                                OptionRow(a.name, selected = a.id in accountIds) {
                                    val next = accountIds.flip(a.id)
                                    // Picking every account is the same as "All accounts".
                                    accountIds = if (next.size == live.size) emptySet() else next
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Summary ───────────────────────────────────────────────────────────
        Card {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(str(R.string.transactions, list.size), fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(label, color = Muted, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("+ ${formatPaise(earned(list))}", color = Success, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("− ${formatPaise(spent(list))}", color = Danger, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
        if (list.isEmpty()) Hint(str(R.string.nothing_to_export))

        // ── Result ────────────────────────────────────────────────────────────
        note?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        saved?.let { (uri, mime) ->
            Card {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Success)
                    Text(str(R.string.file_saved), fontWeight = FontWeight.ExtraBold, color = Ink)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        SecondaryButton(str(R.string.open), {
                            val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            runCatching { ctx.startActivity(i) }.onFailure { note = str(R.string.no_app_can_open) }
                        })
                    }
                    Box(Modifier.weight(1f)) {
                        SecondaryButton(str(R.string.share), {
                            val i =
                                Intent(Intent.ACTION_SEND)
                                    .setType(mime)
                                    .putExtra(Intent.EXTRA_STREAM, uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            ctx.startActivity(Intent.createChooser(i, str(R.string.share)))
                        })
                    }
                }
            }
        }
    }
}

/** A settings-style row: tinted icon badge, optional small label above the title, and a trailing slot. */
@Composable
private fun ExportRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            if (overline != null) Text(overline, color = Muted, fontSize = 12.sp)
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        trailing()
    }
}

/** One selectable line inside an expanded list, indented to line up with the row titles. */
@Composable
private fun OptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 58.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            Modifier.weight(1f),
            color = Ink,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = str(R.string.selected_2),
                tint = Primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun Chevron(open: Boolean) {
    Icon(
        if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
        contentDescription = if (open) str(R.string.collapse) else str(R.string.expand),
        tint = Muted,
    )
}

@Composable
private fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
