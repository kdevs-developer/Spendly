package com.kdevs.spendly.ui.screens.chat

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AddFriendScreen(nav: NavController) {
    val profile by Repo.profile.collectAsState()
    val outgoing by Repo.outgoingRequests.collectAsState()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()

    // An account whose code is missing (cut-short sign up) gets one now instead of showing nothing.
    val codeProblem by Repo.codeProblem.collectAsState()
    // Every visit re-checks that the code is registered for lookup. A failure is shown below, not hidden.
    LaunchedEffect(Unit) { runCatching { Repo.ensureProfile() } }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    fun send(raw: String) {
        msg = null
        saver.run(scope, onDone = {}) {
            val r = Repo.sendFriendRequest(raw)
            ok = r == null
            msg = r ?: str(R.string.request_sent_they_will_see_it)
            if (ok) code = ""
        }
    }

    // Google's scanner opens its own camera screen, so the app needs no camera permission.
    fun scan() {
        msg = null
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(ctx, options).startScan()
            .addOnSuccessListener { result ->
                val found = parseFriendCode(result.rawValue)
                if (found == null) {
                    ok = false
                    msg = str(R.string.that_qr_code_is_not_a)
                } else {
                    code = found
                    send(found)
                }
            }
            .addOnFailureListener {
                ok = false
                msg = str(R.string.could_not_open_the_scanner_type)
            }
    }

    ScreenScaffold(str(R.string.add_friend), { nav.popBackStack() }) {
        HeroCard {
            Text(str(R.string.your_friend_code), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            if (profile.code.isBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Text(str(R.string.getting_your_code), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(profile.code, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(10.dp))
            if (profile.code.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroPill(Icons.Filled.Share, str(R.string.share)) {
                        val i =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, str(R.string.share_code_message, profile.code))
                            }
                        ctx.startActivity(Intent.createChooser(i, str(R.string.share_code)))
                    }
                    HeroPill(
                        if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        if (copied) str(R.string.copied) else str(R.string.copy),
                    ) {
                        clipboard.setText(AnnotatedString(profile.code))
                        copied = true
                    }
                }
            }
        }

        if (profile.code.isNotBlank()) {
            val qr = remember(profile.code) { runCatching { qrImage(friendQrPayload(profile.code)) }.getOrNull() }
            if (qr != null) {
                Card {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        // Always black on white so any phone can scan it, even in the dark theme.
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp)) {
                            Image(qr, str(R.string.your_friend_qr_code), Modifier.size(200.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Hint(str(R.string.let_a_friend_scan_this_to), center = true)
                    }
                }
            }
        }

        codeProblem?.let {
            Text(str(R.string.your_code_is_not_registered_yet, it), color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            SecondaryButton(str(R.string.try_again), { saver.run(scope, {}) { Repo.ensureProfile() } })
        }
        SecondaryButton(str(R.string.scan_a_friend_s_qr_code), { scan() })
        SectionTitle(str(R.string.or_enter_a_friend_s_code))
        AppTextField(code, {
            code =
                it.uppercase().filter {
                        c ->
                    c.isLetterOrDigit() || c == ' '
                }.take(9)
        }, str(R.string.text_8_letters_and_numbers))
        msg?.let { Text(it, color = if (ok) Success else Danger, fontWeight = FontWeight.SemiBold) }
        PrimaryButton(
            str(R.string.send_request),
            { send(code) },
            enabled = code.filter { it.isLetterOrDigit() }.length == 8,
            busy = saver.busy,
        )
        if (outgoing.isNotEmpty()) {
            SectionTitle(str(R.string.waiting_for_a_reply))
            Card(padding = 12.dp) {
                outgoing.forEachIndexed {
                        i,
                        r,
                    ->
                    SettingRow(
                        r.toName.ifBlank { str(R.string.request, i + 1) },
                        str(R.string.pending),
                        icon = Icons.Filled.Schedule,
                        iconColor = Faint,
                        divider = i < outgoing.lastIndex,
                    )
                }
            }
        }
    }
}
