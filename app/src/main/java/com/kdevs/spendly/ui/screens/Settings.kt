package com.kdevs.spendly.ui.screens

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
                        Repo.email.ifBlank { str(R.string.guest_account) },
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
                str(R.string.income_categories),
                icon = Icons.Filled.Savings,
                iconColor = Success,
                onClick = { nav.navigate(Routes.categoryPicker("income", "manage")) },
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
                str(R.string.templates),
                icon = Icons.Filled.ContentCopy,
                iconColor = clr(R.color.swatch_teal),
                onClick = { nav.navigate(Routes.TEMPLATES) },
            )
            SettingRow(
                str(R.string.recurring),
                icon = Icons.Filled.Repeat,
                iconColor = clr(R.color.swatch_pink),
                onClick = { nav.navigate(Routes.RECURRING) },
                divider = false,
            )
        }
        SectionTitle(str(R.string.chat))
        Card(padding = 12.dp) {
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

/** Last step before everything is removed: confirms it is really the account owner, then deletes. */
@Composable
fun DeleteAccountScreen(nav: NavController) {
    var password by remember { mutableStateOf("") }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    ScreenScaffold(str(R.string.delete_my_data), { if (!saver.busy) nav.popBackStack() }) {
        Hint(str(R.string.this_removes_your_accounts_transaction))
        when {
            Repo.usesPassword -> {
                Hint(str(R.string.enter_your_password_to_confirm_it))
                androidx.compose.material3.OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(str(R.string.password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors =
                        androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            unfocusedBorderColor = FieldBorder,
                        ),
                )
            }

            !Repo.isGuest -> Hint(str(R.string.google_will_ask_you_to_pick))
        }
        saver.error?.let {
            Text(
                it,
                color = Danger,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        if (saver.busy) Hint(str(R.string.deleting_keep_the_app_open_and), center = true)
        SecondaryButton(str(R.string.delete_everything_2), {
            saver.run(scope, onDone = {}) {
                Repo.reauthenticate(password.ifEmpty { null }, ctx)
                Repo.deleteEverything()
            }
        }, danger = true)
    }
}

@Composable
fun EditNameScreen(nav: NavController) {
    val profile by Repo.profile.collectAsState()
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    ScreenScaffold(str(R.string.your_name), { nav.popBackStack() }, bottomBar = {
        SavedBar(
            str(R.string.save),
            saver,
            name.isNotBlank() && name.trim() != profile.name,
            onSave = {
                saver.run(
                    scope,
                    onDone = { nav.popBackStack() },
                ) { Repo.updateProfileName(name.trim()) }
            },
        )
    }) {
        Hint(str(R.string.this_is_the_name_your_friends))
        AppTextField(name, { name = it }, str(R.string.display_name))
    }
}
