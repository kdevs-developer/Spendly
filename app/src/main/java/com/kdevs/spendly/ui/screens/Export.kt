package com.kdevs.spendly.ui.screens

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
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.Txn
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
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
        "month" to "This month",
        "lastmonth" to "Last month",
        "last30" to "Last 30 days",
        "year" to "This year",
        "all" to "All time",
        "custom" to "Custom range",
    )

/**
 * Export transactions to Excel (.xlsx) or PDF. The file is saved wherever the person chooses
 * (no storage permission needed).
 *
 * Layout:
 *  - Format toggle
 *  - "Use Activity filter" switch (when on, Period / Types / Accounts are ignored and dimmed)
 *  - Period row (collapsible list, inline From/To when Custom)
 *  - Transaction type chips (multi-select, check icon when selected)
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
    var useFilter by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf("month") }
    var from by remember { mutableLongStateOf(monthStart()) }
    var to by remember { mutableLongStateOf(todayMillis()) }
    var pickingFrom by rememberSaveable { mutableStateOf(true) }
    var kinds by remember { mutableStateOf(setOf("expense", "income", "transfer")) }
    var accountIds by remember { mutableStateOf(setOf<String>()) } // empty = every account
    var periodOpen by remember { mutableStateOf(false) }
    var accountsOpen by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
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
    val periodName = PERIODS.firstOrNull { it.first == period }?.second ?: "This month"
    val periodDates =
        if (period == "all") "Every transaction" else "${fullDate(range.first)} – ${fullDate(range.second - 1)}"
    val label =
        when {
            useFilter -> "Current Activity filter"
            period == "custom" -> "${fullDate(minOf(from, to))} to ${fullDate(maxOf(from, to))}"
            else -> periodName
        }

    val live = accounts.filter { !it.archived }
    val accountSummary =
        when {
            accountIds.isEmpty() -> "All accounts"
            accountIds.size == 1 -> live.firstOrNull { it.id in accountIds }?.name ?: "1 account"
            else -> "${accountIds.size} accounts"
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
        if (uri == null || bytes == null) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not save the file.")
                }
                saved = uri to mime
            } catch (e: Exception) {
                note = e.message ?: "Could not save the file."
            }
        }
    }

    val xlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(XLSX)) { persist(it, XLSX) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF)) { persist(it, PDF) }

    fun <T> Set<T>.flip(v: T) = if (v in this) this - v else this + v

    ScreenScaffold("Export", { nav.popBackStack() }, bottomBar = {
        SavedBar(
            if (format == 0) "Export to Excel" else "Export to PDF",
            saver,
            enabled = list.isNotEmpty(),
            onSave = {
                saved = null
                note = null
                saver.run(scope, onDone = {}) {
                    val bytes =
                        withContext(Dispatchers.IO) {
                            if (format == 0) Exporter.xlsx(list, look, label) else Exporter.pdf(list, look, label)
                        }
                    pending = bytes
                    if (format == 0) xlsxLauncher.launch(Exporter.fileName("xlsx")) else pdfLauncher.launch(Exporter.fileName("pdf"))
                }
            },
        )
    }) {
        SegmentedControl(listOf("Excel (.xlsx)", "PDF"), format, { format = it })
        Hint(
            if (format == 0) {
                "Opens in Excel, Google Sheets and Numbers. One sheet of transactions and one summary sheet."
            } else {
                "A printable report with totals and a table of every transaction."
            },
        )

        // ── Activity filter switch ────────────────────────────────────────────
        Card(padding = 0.dp) {
            ExportRow(
                icon = Icons.Filled.FilterList,
                title = "Use Activity filter",
                subtitle = "Export what you see on the Activity tab",
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
            Hint("No filter is set on the Activity tab, so everything is included.")
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
                    overline = "Period",
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
                        PickRow("From", fullDate(from), Icons.Filled.CalendarMonth, Primary, {
                            if (!useFilter) {
                                pickingFrom = true
                                nav.navigate(Routes.datePicker(from))
                            }
                        })
                        PickRow("To", fullDate(to), Icons.Filled.CalendarMonth, Primary, {
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
                        overline = "Accounts",
                        title = accountSummary,
                        enabled = !useFilter,
                        onClick = { accountsOpen = !accountsOpen },
                    ) {
                        Chevron(accountsOpen)
                    }
                    AnimatedVisibility(accountsOpen && !useFilter) {
                        Column {
                            Hairline()
                            OptionRow("All accounts", selected = accountIds.isEmpty()) { accountIds = emptySet() }
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
                    Text("${list.size} transactions", fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(label, color = Muted, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("+ ${formatPaise(earned(list))}", color = Success, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("− ${formatPaise(spent(list))}", color = Danger, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
        if (list.isEmpty()) Hint("Nothing to export for these choices.")

        // ── Result ────────────────────────────────────────────────────────────
        note?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        saved?.let { (uri, mime) ->
            Card {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Success)
                    Text("File saved", fontWeight = FontWeight.ExtraBold, color = Ink)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        SecondaryButton("Open", {
                            val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            runCatching { ctx.startActivity(i) }.onFailure { note = "No app on this phone can open this file." }
                        })
                    }
                    Box(Modifier.weight(1f)) {
                        SecondaryButton("Share", {
                            val i =
                                Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            ctx.startActivity(Intent.createChooser(i, "Share"))
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
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Chevron(open: Boolean) {
    Icon(
        if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
        contentDescription = if (open) "Collapse" else "Expand",
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