package com.kdevs.spendly.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.kdevs.spendly.ui.screens.common.TickBox
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Chat list and friends

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(nav: NavController) {
    val allChats by Repo.chats.collectAsState()
    val hidden by Repo.hiddenChats.collectAsState()
    val incoming by Repo.incomingRequests.collectAsState()
    val me = Repo.uid
    val loadedSet = Repo.loaded.collectAsState().value
    val chatsLoaded = "chats" in loadedSet
    val ready = chatsLoaded && "hiddenChats" in loadedSet
    // A chat you deleted stays out of the list until someone sends a newer message.
    val chats =
        if (!ready) {
            emptyList()
        } else {
            allChats.filter { c ->
                val at = hidden[c.id]
                at == null || (c.lastAt?.toDate()?.time ?: Long.MAX_VALUE) > at
            }
        }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    val selecting = selected.isNotEmpty()
    // Chats that disappear (deleted, left) drop out of the selection.
    LaunchedEffect(chats) {
        if (selected.isNotEmpty()) selected = selected.filter { id -> chats.any { it.id == id } }.toSet()
    }
    BackHandler(enabled = selecting) { selected = emptySet() }
    TabScreen(
        if (selecting) str(R.string.selected, selected.size) else str(R.string.chat),
        Routes.CHATS,
        nav,
        actions = {
            if (selecting) {
                RoundIconButton(Icons.Filled.Close, str(R.string.cancel), { selected = emptySet() })
                Spacer(Modifier.width(8.dp))
                RoundIconButton(Icons.Filled.Delete, str(R.string.delete), { confirm = true }, tint = Danger)
            } else {
                Box {
                    RoundIconButton(Icons.Filled.PersonAdd, str(R.string.friend_requests), { nav.navigate(Routes.REQUESTS) })
                    if (incoming.isNotEmpty()) {
                        Box(
                            Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(Danger),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(str(R.string.text__2, incoming.size), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        },
        fab = { if (!selecting) Fab(Icons.Filled.Edit, str(R.string.new_chat)) { nav.navigate(Routes.NEW_CHAT) } },
    ) {
        saver.error?.let { Text(it, color = Danger) }
        if (chats.isEmpty() && !ready) {
            InlineLoader()
        } else if (chats.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Filled.Chat, str(R.string.no_chats_yet), str(R.string.add_a_friend_with_their_code))
        } else {
            Card(padding = 12.dp) {
                chats.forEachIndexed { i, c ->
                    val title = c.title(me)
                    val isPicked = c.id in selected
                    val rowShape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(rowShape).background(if (isPicked) PrimaryContainer else Color.Transparent)
                            .combinedClickable(
                                onClick = {
                                    if (selecting) {
                                        selected = if (isPicked) selected - c.id else selected + c.id
                                    } else {
                                        nav.navigate(Routes.chat(c.id))
                                    }
                                },
                                onLongClick = { selected = if (isPicked) selected - c.id else selected + c.id },
                            )
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selecting) TickBox(isPicked)
                        if (c.type == "direct") {
                            Avatar(title, colorKeyFor(c.otherId(me)), 48.dp)
                        } else {
                            IconCircle(if (c.type == "split") "wallet" else "kids", if (c.type == "split") "teal" else "purple", 48.dp)
                        }
                        val unread = (c.unread[me] ?: 0L).toInt()
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1)
                            Text(
                                c.lastMessage.ifBlank {
                                    str(R.string.no_messages_yet)
                                },
                                color = if (unread > 0) Ink else Muted,
                                fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            c.lastAt?.let {
                                Text(
                                    timeOf(it.toDate().time),
                                    color = if (unread > 0) Primary else Muted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (unread > 0) {
                                Box(
                                    Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp).clip(CircleShape).background(Primary)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(if (unread > 99) "99+" else "$unread", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                    if (i < chats.lastIndex) HorizontalDivider(color = Line)
                }
            }
            if (!selecting) Hint(str(R.string.hold_a_chat_to_delete_it), center = true)
        }
        Spacer(Modifier.height(70.dp))
    }
    val picked = chats.filter { it.id in selected }
    if (confirm && picked.isNotEmpty()) {
        val one = picked.singleOrNull()
        val oneDirect = one?.type == "direct"
        ConfirmDialog(
            when {
                one == null -> str(R.string.delete_n_chats, picked.size)
                oneDirect -> str(R.string.delete_chat)
                else -> str(R.string.leave_this_group)
            },
            when {
                one == null -> str(R.string.delete_chats_text)
                oneDirect -> str(R.string.delete_chat_text, one.title(me))
                else -> str(R.string.you_will_no_longer_get_its)
            },
            if (picked.none { it.type == "direct" }) str(R.string.leave) else str(R.string.delete),
            {
                confirm = false
                saver.run(scope, onDone = { selected = emptySet() }) {
                    picked.forEach { if (it.type == "direct") Repo.deleteChatForMe(it.id) else Repo.leaveChat(it.id) }
                }
            },
            { confirm = false },
            danger = true,
        )
    }
}

@Composable
fun NewChatScreen(nav: NavController) {
    val friends by Repo.friends.collectAsState()
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    ScreenScaffold(str(R.string.new_chat), { nav.popBackStack() }) {
        Card(padding = 12.dp) {
            SettingRow(str(R.string.new_group), icon = Icons.Filled.Groups, iconColor = clr(R.color.swatch_purple), onClick = {
                nav.navigate(Routes.CREATE_GROUP)
            })
            SettingRow(str(R.string.add_a_friend), icon = Icons.Filled.PersonAdd, iconColor = Primary, onClick = {
                nav.navigate(Routes.ADD_FRIEND)
            }, divider = false)
        }
        saver.error?.let { Text(it, color = Danger) }
        SectionTitle(str(R.string.friends))
        if (friends.isEmpty()) {
            EmptyState(Icons.Filled.People, str(R.string.no_friends_yet), str(R.string.share_your_friend_code_or_enter))
        } else {
            Card(padding = 12.dp) {
                friends.forEachIndexed { i, f ->
                    SettingRow(f.name, leading = { Avatar(f.name, colorKeyFor(f.id)) }, onClick = {
                        saver.run(scope, onDone = {}) {
                            val id = Repo.openDirectChat(f)
                            nav.navigate(Routes.chat(id)) { popUpTo(Routes.CHATS) }
                        }
                    }, divider = i < friends.lastIndex)
                }
            }
        }
    }
}

@Composable
fun HeroPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.pressable(shape, onClick = onClick).clip(shape).background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
