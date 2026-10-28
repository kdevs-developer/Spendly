package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Switch
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
import com.kdevs.spendly.data.Biometric
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.ThemeMode
import com.kdevs.spendly.data.findFragmentActivity
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

@Composable
fun SettingsScreen(nav: NavController) {
    val profile by Repo.profile.collectAsState()
    val uid by Repo.uidFlow.collectAsState()
    var confirmOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var bioNote by remember { mutableStateOf<String?>(null) }

    // Turning the lock on or off asks for the fingerprint / face / screen lock first.
    fun toggleBiometric(want: Boolean) {
        bioNote = null
        val activity = ctx.findFragmentActivity() ?: return
        if (!Biometric.available(ctx)) {
            if (want) bioNote = "Set up a fingerprint, face or screen lock in your phone settings first."
            else Prefs.setBiometric(false)
            return
        }
        Biometric.prompt(activity, if (want) "Turn on biometric lock" else "Turn off biometric lock", "Confirm it is you") { ok ->
            if (ok) Prefs.setBiometric(want)
        }
    }

    ScreenScaffold("Settings", { nav.popBackStack() }) {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(profile.name.ifBlank { "S" }, "blue", 56.dp)
                Column(Modifier.weight(1f)) {
                    Text(profile.name.ifBlank { "Spendly user" }, fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 18.sp)
                    Text(Repo.email.ifBlank { "Guest account" }, color = Muted, fontSize = 13.sp)
                    if (profile.code.isNotBlank()) Text("Friend code ${profile.code}", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        SectionTitle("Appearance")
        Card {
            SegmentedControl(ThemeMode.values().map { it.label }, Prefs.themeMode.ordinal, { Prefs.setTheme(ThemeMode.values()[it]) })
            Spacer(Modifier.height(8.dp))
            Hint("System follows your phone. Light and Dark stay as you choose.")
        }
        SectionTitle("Security")
        Card(padding = 12.dp) {
            SettingRow("Fingerprint, face or screen lock", "Biometric lock", icon = Icons.Filled.Fingerprint, iconColor = Success,
                trailing = { Switch(checked = Prefs.biometric, onCheckedChange = { toggleBiometric(it) }) }, divider = false)
        }
        bioNote?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        SectionTitle("Your space")
        Card(padding = 12.dp) {
            SettingRow("Your name", profile.name, icon = Icons.Filled.Person, iconColor = Primary, onClick = { nav.navigate(Routes.EDIT_NAME) })
            SettingRow("Categories", icon = Icons.Filled.Category, iconColor = Color(0xFF7E22CE), onClick = { nav.navigate(Routes.categoryPicker("expense", "manage")) })
            SettingRow("Income categories", icon = Icons.Filled.Savings, iconColor = Success, onClick = { nav.navigate(Routes.categoryPicker("income", "manage")) })
            SettingRow("Budgets", icon = Icons.Filled.DonutLarge, iconColor = Color(0xFF0F766E), onClick = { nav.navigate(Routes.BUDGETS) })
            SettingRow("Savings goals", icon = Icons.Filled.Flag, iconColor = Color(0xFFC2410C), onClick = { nav.navigate(Routes.GOALS) })
            SettingRow("Templates", icon = Icons.Filled.ContentCopy, iconColor = Color(0xFF0F766E), onClick = { nav.navigate(Routes.TEMPLATES) })
            SettingRow("Recurring", icon = Icons.Filled.Repeat, iconColor = Color(0xFFBE185D), onClick = { nav.navigate(Routes.RECURRING) }, divider = false)
        }
        SectionTitle("Chat")
        Card(padding = 12.dp) {
            SettingRow("Friends and requests", icon = Icons.Filled.People, iconColor = Primary, onClick = { nav.navigate(Routes.REQUESTS) })
            SettingRow("Blocked people", icon = Icons.Filled.Block, iconColor = Danger, onClick = { nav.navigate(Routes.BLOCKED) }, divider = false)
        }
        SectionTitle("About")
        Card(padding = 12.dp) {
            SettingRow("Privacy Policy", icon = Icons.Filled.PrivacyTip, iconColor = Muted, onClick = { nav.navigate(Routes.PRIVACY) })
            SettingRow("Terms of Service", icon = Icons.Filled.Description, iconColor = Muted, onClick = { nav.navigate(Routes.TERMS) })
            SettingRow("Version", "1.0", icon = Icons.Filled.Info, iconColor = Muted, divider = false)
        }
        saver.error?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold) }
        SecondaryButton("Sign out", { confirmOut = true })
        SecondaryButton("Delete all my data", { confirmDelete = true }, danger = true)
        Hint("Deleting removes every account, transaction, chat entry and your login. This cannot be undone.", center = true)
    }
    if (confirmOut) ConfirmDialog("Sign out?", "Your data stays safe in your account.", "Sign out", { Repo.signOut() }, { confirmOut = false })
    if (confirmDelete) ConfirmDialog("Delete everything?", "All your data and your login will be removed for good.", "Delete", {
        saver.run(scope, onDone = {}) { Repo.deleteEverything() }
    }, { confirmDelete = false }, danger = true)
}

@Composable
fun EditNameScreen(nav: NavController) {
    val profile by Repo.profile.collectAsState()
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    ScreenScaffold("Your name", { nav.popBackStack() }, bottomBar = {
        SavedBar("Save", saver, name.isNotBlank() && name.trim() != profile.name, onSave = {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.updateProfileName(name.trim()) }
        })
    }) {
        Hint("This is the name your friends see in chat.")
        AppTextField(name, { name = it }, "Display name")
    }
}
