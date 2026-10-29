package com.kdevs.spendly.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

/** Export transactions to Excel (.xlsx) or PDF. The file is saved wherever the person chooses (no storage permission needed). */
@OptIn(ExperimentalLayoutApi::class)
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
    var period by remember { mutableStateOf("month") }
    var from by remember { mutableLongStateOf(monthStart()) }
    var to by remember { mutableLongStateOf(todayMillis()) }
    var pickingFrom by remember { mutableStateOf(true) }
    var kinds by remember { mutableStateOf(setOf("expense", "income", "transfer")) }
    var accountIds by remember { mutableStateOf(setOf<String>()) } // empty = every account
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
    val label =
        when (period) {
            "lastmonth" -> "Last month"
            "last30" -> "Last 30 days"
            "year" -> "This year"
            "all" -> "All time"
            "custom" -> "${fullDate(minOf(from, to))} to ${fullDate(maxOf(from, to))}"
            "filter" -> "Current Activity filter"
            else -> "This month"
        }

    val list: List<Txn> =
        remember(txns, period, from, to, kinds, accountIds, look, TxnFilter.activeCount, TxnFilter.query) {
            txns.filter { t ->
                (period == "filter" || t.dateMillis in range.first until range.second) &&
                        (period != "filter" || TxnFilter.matches(t) { id -> look.categoryName(id) }) &&
                        t.kind in kinds &&
                        (accountIds.isEmpty() || t.accountId in accountIds || t.toAccountId in accountIds)
            }
        }

    val xlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(XLSX)) { uri ->
        val bytes = pending
        pending = null
        if (uri != null && bytes != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not save the file.") }
                    saved = uri to XLSX
                } catch (e: Exception) {
                    note = e.message ?: "Could not save the file."
                }
            }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PDF)) { uri ->
        val bytes = pending
        pending = null
        if (uri != null && bytes != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not save the file.") }
                    saved = uri to PDF
                } catch (e: Exception) {
                    note = e.message ?: "Could not save the file."
                }
            }
        }
    }

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

        SectionTitle("Period")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "month" to "This month",
                "lastmonth" to "Last month",
                "last30" to "Last 30 days",
                "year" to "This year",
                "all" to "All time",
                "custom" to "Custom",
                "filter" to "Activity filter",
            ).forEach { (k, v) -> SelectChip(v, period == k) { period = k } }
        }
        if (period == "custom") {
            Card(padding = 12.dp) {
                PickRow("From", fullDate(from), Icons.Filled.CalendarMonth, Primary, {
                    pickingFrom = true
                    nav.navigate(Routes.datePicker(from))
                })
                PickRow("To", fullDate(to), Icons.Filled.CalendarMonth, Primary, {
                    pickingFrom = false
                    nav.navigate(Routes.datePicker(to))
                }, divider = false)
            }
        }
        if (period == "filter" && TxnFilter.activeCount == 0 && TxnFilter.query.isBlank()) {
            Hint("No filter is set on the Activity tab, so everything is included.")
        }

        SectionTitle("Include")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("expense" to "Expenses", "income" to "Income", "transfer" to "Transfers").forEach { (k, v) ->
                SelectChip(v, k in kinds) { if (kinds.size > 1 || k !in kinds) kinds = kinds.flip(k) }
            }
        }

        val live = accounts.filter { !it.archived }
        if (live.size > 1) {
            SectionTitle("Accounts")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("All", accountIds.isEmpty()) { accountIds = emptySet() }
                live.forEach { a -> SelectChip(a.name, a.id in accountIds) { accountIds = accountIds.flip(a.id) } }
            }
        }

        Card {
            Text("${list.size} transactions", fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(0.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("Income", color = Muted, fontSize = 12.sp)
                    Text(formatPaise(earned(list)), color = Success, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("Expenses", color = Muted, fontSize = 12.sp)
                    Text(formatPaise(spent(list)), color = Danger, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        if (list.isEmpty()) Hint("Nothing to export for these choices.")

        note?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        saved?.let { (uri, mime) ->
            Card {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Icon(Icons.Filled.CheckCircle, null, tint = Success)
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