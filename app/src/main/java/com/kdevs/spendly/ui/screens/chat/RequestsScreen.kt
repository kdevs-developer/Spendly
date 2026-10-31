package com.kdevs.spendly.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

/** Friends and requests: one screen, three tabs (Friends, Requests, Sent), with a search box and a code card on top. */
@Composable
fun RequestsScreen(nav: NavController) {
    val allIncoming by Repo.incomingRequests.collectAsState()
    val outgoing by Repo.outgoingRequests.collectAsState()
    val myProfile by Repo.profile.collectAsState()
    val incoming = allIncoming.filter { it.from !in myProfile.blocked } // blocked people cannot nudge you
    val friends by Repo.friends.collectAsState()
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var removeFriend by remember { mutableStateOf<Friend?>(null) }
    var blockFriend by remember { mutableStateOf<Friend?>(null) }
    val shown = friends.filter { query.isBlank() || it.name.contains(query, true) }.sortedBy { it.name.lowercase() }

    ScreenScaffold(
        str(R.string.friends_and_requests),
        { nav.popBackStack() },
        header = {
            // Your code, always one tap away.
            HeroCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            str(R.string.your_friend_code),
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        Text(
                            myProfile.code.ifBlank { "..." },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            letterSpacing = 2.sp,
                        )
                    }
                    HeroPill(Icons.Filled.QrCode2, "Share") { nav.navigate(Routes.ADD_FRIEND) }
                }
            }
            SegmentedControl(
                listOf(
                    str(R.string.tab_friends, friends.size),
                    str(R.string.tab_requests, incoming.size),
                    str(R.string.tab_sent, outgoing.size),
                ),
                tab,
                { tab = it },
            )
        },
    ) {
        saver.error?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold) }
        when (tab) {
            0 -> {
                if (friends.size >
                    5
                ) {
                    AppTextField(query, { query = it }, str(R.string.search_friends), leading = { Icon(Icons.Filled.Search, null) })
                }
                if (friends.isEmpty()) {
                    EmptyState(Icons.Filled.People, str(R.string.no_friends_yet), str(R.string.share_code_or_scan))
                    PrimaryButton(str(R.string.add_friend), { nav.navigate(Routes.ADD_FRIEND) })
                } else if (shown.isEmpty()) {
                    Hint(str(R.string.no_friend_matches, query))
                } else {
                    shown.forEach { f ->
                        Card(padding = 14.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Avatar(f.name, colorKeyFor(f.id), 48.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(f.name, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1)
                                    Text(f.code, color = Muted, fontSize = 12.sp, letterSpacing = 1.sp)
                                }
                                RoundIconButton(Icons.AutoMirrored.Filled.Chat, str(R.string.message_person, f.name), {
                                    saver.run(scope, onDone = {}) {
                                        val id = Repo.openDirectChat(f)
                                        nav.navigate(Routes.chat(id))
                                    }
                                }, tint = Color.White, bg = Primary)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FriendAction(Icons.Filled.PersonRemove, str(R.string.remove), Modifier.weight(1f)) { removeFriend = f }
                                FriendAction(Icons.Filled.Block, str(R.string.block), Modifier.weight(1f)) { blockFriend = f }
                            }
                        }
                    }
                }
                Card(padding = 12.dp) {
                    SettingRow(str(R.string.blocked_people), icon = Icons.Filled.Block, iconColor = Danger, onClick = {
                        nav.navigate(Routes.BLOCKED)
                    }, divider = false)
                }
            }
            1 -> {
                if (incoming.isEmpty()) {
                    EmptyState(Icons.Filled.MarkEmailRead, str(R.string.no_new_requests_title), str(R.string.requests_empty_hint))
                }
                incoming.forEach { r ->
                    Card(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(r.fromName, colorKeyFor(r.from), 52.dp)
                            Column(Modifier.weight(1f)) {
                                Text(r.fromName, fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 16.sp)
                                Text(str(R.string.wants_to_be_friends), color = Muted, fontSize = 13.sp)
                                if (r.fromCode.isNotBlank()) Text(r.fromCode, color = Faint, fontSize = 11.sp, letterSpacing = 1.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                SecondaryButton(
                                    str(R.string.decline),
                                    { saver.run(scope, {}) { Repo.answerRequest(r, false) } },
                                    danger = true,
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                PrimaryButton(
                                    str(R.string.accept),
                                    { saver.run(scope, {}) { Repo.answerRequest(r, true) } },
                                    busy = saver.busy,
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                if (outgoing.isEmpty()) {
                    EmptyState(Icons.Filled.Send, str(R.string.nothing_waiting), str(R.string.sent_empty_hint))
                }
                outgoing.forEach { r ->
                    Card(padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Avatar(r.toName.ifBlank { "?" }, colorKeyFor(r.to), 48.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    r.toName.ifBlank { str(R.string.friend) },
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Ink,
                                    maxLines = 1,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Schedule, null, tint = Faint, modifier = Modifier.size(14.dp))
                                    Text(str(R.string.pending), color = Muted, fontSize = 12.sp)
                                }
                            }
                            Text(
                                "Cancel",
                                color = Danger,
                                fontWeight = FontWeight.Bold,
                                modifier =
                                    Modifier
                                        .clip(
                                            CircleShape,
                                        ).clickable { saver.run(scope, {}) { Repo.cancelRequest(r) } }
                                        .padding(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
    removeFriend?.let { f ->
        ConfirmDialog("Remove ${f.name}?", "They will no longer be in your friends list. Your chat history stays.", str(R.string.remove), {
            saver.run(scope, {}) { Repo.removeFriend(f.id) }
        }, { removeFriend = null }, danger = true)
    }
    blockFriend?.let { f ->
        ConfirmDialog(str(R.string.block_this_person), str(R.string.you_will_stop_seeing_their_messages), str(R.string.block), {
            saver.run(scope, {}) {
                Repo.block(f.id)
                Repo.removeFriend(f.id)
            }
        }, { blockFriend = null }, danger = true)
    }
}

@Composable
private fun FriendAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Bg)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Muted, modifier = Modifier.size(16.dp))
        Text(label, color = Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun BlockedScreen(nav: NavController) {
    val profile by Repo.profile.collectAsState()
    val friends by Repo.friends.collectAsState()
    val chats by Repo.chats.collectAsState()
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()

    fun nameOf(id: String) =
        friends.firstOrNull { it.id == id }?.name
            ?: chats.firstNotNullOfOrNull { it.memberNames[id] } ?: str(R.string.user, id.take(6))
    ScreenScaffold(str(R.string.blocked_people), { nav.popBackStack() }) {
        Hint(str(R.string.you_will_not_see_messages_from))
        if (profile.blocked.isEmpty()) {
            EmptyState(Icons.Filled.Block, str(R.string.no_one_blocked), str(R.string.people_you_block_will_show_up))
        } else {
            Card(padding = 12.dp) {
                profile.blocked.forEachIndexed { i, id ->
                    SettingRow(
                        nameOf(id),
                        leading = { Avatar(nameOf(id), colorKeyFor(id)) },
                        divider = i < profile.blocked.lastIndex,
                        trailing = {
                            Text(
                                str(
                                    R.string.unblock,
                                ),
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                modifier =
                                    Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            saver.run(scope, {}) { Repo.unblock(id) }
                                        }.padding(8.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}
