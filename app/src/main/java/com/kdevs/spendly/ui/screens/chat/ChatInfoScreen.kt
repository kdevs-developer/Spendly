package com.kdevs.spendly.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.money.SavedBar
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Group info, report

@Composable
fun ChatInfoScreen(
    nav: NavController,
    id: String,
) {
    val me = Repo.uid
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    val profile by Repo.profile.collectAsState()
    var rename by remember(chat?.name) { mutableStateOf(chat?.name ?: "") }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val c = chat
    ScreenScaffold(if (c?.type == "direct") str(R.string.contact_info) else str(R.string.group_info), { nav.popBackStack() }) {
        if (c == null) return@ScreenScaffold
        val direct = c.type == "direct"
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (direct) {
                Avatar(
                    c.title(me),
                    colorKeyFor(c.otherId(me)),
                    88.dp,
                )
            } else {
                IconCircle(if (c.type == "split") "wallet" else "kids", if (c.type == "split") "teal" else "purple", 88.dp)
            }
            Text(c.title(me), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink)
            if (!direct) {
                Hint(
                    str(
                        R.string.members_count_kind,
                        c.members.size,
                        if (c.type == "split") str(R.string.expense_split_group) else str(R.string.chat_group),
                    ),
                )
            }
        }
        if (!direct) {
            AppTextField(rename, { rename = it }, str(R.string.group_name))
            if (rename.trim() != c.name && rename.isNotBlank()) {
                PrimaryButton(str(R.string.save_name), {
                    saver.run(scope, {}) { Repo.renameChat(id, rename.trim()) }
                }, busy = saver.busy)
            }
        }
        run {
            Card(padding = 12.dp) {
                SettingRow(
                    str(
                        R.string.split_ledger,
                    ),
                    str(R.string.balances_and_expenses),
                    icon = Icons.Filled.Receipt,
                    iconColor = clr(R.color.swatch_teal),
                    onClick = {
                        nav.navigate(Routes.splits(id))
                    },
                    divider = false,
                )
            }
        }
        SectionTitle(str(R.string.members))
        Card(padding = 12.dp) {
            c.members.forEachIndexed { i, uid ->
                val n = c.memberNames[uid] ?: str(R.string.member)
                SettingRow(
                    if (uid == me) str(R.string.name_you, n) else n,
                    if (uid == c.createdBy) str(R.string.admin) else "",
                    leading = { Avatar(n, colorKeyFor(uid)) },
                    onClick = if (uid != me && !direct) ({ nav.navigate(Routes.reportUser(id, uid)) }) else null,
                    trailing =
                        if (uid != me && !direct) {
                            (
                                {
                                    Text(str(R.string.report), color = Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            )
                        } else {
                            null
                        },
                    divider = i < c.members.lastIndex || !direct,
                )
            }
            if (!direct) {
                SettingRow(str(R.string.add_members_2), icon = Icons.Filled.PersonAdd, iconColor = Primary, onClick = {
                    nav.navigate(Routes.addMembers(id))
                }, divider = false)
            }
        }
        if (direct) {
            val other = c.otherId(me)
            Card(padding = 12.dp) {
                SettingRow(
                    if (other in profile.blocked) str(R.string.unblock) else str(R.string.block),
                    icon = Icons.Filled.Block,
                    iconColor = Danger,
                    onClick = { if (other in profile.blocked) saver.run(scope, {}) { Repo.unblock(other) } else confirmBlock = true },
                )
                SettingRow(str(R.string.report), icon = Icons.Filled.Flag, iconColor = Danger, onClick = {
                    nav.navigate(Routes.reportUser(id, other))
                }, divider = false)
            }
        } else {
            SecondaryButton(str(R.string.leave_group), { confirmLeave = true }, danger = true)
        }
        saver.error?.let { Text(it, color = Danger) }
    }
    if (confirmLeave) {
        ConfirmDialog(str(R.string.leave_this_group), str(R.string.you_will_no_longer_get_its), str(R.string.leave), {
            saver.run(scope, onDone = { nav.popBackStack(Routes.CHATS, false) }) { Repo.leaveChat(id) }
        }, { confirmLeave = false }, danger = true)
    }
    if (confirmBlock) {
        ConfirmDialog(str(R.string.block_this_person), str(R.string.you_will_stop_seeing_their_messages), str(R.string.block), {
            saver.run(scope, onDone = {}) { chat?.otherId(me)?.let { Repo.block(it) } }
        }, { confirmBlock = false }, danger = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportUserScreen(
    nav: NavController,
    chatId: String,
    uid: String,
) {
    var reason by remember { mutableStateOf(str(R.string.spam)) }
    var details by remember { mutableStateOf("") }
    var alsoBlock by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    ScreenScaffold(str(R.string.report), { nav.popBackStack() }, bottomBar = {
        SavedBar(if (done) str(R.string.done) else str(R.string.send_report), saver, true, onSave = {
            if (done) {
                nav.popBackStack()
                return@SavedBar
            }
            saver.run(scope, onDone = { done = true }) {
                Repo.report(uid, chatId, if (details.isBlank()) reason else "$reason: ${details.trim()}")
                if (alsoBlock) Repo.block(uid)
            }
        })
    }) {
        if (done) {
            EmptyState(Icons.Filled.CheckCircle, str(R.string.report_sent), str(R.string.thank_you_we_will_look_into))
            return@ScreenScaffold
        }
        Hint(str(R.string.tell_us_what_is_wrong_the))
        Card(padding = 8.dp) {
            listOf(
                str(R.string.spam),
                str(R.string.harassment),
                str(R.string.scam),
                str(R.string.inappropriate),
                str(R.string.other),
            ).forEach {
                RadioRow(it, reason == it) { reason = it }
            }
        }
        AppTextField(details, { details = it }, str(R.string.more_detail_optional), singleLine = false, minLines = 3)
        Card(padding = 12.dp) {
            SettingRow(str(R.string.also_block_this_person), icon = Icons.Filled.Block, iconColor = Danger, trailing = {
                androidx.compose.material3.Switch(alsoBlock, { alsoBlock = it })
            }, divider = false)
        }
    }
}
