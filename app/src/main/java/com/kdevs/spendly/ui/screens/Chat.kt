package com.kdevs.spendly.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Chat.title(me: String): String =
    if (type == "direct") {
        memberNames.entries.firstOrNull {
            it.key != me
        }?.value ?: str(R.string.chat)
    } else {
        name.ifBlank { str(R.string.group) }
    }

private fun Chat.otherId(me: String): String = members.firstOrNull { it != me } ?: ""

private fun Message.millis(): Long = createdAt?.toDate()?.time ?: System.currentTimeMillis()

/** Pending = still on this phone. Sent = on the server. Delivered = on every other phone. Read = opened by everyone. */
private enum class Delivery { Pending, Sent, Delivered, Read }

private fun Message.delivery(
    me: String,
    members: List<String>,
): Delivery {
    if (pending) return Delivery.Pending
    val others = members.filter { it != me }
    if (others.isEmpty()) return Delivery.Sent
    if (others.all { it in readBy }) return Delivery.Read
    if (others.all { it in deliveredTo || it in readBy }) return Delivery.Delivered
    return Delivery.Sent
}

/** Reaction keys and their icons. Drawn as icons, not emoji, so they match the app. */
private val reactionIcons =
    listOf(
        Triple("like", Icons.Filled.ThumbUp, clr(R.color.swatch_blue)),
        Triple("love", Icons.Filled.Favorite, clr(R.color.swatch_red)),
        Triple("laugh", Icons.Filled.SentimentVerySatisfied, clr(R.color.swatch_amber)),
        Triple("wow", Icons.Filled.Bolt, clr(R.color.swatch_amber)),
        Triple("sad", Icons.Filled.SentimentDissatisfied, clr(R.color.swatch_amber)),
        Triple("star", Icons.Filled.Star, clr(R.color.swatch_amber)),
    )

