package com.kdevs.spendly.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Biometric
import com.kdevs.spendly.data.Inbox
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.ThemeMode
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.findFragmentActivity
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

@Composable
fun SettingsScreen(nav: NavController) {
    val profile by Repo.profile.collectAsState()
    val uid by Repo.uidFlow.collectAsState()
    var confirmOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var bioNote by remember { mutableStateOf<String?>(null) }

    // Turning the lock on or off asks for the fingerprint / face / screen lock first.
    fun toggleBiometric(want: Boolean) {
        bioNote = null
        val activity = ctx.findFragmentActivity() ?: return
        if (!Biometric.available(ctx)) {
            if (want) {
                bioNote = str(R.string.set_up_a_fingerprint_face_or)
            } else {
                Prefs.updateBiometric(false)
            }
            return
        }
        Biometric.prompt(
            activity,
            if (want) str(R.string.turn_on_biometric_lock) else str(R.string.turn_off_biometric_lock),
            str(R.string.confirm_it_is_you),
        ) { ok ->
            if (ok) Prefs.updateBiometric(want)
        }
    }

    ScreenScaffold(str(R.string.settings), { nav.popBackStack() }) {
        Card {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Avatar(profile.name.ifBlank { "S" }, "blue", 56.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.name.ifBlank { str(R.string.spendly_user) },
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                        fontSize = 18.sp,
                    )
                    Text(
                        Repo.email,
                        color = Muted,
                        fontSize = 13.sp,
                    )
                    if (profile.code.isNotBlank()) {
                        Text(
                            str(R.string.friend_code_2, profile.code),
                            color = Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        SectionTitle(str(R.string.appearance))
        Card {
            SegmentedControl(
                ThemeMode.values().map { it.label },
                Prefs.themeMode.ordinal,
                { Prefs.setTheme(ThemeMode.values()[it]) },
            )
            Spacer(Modifier.height(8.dp))
            Hint(str(R.string.system_follows_your_phone_light_and))
        }
        SectionTitle(str(R.string.security))
        Card(padding = 12.dp) {
            SettingRow(
                str(R.string.fingerprint_face_or_screen_lock),
                str(R.string.biometric_lock),
                icon = Icons.Filled.Fingerprint,
                iconColor = Success,
                trailing = {
                    Switch(
                        checked = Prefs.biometric,
                        onCheckedChange = { toggleBiometric(it) },
                    )
                },
                divider = false,
            )
        }
        bioNote?.let {
            Text(
                it,
                color = Danger,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        SectionTitle(str(R.string.your_space))
        Card(padding = 12.dp) {
            SettingRow(
                str(R.string.your_name),
                profile.name,
                icon = Icons.Filled.Person,
                iconColor = Primary,
                onClick = { nav.navigate(Routes.EDIT_NAME) },
            )
            SettingRow(
                str(R.string.categories),
                icon = Icons.Filled.Category,
                iconColor = clr(R.color.swatch_purple),
                onClick = { nav.navigate(Routes.categoryPicker("expense", "manage")) },
            )
            SettingRow(
                str(R.string.budgets),
                icon = Icons.Filled.DonutLarge,
                iconColor = clr(R.color.swatch_teal),
                onClick = { nav.navigate(Routes.BUDGETS) },
            )
            SettingRow(
                str(R.string.savings_goals),
                icon = Icons.Filled.Flag,
                iconColor = clr(R.color.swatch_orange),
                onClick = { nav.navigate(Routes.GOALS) },
            )
            SettingRow(
                str(R.string.recurring),
                icon = Icons.Filled.Repeat,
                iconColor = clr(R.color.swatch_pink),
                onClick = { nav.navigate(Routes.RECURRING) },
            )
            SettingRow(
                str(R.string.export_data),
                str(R.string.excel_or_pdf),
                icon = Icons.Filled.FileDownload,
                iconColor = Primary,
                onClick = { nav.navigate(Routes.EXPORT) },
                divider = false,
            )
        }
        SectionTitle(str(R.string.chat))
        Card(padding = 12.dp) {
            val allowed = Inbox.notificationsAllowed(ctx)
            SettingRow(
                str(R.string.message_notifications),
                if (allowed) str(R.string.on) else str(R.string.off_tap_to_turn_on),
                icon = Icons.Filled.Notifications,
                iconColor = if (allowed) Success else Danger,
                onClick = {
                    val i =
                        android.content
                            .Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    runCatching { ctx.startActivity(i) }
                },
            )
            SettingRow(
                str(R.string.friends_and_requests),
                icon = Icons.Filled.People,
                iconColor = Primary,
                onClick = { nav.navigate(Routes.REQUESTS) },
            )
            SettingRow(
                str(R.string.blocked_people),
                icon = Icons.Filled.Block,
                iconColor = Danger,
                onClick = { nav.navigate(Routes.BLOCKED) },
                divider = false,
            )
        }
        SectionTitle(str(R.string.about))
        Card(padding = 12.dp) {
            SettingRow(
                str(R.string.privacy_policy),
                icon = Icons.Filled.PrivacyTip,
                iconColor = Muted,
                onClick = { nav.navigate(Routes.PRIVACY) },
            )
            SettingRow(
                str(R.string.terms_of_service),
                icon = Icons.Filled.Description,
                iconColor = Muted,
                onClick = { nav.navigate(Routes.TERMS) },
            )
            SettingRow(
                str(R.string.version),
                "1.0",
                icon = Icons.Filled.Info,
                iconColor = Muted,
                divider = false,
            )
        }
        saver.error?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold) }
        SecondaryButton(str(R.string.sign_out), { confirmOut = true })
        SecondaryButton(str(R.string.delete_all_my_data), { confirmDelete = true }, danger = true)
        Hint(str(R.string.deleting_removes_every_account_transac), center = true)
    }
    if (confirmOut) {
        ConfirmDialog(
            str(R.string.sign_out_2),
            str(R.string.your_data_stays_safe_in_your),
            str(R.string.sign_out),
            { Repo.signOut() },
            { confirmOut = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            str(R.string.delete_everything),
            str(R.string.all_your_data_and_your_login),
            str(R.string.spendly_continue),
            {
                confirmDelete = false
                nav.navigate(Routes.DELETE_ACCOUNT)
            },
            { confirmDelete = false },
            danger = true,
        )
    }
}
