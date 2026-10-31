package com.kdevs.spendly.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.money.SavedBar
import com.kdevs.spendly.ui.theme.*

/** Last step before everything is removed: confirms it is really the account owner, then deletes. */
@Composable
fun DeleteAccountScreen(nav: NavController) {
    var password by remember { mutableStateOf("") }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    ScreenScaffold(str(R.string.delete_my_data), {
        if (!saver.busy) nav.popBackStack()
    }, busy = saver.busy, busyText = str(R.string.deleting_your_data)) {
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
                    shape =
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(16.dp),
                    visualTransformation =
                        androidx.compose.ui.text.input
                            .PasswordVisualTransformation(),
                    colors =
                        androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            unfocusedBorderColor = FieldBorder,
                        ),
                )
            }

            else -> Hint(str(R.string.google_will_ask_you_to_pick))
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