private fun reactionIcon(key: String) = reactionIcons.firstOrNull { it.first == key }

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
    var pending by remember { mutableStateOf<Chat?>(null) }
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    TabScreen(
        str(R.string.chat),
        Routes.CHATS,
        nav,
        actions = {
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
        },
        fab = { Fab(Icons.Filled.Edit, str(R.string.new_chat)) { nav.navigate(Routes.NEW_CHAT) } },
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
                    Row(
                        Modifier.fillMaxWidth()
                            .combinedClickable(onClick = { nav.navigate(Routes.chat(c.id)) }, onLongClick = { pending = c })
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
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
            Hint(str(R.string.hold_a_chat_to_delete_it), center = true)
        }
        Spacer(Modifier.height(70.dp))
    }
    pending?.let { c ->
        val isDirect = c.type == "direct"
        ConfirmDialog(
            if (isDirect) str(R.string.delete_chat) else str(R.string.leave_this_group),
            if (isDirect) str(R.string.delete_chat_text, c.title(me)) else str(R.string.you_will_no_longer_get_its),
            if (isDirect) str(R.string.delete) else str(R.string.leave),
            {
                pending = null
                saver.run(scope, onDone = {}) { if (isDirect) Repo.deleteChatForMe(c.id) else Repo.leaveChat(c.id) }
            },
            { pending = null },
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
private fun HeroPill(
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
                        Text(str(R.string.your_friend_code), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                listOf(str(R.string.tab_friends, friends.size), str(R.string.tab_requests, incoming.size), str(R.string.tab_sent, outgoing.size)),
                tab,
                { tab = it },
            )
        },
    ) {
        saver.error?.let { Text(it, color = Danger, fontWeight = FontWeight.SemiBold) }
        when (tab) {
            0 -> {
                if (friends.size > 5) AppTextField(query, { query = it }, str(R.string.search_friends), leading = { Icon(Icons.Filled.Search, null) })
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
                    SettingRow(str(R.string.blocked_people), icon = Icons.Filled.Block, iconColor = Danger, onClick = { nav.navigate(Routes.BLOCKED) }, divider = false)
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
                                SecondaryButton(str(R.string.decline), { saver.run(scope, {}) { Repo.answerRequest(r, false) } }, danger = true)
                            }
                            Box(Modifier.weight(1f)) {
                                PrimaryButton(str(R.string.accept), { saver.run(scope, {}) { Repo.answerRequest(r, true) } }, busy = saver.busy)
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
                                Text(r.toName.ifBlank { str(R.string.friend) }, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Schedule, null, tint = Faint, modifier = Modifier.size(14.dp))
                                    Text(str(R.string.pending), color = Muted, fontSize = 12.sp)
                                }
                            }
                            Text(
                                "Cancel",
                                color = Danger,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).clickable { saver.run(scope, {}) { Repo.cancelRequest(r) } }.padding(10.dp),
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
        modifier.clip(RoundedCornerShape(12.dp)).background(Bg).clickable(onClick = onClick).padding(vertical = 9.dp),
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
                                    Modifier.clip(CircleShape).clickable {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(nav: NavController) {
    val friends by Repo.friends.collectAsState()
    var name by remember { mutableStateOf("") }
    var split by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    ScreenScaffold(str(R.string.new_group), { nav.popBackStack() }, bottomBar = {
        SavedBar(str(R.string.create_group), saver, name.isNotBlank() && picked.isNotEmpty(), onSave = {
            saver.run(scope, onDone = {}) {
                val id = Repo.createGroup(name.trim(), if (split == 1) "split" else "group", friends.filter { it.id in picked })
                nav.navigate(Routes.chat(id)) { popUpTo(Routes.CHATS) }
            }
        })
    }) {
        SegmentedControl(listOf(str(R.string.chat_group), str(R.string.expense_split)), split, { split = it })
        Hint(if (split == 1) str(R.string.chat_plus_a_shared_ledger_add) else str(R.string.group_chat_hint))
        AppTextField(name, { name = it }, str(R.string.group_name))
        SectionTitle(str(R.string.add_members, picked.size))
        if (friends.isEmpty()) Hint(str(R.string.add_friends_first_from_the_chat))
        Card(padding = 12.dp) {
            if (friends.isEmpty()) {
                EmptyState(Icons.Filled.People, str(R.string.no_friends_yet), str(R.string.add_friends_first_from_the_chat))
                PrimaryButton(str(R.string.add_a_friend), { nav.navigate(Routes.ADD_FRIEND) })
            } else {
                friends.forEachIndexed { i, f ->
                    SettingRow(
                        f.name,
                        leading = { Avatar(f.name, colorKeyFor(f.id)) },
                        onClick = { picked = if (f.id in picked) picked - f.id else picked + f.id },
                        trailing = { TickBox(f.id in picked) },
                        divider = i < friends.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
fun AddMembersScreen(
    nav: NavController,
    id: String,
) {
    val friends by Repo.friends.collectAsState()
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    var picked by remember { mutableStateOf(setOf<String>()) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val candidates = friends.filter { f -> chat?.members?.contains(f.id) != true }
    ScreenScaffold(str(R.string.add_members_2), { nav.popBackStack() }, bottomBar = {
        SavedBar(str(R.string.add_people, picked.size), saver, picked.isNotEmpty(), onSave = {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.addMembers(id, candidates.filter { it.id in picked }) }
        })
    }) {
        if (candidates.isEmpty()) {
            EmptyState(Icons.Filled.People, str(R.string.no_one_to_add), str(R.string.all_your_friends_are_already_here))
        } else {
            Card(padding = 12.dp) {
                candidates.forEachIndexed { i, f ->
                    SettingRow(
                        f.name,
                        leading = { Avatar(f.name, colorKeyFor(f.id)) },
                        onClick = { picked = if (f.id in picked) picked - f.id else picked + f.id },
                        trailing = { TickBox(f.id in picked) },
                        divider = i < candidates.lastIndex,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Conversation

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(
    nav: NavController,
    entry: NavBackStackEntry,
    id: String,
) {
    val me = Repo.uid
    var chat by remember { mutableStateOf<Chat?>(null) }
    val msgs by remember(id) { Repo.messages(id) }.collectAsState(initial = emptyList())
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Message?>(null) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val ctx = LocalContext.current

    // No alert for the chat that is already on screen; opening it also clears its notification.
    DisposableEffect(id) {
        Inbox.openChatId = id
        Inbox.clear(ctx, id)
        onDispose { if (Inbox.openChatId == id) Inbox.openChatId = null }
    }
    entry.Picked<String>("replyTo") { mid -> replyTo = msgs.firstOrNull { it.id == mid } }

    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.animateScrollToItem(list.layoutInfo.totalItemsCount - 1) }

    // When the keyboard opens (or grows, e.g. while a reply bar appears) keep the last message in view.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && msgs.isNotEmpty()) list.scrollToItem(list.layoutInfo.totalItemsCount - 1)
    }
    LaunchedEffect(msgs) { runCatching { Repo.markRead(id, msgs) } }

    // Typing: say "typing" while the field changes, and clear it after 3 seconds of quiet.
    LaunchedEffect(text) {
        if (text.isNotBlank()) {
            Repo.setTyping(id, true)
            delay(3000)
            Repo.setTyping(id, false)
        }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            now = System.currentTimeMillis()
        }
    }
    val typingNames =
        chat?.typing?.filter { it.key != me && now - it.value < 5000 }?.keys?.mapNotNull {
            chat?.memberNames?.get(
                it,
            )
        }.orEmpty()

    val c = chat
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, str(R.string.back), { nav.popBackStack() })
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable {
                    nav.navigate(Routes.chatInfo(id))
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val title = c?.title(me) ?: ""
                if (c?.type == "direct") {
                    Avatar(
                        title.ifBlank {
                            "?"
                        },
                        colorKeyFor(
                            c.otherId(me),
                        ),
                        38.dp,
                    )
                } else {
                    IconCircle(if (c?.type == "split") "wallet" else "kids", if (c?.type == "split") "teal" else "purple", 38.dp)
                }
                Column {
                    Text(title, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1)
                    Text(
                        if (typingNames.isNotEmpty()) {
                            str(R.string.typing)
                        } else if (c?.type == "direct") {
                            str(R.string.tap_for_info)
                        } else {
                            str(R.string.members_count, c?.members?.size ?: 0)
                        },
                        color = if (typingNames.isNotEmpty()) Primary else Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            // Splitting a bill works in every chat: one friend, a group, or a split group.
            if (c != null) RoundIconButton(Icons.Filled.Receipt, str(R.string.split_ledger), { nav.navigate(Routes.splits(id)) })
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            state = list,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { Spacer(Modifier.height(6.dp)) }
            items(msgs, key = { it.id }) { m ->
                if (m.type == "system") {
                    Text(
                        m.text,
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        color = Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Bubble(
                        m,
                        mine = m.senderId == me,
                        group = c?.type != "direct",
                        me = me,
                        memberIds = c?.members.orEmpty(),
                        onLong = { nav.navigate(Routes.messageActions(id, m.id)) },
                        onImage = {
                            if (m.type == "video") {
                                runCatching {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW).setDataAndType(android.net.Uri.parse(m.imageUrl), "video/*"),
                                    )
                                }
                            } else {
                                nav.navigate(Routes.image(id, m.id))
                            }
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
        Column(
            Modifier.fillMaxWidth().background(
                Surface,
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ).navigationBarsPadding().imePadding().padding(12.dp),
        ) {
            replyTo?.let { r ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PrimaryContainer).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            str(R.string.replying_to, if (r.senderId == me) str(R.string.yourself) else r.senderName),
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                        Text(
                            if (r.type == "image") str(R.string.photo) else if (r.type == "video") str(R.string.video) else r.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Ink,
                            fontSize = 13.sp,
                        )
                    }
                    Icon(
                        Icons.Filled.Close,
                        str(R.string.cancel_reply),
                        Modifier.clip(CircleShape).clickable { replyTo = null }.padding(4.dp),
                        tint = Muted,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    text,
                    { text = it },
                    Modifier.weight(1f),
                    placeholder = { Text(str(R.string.message)) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors =
                        androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Bg,
                            unfocusedContainerColor = Bg,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                        ),
                )
                RoundIconButton(Icons.AutoMirrored.Filled.Send, str(R.string.send), {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        val r = replyTo
                        text = ""
                        replyTo = null
                        scope.launchIO { Repo.sendText(id, t, r) }
                    }
                }, tint = Color.White, bg = Primary)
            }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchIO(block: suspend () -> Unit) {
    this.launch { runCatching { block() } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(
    m: Message,
    mine: Boolean,
    group: Boolean,
    me: String,
    memberIds: List<String>,
    onLong: () -> Unit,
    onImage: () -> Unit,
) {
    val shape = if (mine) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        if (!mine && group) {
            Text(
                m.senderName,
                color = colorSpec(colorKeyFor(m.senderId)).solid,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Column(
            Modifier.widthIn(max = 290.dp).clip(shape)
                .background(
                    if (m.deleted) {
                        Track
                    } else if (mine) {
                        Brand
                    } else {
                        Surface
                    },
                )
                .combinedClickable(
                    onClick = { if ((m.type == "image" || m.type == "video") && !m.deleted) onImage() },
                    onLongClick = { if (!m.deleted) onLong() },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val ink = if (mine && !m.deleted) Color.White else Ink
            if (m.replyPreview.isNotBlank() && !m.deleted) {
                Text(
                    m.replyPreview,
                    color = ink.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ink.copy(alpha = 0.12f)).padding(6.dp),
                )
            }
            when {
                m.deleted ->
                    Text(
                        str(R.string.this_message_was_deleted),
                        color = Muted,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 14.sp,
                    )
                m.type == "image" ->
                    AsyncImage(
                        m.imageUrl,
                        str(R.string.photo),
                        Modifier.size(220.dp).clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                m.type == "video" ->
                    Box(
                        Modifier.size(220.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, str(R.string.play_video), tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Text(str(R.string.video), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                    }
                else -> Text(m.text, color = ink, fontSize = 15.sp)
            }
            Row(
                Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(timeOf(m.millis()), color = ink.copy(alpha = 0.7f), fontSize = 10.sp)
                if (mine && !m.deleted) {
                    val d = m.delivery(me, memberIds)
                    Icon(
                        when (d) {
                            Delivery.Pending -> Icons.Filled.Schedule
                            Delivery.Sent -> Icons.Filled.Done
                            else -> Icons.Filled.DoneAll
                        },
                        when (d) {
                            Delivery.Pending -> str(R.string.sending)
                            Delivery.Sent -> str(R.string.sent)
                            Delivery.Delivered -> str(R.string.received)
                            Delivery.Read -> str(R.string.seen)
                        },
                        tint = if (d == Delivery.Read) clr(R.color.read_receipt) else ink.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        if (m.reactions.isNotEmpty() && !m.deleted) {
            Row(
                Modifier.padding(
                    horizontal = 6.dp,
                ).offset(y = (-6).dp).clip(CircleShape).background(Surface).padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                m.reactions.values.distinct().forEach {
                        k ->
                    reactionIcon(k)?.let { Icon(it.second, k, tint = it.third, modifier = Modifier.size(14.dp)) }
                }
                if (m.reactions.size > 1) {
                    Text(
                        str(R.string.text__2, m.reactions.size),
                        fontSize = 11.sp,
                        color = Muted,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

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
            Box(Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(20.dp)).background(if (mine) Primary else Surface).padding(14.dp)) {
                Text(if (m.type == "image") str(R.string.photo) else if (m.type == "video") "Video" else m.text, color = if (mine) Color.White else Ink, fontSize = 15.sp)
            }
            Hint(timeOf(m.millis()) + " · " + fullDate(m.millis()))
        }
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(Surface).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            reactionIcons.forEach { (k, icon, tint) ->
                val on = m.reactions[me] == k
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(if (on) tint.copy(alpha = 0.15f) else Color.Transparent)
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
                    SettingRow(str(R.string.sending), str(R.string.waiting_for_connection), icon = Icons.Filled.Schedule, iconColor = Muted, divider = false)
                } else {
                    SettingRow(str(R.string.sent), timeOf(m.millis()), icon = Icons.Filled.Done, iconColor = Muted, divider = others.isNotEmpty())
                    others.forEachIndexed { i, u ->
                        val n = chat?.memberNames?.get(u) ?: str(R.string.member)
                        val read = u in m.readBy
                        val got = read || u in m.deliveredTo
                        SettingRow(
                            n,
                            if (read) str(R.string.read) else if (got) str(R.string.received) else str(R.string.not_received_yet),
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

@Composable
fun ImageViewerScreen(
    nav: NavController,
    chatId: String,
    msgId: String,
) {
    val msgs by remember(chatId) { Repo.messages(chatId) }.collectAsState(initial = emptyList())
    val images = msgs.filter { it.type == "image" && !it.deleted }
    var index by remember(images.size) { mutableIntStateOf(images.indexOfFirst { it.id == msgId }.coerceAtLeast(0)) }
    val cur = images.getOrNull(index)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(clr(R.color.viewer_bg)).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                Icons.Filled.Close,
                str(R.string.close),
                { nav.popBackStack() },
                tint = Color.White,
                bg = Color.White.copy(alpha = 0.12f),
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cur?.senderName.orEmpty(), color = Color.White, fontWeight = FontWeight.ExtraBold)
                if (cur != null) {
                    Text(
                        str(R.string.of, timeOf(cur.millis()), index + 1, images.size),
                        color = clr(R.color.viewer_text),
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.size(40.dp))
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(clr(R.color.viewer_surface)),
            contentAlignment = Alignment.Center,
        ) {
            if (cur != null) AsyncImage(cur.imageUrl, str(R.string.photo), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        if (images.size > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                images.take(7).forEachIndexed { i, m ->
                    AsyncImage(
                        m.imageUrl,
                        null,
                        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).clickable {
                            index = i
                        }.background(
                            if (i == index) Color.White else Color.Transparent,
                        ).padding(if (i == index) 2.dp else 0.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        alpha = if (i == index) 1f else 0.6f,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            ViewerAction(str(R.string.share), Icons.Filled.Share, Color.White) {
                cur?.let {
                    val i =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, it.imageUrl)
                        }
                    ctx.startActivity(Intent.createChooser(i, str(R.string.share_photo)))
                }
            }
            ViewerAction(str(R.string.open), Icons.Filled.OpenInBrowser, Color.White) {
                cur?.let { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it.imageUrl))) }
            }
            ViewerAction(str(R.string.hide), Icons.Filled.VisibilityOff, clr(R.color.viewer_danger)) {
                cur?.let {
                    scope.launchIO { Repo.deleteForMe(it) }
                    nav.popBackStack()
                }
            }
        }
    }
}

@Composable
private fun ViewerAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = tint)
        }
        Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                str(R.string.spam),
                str(R.string.harassment),
                str(R.string.scam),
                str(R.string.inappropriate),
                str(R.string.other),
            ).forEach {
                SelectChip(it, reason == it) { reason = it }
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