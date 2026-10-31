package com.kdevs.spendly.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MessageActionsScreen(
    nav: NavController,
    chatId: String,
    msgId: String,
) {
    val me = Repo.uid
    val msgs by remember(chatId) { Repo.messages(chatId) }.collectAsState(initial = emptyList())
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(chatId) { Repo.chat(chatId).collect { chat = it } }
    val m = msgs.firstOrNull { it.id == msgId }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold(str(R.string.message), { nav.popBackStack() }) {
        if (m == null) {
            EmptyState(Icons.Filled.SearchOff, str(R.string.message_gone), str(R.string.it_was_deleted))
            return@ScreenScaffold
        }
        val mine = m.senderId == me
        val left = 5 * 60_000L - (now - m.millis())
        val canDeleteAll = mine && left > 0 && !m.deleted
        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Box(
                Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (mine) Primary else Surface)
                    .padding(14.dp),
            ) {
                Text(
                    if (m.type ==
                        "image"
                    ) {
                        str(R.string.photo)
                    } else if (m.type ==
                        "video"
                    ) {
                        "Video"
                    } else {
                        m.text
                    },
                    color = if (mine) Color.White else Ink,
                    fontSize = 15.sp,
                )
            }
            Hint(timeOf(m.millis()) + " · " + fullDate(m.millis()))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(Surface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            reactionIcons.forEach { (k, icon, tint) ->
                val on = m.reactions[me] == k
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (on) tint.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            scope.launchIO { Repo.react(chatId, m, k) }
                            nav.popBackStack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, k, tint = tint, modifier = Modifier.size(26.dp))
                }
            }
        }
        Card(padding = 12.dp) {
            SettingRow(str(R.string.reply), icon = Icons.AutoMirrored.Filled.Reply, iconColor = Primary, onClick = {
                nav.setPicked("replyTo", m.id)
                nav.popBackStack()
            })
            if (m.type == "text") {
                SettingRow(str(R.string.copy_text), icon = Icons.Filled.ContentCopy, iconColor = Muted, onClick = {
                    clip.setText(AnnotatedString(m.text))
                    nav.popBackStack()
                })
            }
            SettingRow(
                str(R.string.message_info),
                if (mine) timeOf(m.millis()) + " · " + fullDate(m.millis()) else str(R.string.sent_by, m.senderName),
                icon = Icons.Filled.Info,
                iconColor = Muted,
            )
            if (canDeleteAll) {
                SettingRow(
                    str(R.string.delete_for_everyone),
                    str(R.string.d_02d_left).format(left / 60000, (left / 1000) % 60),
                    icon = Icons.Filled.Delete,
                    iconColor = Danger,
                    onClick = { confirm = true },
                )
            }
            SettingRow(str(R.string.delete_for_me), icon = Icons.Filled.DeleteOutline, iconColor = Danger, onClick = {
                scope.launchIO { Repo.deleteForMe(m) }
                nav.popBackStack()
            }, divider = false)
        }
        if (mine) {
            val others = chat?.members.orEmpty().filter { it != me }
            SectionTitle(str(R.string.delivery))
            Card(padding = 12.dp) {
                if (m.pending) {
                    SettingRow(
                        str(R.string.sending),
                        str(R.string.waiting_for_connection),
                        icon = Icons.Filled.Schedule,
                        iconColor = Muted,
                        divider = false,
                    )
                } else {
                    SettingRow(
                        str(R.string.sent),
                        timeOf(m.millis()),
                        icon = Icons.Filled.Done,
                        iconColor = Muted,
                        divider = others.isNotEmpty(),
                    )
                    others.forEachIndexed { i, u ->
                        val n = chat?.memberNames?.get(u) ?: str(R.string.member)
                        val read = u in m.readBy
                        val got = read || u in m.deliveredTo
                        SettingRow(
                            n,
                            if (read) {
                                str(R.string.read)
                            } else if (got) {
                                str(R.string.received)
                            } else {
                                str(R.string.not_received_yet)
                            },
                            leading = { Avatar(n, colorKeyFor(u), 38.dp) },
                            trailing = {
                                Icon(
                                    if (got) Icons.Filled.DoneAll else Icons.Filled.Done,
                                    null,
                                    tint = if (read) clr(R.color.swatch_blue) else Faint,
                                )
                            },
                            divider = i < others.lastIndex,
                        )
                    }
                }
            }
        }
        Hint(str(R.string.you_can_delete_for_everyone_within), center = true)
    }
    if (confirm && m != null) {
        ConfirmDialog(str(R.string.delete_for_everyone_2), str(R.string.everyone_in_the_chat_will_see), str(R.string.delete), {
            scope.launchIO { Repo.deleteForEveryone(chatId, m) }
            nav.popBackStack()
        }, { confirm = false }, danger = true)
    }
}
