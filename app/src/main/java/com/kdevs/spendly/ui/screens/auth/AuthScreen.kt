package com.kdevs.spendly.ui.screens.auth

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

@Composable
fun AuthScreen(nav: NavController) {
    var mode by remember { mutableIntStateOf(0) } // 0 sign in, 1 create account
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    fun validate(): String? =
        when {
            mode == 1 && name.isBlank() -> str(R.string.enter_your_name)
            !email.contains("@") || !email.contains(".") -> str(R.string.enter_a_valid_email)
            pass.length < 6 -> str(R.string.password_needs_at_least_6_characters)
            else -> null
        }

    ScreenScaffold(
        if (mode ==
            0
        ) {
            str(R.string.welcome_back)
        } else {
            str(R.string.create_account)
        },
        onBack = { nav.popBackStack() },
        busy = saver.busy,
        busyText = str(R.string.signing_you_in),
    ) {
        Spacer(Modifier.height(4.dp))
        SegmentedControl(listOf(str(R.string.sign_in), str(R.string.create_account)), mode, {
            mode = it
            note = null
            saver.error = null
        })
        if (mode == 1) AppTextField(name, { name = it }, str(R.string.your_name))
        AppTextField(email, { email = it }, str(R.string.email), keyboard = KeyboardType.Email)
        var show by remember { mutableStateOf(false) }
        androidx.compose.material3.OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text(str(R.string.password)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(16.dp),
            visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    str(
                        R.string.show_password,
                    ),
                    Modifier
                        .clip(CircleShape)
                        .clickable {
                            show = !show
                        }.padding(6.dp),
                )
            },
            colors =
                androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    unfocusedBorderColor = FieldBorder,
                ),
        )
        (note ?: saver.error)?.let { Text(it, color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        PrimaryButton(if (mode == 0) str(R.string.sign_in) else str(R.string.create_account), {
            val e = validate()
            if (e != null) {
                note = e
                return@PrimaryButton
            }
            note = null
            saver.run(scope, onDone = {}) { if (mode == 0) Repo.signIn(email, pass) else Repo.signUp(name, email, pass) }
        }, busy = saver.busy)
        if (mode == 0) {
            Text(
                str(R.string.forgot_password),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .clickable {
                            // Clear the old message first, otherwise it would hide the real result of this try.
                            note = null
                            if (!email.contains("@") || !email.contains(".")) {
                                note = str(R.string.enter_your_email_above_first)
                            } else {
                                saver.run(scope, onDone = {
                                    note = str(R.string.if_has_an_account_a_reset, email.trim())
                                }) { Repo.resetPassword(email) }
                            }
                        }.padding(10.dp),
            )
        }
        OrDivider()
        GoogleButton(str(R.string.continue_with_google), {
            note = null
            saver.run(scope, onDone = {}) { Repo.signInWithGoogle(ctx) }
        }, enabled = !saver.busy)
    }
}
