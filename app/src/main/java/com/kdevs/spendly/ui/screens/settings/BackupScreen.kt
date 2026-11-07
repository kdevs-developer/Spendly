package com.kdevs.spendly.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Backup
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Settings > Backup and restore: save everything to one file, put it back, or bring in rows from a CSV file. */
@Composable
fun BackupScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }

    fun report(
        text: String,
        error: Boolean = false,
    ) {
        message = text
        failed = error
        busy = false
    }

    val savePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                runCatching {
                    val json = Backup.toJson()
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) } ?: error("Could not open the file.")
                    }
                }.onSuccess { report(str(R.string.backup_saved)) }
                    .onFailure { report(it.message ?: str(R.string.something_went_wrong), true) }
            }
        }

    fun readText(uri: android.net.Uri): String =
        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("Could not open the file.")

    val restorePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                runCatching {
                    val text = withContext(Dispatchers.IO) { readText(uri) }
                    Backup.restore(text)
                }.onSuccess { report(str(R.string.backup_restored, it)) }
                    .onFailure { report(it.message ?: str(R.string.something_went_wrong), true) }
            }
        }

    val csvPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                runCatching {
                    val text = withContext(Dispatchers.IO) { readText(uri) }
                    Backup.importCsv(text)
                }.onSuccess { report(str(R.string.csv_imported, it.imported, it.skipped, it.newAccounts, it.newCategories)) }
                    .onFailure { report(it.message ?: str(R.string.something_went_wrong), true) }
            }
        }

    ScreenScaffold(str(R.string.backup_and_restore), { nav.popBackStack() }, busy = busy) {
        Hint(str(R.string.backup_hint))
        Card(padding = 12.dp) {
            SettingRow(
                str(R.string.save_backup),
                str(R.string.save_backup_sub),
                icon = Icons.Filled.CloudDownload,
                iconColor = clr(R.color.swatch_blue),
                onClick = { savePicker.launch("spendly-backup-${LocalDate.now()}.json") },
            )
            SettingRow(
                str(R.string.restore_backup),
                str(R.string.restore_backup_sub),
                icon = Icons.Filled.Restore,
                iconColor = clr(R.color.swatch_green),
                onClick = { restorePicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                divider = false,
            )
        }
        SectionTitle(str(R.string.import_from_csv))
        Card(padding = 12.dp) {
            SettingRow(
                str(R.string.choose_csv_file),
                str(R.string.csv_columns_hint),
                icon = Icons.Filled.UploadFile,
                iconColor = clr(R.color.swatch_orange),
                onClick = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*")) },
                divider = false,
            )
        }
        message?.let { Text(it, color = if (failed) Danger else Success, fontWeight = FontWeight.SemiBold) }
    }
}
