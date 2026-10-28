package com.kdevs.spendly.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Chat.title(me: String): String =
    if (type == "direct") memberNames.entries.firstOrNull { it.key != me }?.value ?: "Chat" else name.ifBlank { "Group" }

private fun Chat.otherId(me: String): String = members.firstOrNull { it != me } ?: ""
private fun Message.millis(): Long = createdAt?.toDate()?.time ?: System.currentTimeMillis()

/** Reaction keys and their icons. Drawn as icons, not emoji, so they match the app. */
private val reactionIcons = listOf(
    Triple("like", Icons.Filled.ThumbUp, Color(0xFF1D4ED8)),
    Triple("love", Icons.Filled.Favorite, Color(0xFFC62828)),
    Triple("laugh", Icons.Filled.SentimentVerySatisfied, Color(0xFFB45309)),
    Triple("wow", Icons.Filled.Bolt, Color(0xFFB45309)),
    Triple("sad", Icons.Filled.SentimentDissatisfied, Color(0xFFB45309)),
    Triple("star", Icons.Filled.Star, Color(0xFFB45309)),
)
private fun reactionIcon(key: String) = reactionIcons.firstOrNull { it.first == key }

// ------------------------------------------------------------------ Chat list and friends

@Composable
fun ChatListScreen(nav: NavController) {
    val chats by Repo.chats.collectAsState()
    val incoming by Repo.incomingRequests.collectAsState()
    val me = Repo.uid
    TabScreen("Chat", Routes.CHATS, nav,
        actions = {
            Box {
                RoundIconButton(Icons.Filled.PersonAdd, "Friend requests", { nav.navigate(Routes.REQUESTS) })
                if (incoming.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(Danger), contentAlignment = Alignment.Center) {
                    Text("${incoming.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        fab = { Fab(Icons.Filled.Edit, "New chat") { nav.navigate(Routes.NEW_CHAT) } },
    ) {
        if (chats.isEmpty()) EmptyState(Icons.AutoMirrored.Filled.Chat, "No chats yet", "Add a friend with their code, then start a chat or a split group.")
        else Card(padding = 12.dp) {
            chats.forEachIndexed { i, c ->
                val title = c.title(me)
                Row(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chat(c.id)) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (c.type == "direct") Avatar(title, colorKeyFor(c.otherId(me)), 48.dp)
                    else IconCircle(if (c.type == "split") "wallet" else "kids", if (c.type == "split") "teal" else "purple", 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1)
                        Text(c.lastMessage.ifBlank { "No messages yet" }, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    c.lastAt?.let { Text(timeOf(it.toDate().time), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
                if (i < chats.lastIndex) HorizontalDivider(color = Line)
            }
        }
        Spacer(Modifier.height(70.dp))
    }
}

@Composable
fun NewChatScreen(nav: NavController) {
    val friends by Repo.friends.collectAsState()
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    ScreenScaffold("New chat", { nav.popBackStack() }) {
        Card(padding = 12.dp) {
            SettingRow("New group", icon = Icons.Filled.Groups, iconColor = Color(0xFF7E22CE), onClick = { nav.navigate(Routes.CREATE_GROUP) })
            SettingRow("Add a friend", icon = Icons.Filled.PersonAdd, iconColor = Primary, onClick = { nav.navigate(Routes.ADD_FRIEND) }, divider = false)
        }
        saver.error?.let { Text(it, color = Danger) }
        SectionTitle("Friends")
        if (friends.isEmpty()) EmptyState(Icons.Filled.People, "No friends yet", "Share your friend code or enter theirs.")
        else Card(padding = 12.dp) {
            friends.forEachIndexed { i, f ->
                SettingRow(f.name, leading = { Avatar(f.name, colorKeyFor(f.id)) }, onClick = {
                    saver.run(scope, onDone = {}) { val id = Repo.openDirectChat(f); nav.navigate(Routes.chat(id)) { popUpTo(Routes.CHATS) } }
                }, divider = i < friends.lastIndex)
            }
        }
    }
}

@Composable
fun AddFriendScreen(nav: NavController) {
    val profile by Repo.profile.collectAsState()
    val outgoing by Repo.outgoingRequests.collectAsState()
    val ctx = LocalContext.current
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    ScreenScaffold("Add friend", { nav.popBackStack() }) {
        HeroCard {
            Text("Your friend code", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            Text(profile.code.ifBlank { "..." }, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)).clickable {
                val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Add me on Spendly. My friend code is ${profile.code}") }
                ctx.startActivity(Intent.createChooser(i, "Share code"))
            }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Share my code", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        SectionTitle("Enter a friend's code")
        AppTextField(code, { code = it.uppercase().filter { c -> c.isLetterOrDigit() || c == ' ' }.take(9) }, "8 letters and numbers")
        msg?.let { Text(it, color = if (ok) Success else Danger, fontWeight = FontWeight.SemiBold) }
        PrimaryButton("Send request", {
            msg = null
            saver.run(scope, onDone = {}) {
                val r = Repo.sendFriendRequest(code)
                ok = r == null
                msg = r ?: "Request sent. They will see it in Chat."
                if (ok) code = ""
            }
        }, enabled = code.filter { it.isLetterOrDigit() }.length == 8, busy = saver.busy)
        if (outgoing.isNotEmpty()) {
            SectionTitle("Waiting for a reply")
            Card(padding = 12.dp) { outgoing.forEachIndexed { i, r -> SettingRow("Request ${i + 1}", "Pending", icon = Icons.Filled.Schedule, iconColor = Faint, divider = i < outgoing.lastIndex) } }
        }
    }
}

@Composable
fun RequestsScreen(nav: NavController) {
    val incoming by Repo.incomingRequests.collectAsState()
    val friends by Repo.friends.collectAsState()
    val scope = rememberCoroutineScope()
    val saver = rememberSaver()
    ScreenScaffold("Friends and requests", { nav.popBackStack() },
        actions = { RoundIconButton(Icons.Filled.PersonAdd, "Add friend", { nav.navigate(Routes.ADD_FRIEND) }) }) {
        SectionTitle("Requests")
        if (incoming.isEmpty()) Hint("No new requests.")
        incoming.forEach { r ->
            Card {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(r.fromName, colorKeyFor(r.from), 46.dp)
                    Column(Modifier.weight(1f)) { Text(r.fromName, fontWeight = FontWeight.ExtraBold, color = Ink); Hint("wants to be friends") }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { SecondaryButton("Decline", { saver.run(scope, {}) { Repo.answerRequest(r, false) } }, danger = true) }
                    Box(Modifier.weight(1f)) { PrimaryButton("Accept", { saver.run(scope, {}) { Repo.answerRequest(r, true) } }) }
                }
            }
        }
        saver.error?.let { Text(it, color = Danger) }
        SectionTitle("Friends")
        if (friends.isEmpty()) Hint("No friends yet.")
        else Card(padding = 12.dp) {
            friends.forEachIndexed { i, f ->
                SettingRow(f.name, f.code, leading = { Avatar(f.name, colorKeyFor(f.id)) }, divider = i < friends.lastIndex,
                    trailing = { Text("Remove", color = Danger, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable { saver.run(scope, {}) { Repo.removeFriend(f.id) } }.padding(8.dp)) })
            }
        }
        SettingRowCard("Blocked people", Icons.Filled.Block) { nav.navigate(Routes.BLOCKED) }
    }
}

@Composable
private fun SettingRowCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(padding = 12.dp) { SettingRow(label, icon = icon, iconColor = Danger, onClick = onClick, divider = false) }
}

@Composable
fun BlockedScreen(nav: NavController) {
    val profile by Repo.profile.collectAsState()
    val friends by Repo.friends.collectAsState()
    val chats by Repo.chats.collectAsState()
    val scope = rememberCoroutineScope(); val saver = rememberSaver()
    fun nameOf(id: String) = friends.firstOrNull { it.id == id }?.name
        ?: chats.firstNotNullOfOrNull { it.memberNames[id] } ?: "User ${id.take(6)}"
    ScreenScaffold("Blocked people", { nav.popBackStack() }) {
        Hint("You will not see messages from people you block.")
        if (profile.blocked.isEmpty()) EmptyState(Icons.Filled.Block, "No one blocked", "People you block will show up here.")
        else Card(padding = 12.dp) {
            profile.blocked.forEachIndexed { i, id ->
                SettingRow(nameOf(id), leading = { Avatar(nameOf(id), colorKeyFor(id)) }, divider = i < profile.blocked.lastIndex,
                    trailing = { Text("Unblock", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable { saver.run(scope, {}) { Repo.unblock(id) } }.padding(8.dp)) })
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
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    ScreenScaffold("New group", { nav.popBackStack() }, bottomBar = {
        SavedBar("Create group", saver, name.isNotBlank() && picked.isNotEmpty(), onSave = {
            saver.run(scope, onDone = {}) {
                val id = Repo.createGroup(name.trim(), if (split == 1) "split" else "group", friends.filter { it.id in picked })
                nav.navigate(Routes.chat(id)) { popUpTo(Routes.CHATS) }
            }
        })
    }) {
        SegmentedControl(listOf("Chat group", "Expense split"), split, { split = it })
        Hint(if (split == 1) "Chat plus a shared ledger. Add expenses and see who owes whom." else "A group chat for everyone you add.")
        AppTextField(name, { name = it }, "Group name")
        SectionTitle("Add members (${picked.size})")
        if (friends.isEmpty()) Hint("Add friends first from the Chat tab.")
        Card(padding = 12.dp) {
            friends.forEachIndexed { i, f ->
                SettingRow(f.name, leading = { Avatar(f.name, colorKeyFor(f.id)) },
                    onClick = { picked = if (f.id in picked) picked - f.id else picked + f.id },
                    trailing = { TickBox(f.id in picked) }, divider = i < friends.lastIndex)
            }
        }
    }
}

@Composable
fun AddMembersScreen(nav: NavController, id: String) {
    val friends by Repo.friends.collectAsState()
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    var picked by remember { mutableStateOf(setOf<String>()) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    val candidates = friends.filter { f -> chat?.members?.contains(f.id) != true }
    ScreenScaffold("Add members", { nav.popBackStack() }, bottomBar = {
        SavedBar("Add ${picked.size} people", saver, picked.isNotEmpty(), onSave = {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.addMembers(id, candidates.filter { it.id in picked }) }
        })
    }) {
        if (candidates.isEmpty()) EmptyState(Icons.Filled.People, "No one to add", "All your friends are already here.")
        else Card(padding = 12.dp) {
            candidates.forEachIndexed { i, f ->
                SettingRow(f.name, leading = { Avatar(f.name, colorKeyFor(f.id)) }, onClick = { picked = if (f.id in picked) picked - f.id else picked + f.id },
                    trailing = { TickBox(f.id in picked) }, divider = i < candidates.lastIndex)
            }
        }
    }
}

// ------------------------------------------------------------------ Conversation

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(nav: NavController, entry: NavBackStackEntry, id: String) {
    val me = Repo.uid
    var chat by remember { mutableStateOf<Chat?>(null) }
    val msgs by remember(id) { Repo.messages(id) }.collectAsState(initial = emptyList())
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Message?>(null) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launchIO { Repo.sendImage(id, uri) }
    }
    entry.Picked<String>("replyTo") { mid -> replyTo = msgs.firstOrNull { it.id == mid } }

    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.lastIndex) }
    LaunchedEffect(msgs) { runCatching { Repo.markRead(id, msgs) } }

    // Typing: say "typing" while the field changes, and clear it after 3 seconds of quiet.
    LaunchedEffect(text) {
        if (text.isNotBlank()) { Repo.setTyping(id, true); delay(3000); Repo.setTyping(id, false) }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(2000); now = System.currentTimeMillis() } }
    val typingNames = chat?.typing?.filter { it.key != me && now - it.value < 5000 }?.keys?.mapNotNull { chat?.memberNames?.get(it) }.orEmpty()

    val c = chat
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { nav.popBackStack() })
            Row(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { nav.navigate(Routes.chatInfo(id)) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val title = c?.title(me) ?: ""
                if (c?.type == "direct") Avatar(title.ifBlank { "?" }, colorKeyFor(c.otherId(me)), 38.dp) else IconCircle(if (c?.type == "split") "wallet" else "kids", if (c?.type == "split") "teal" else "purple", 38.dp)
                Column {
                    Text(title, fontWeight = FontWeight.ExtraBold, color = Ink, maxLines = 1)
                    Text(if (typingNames.isNotEmpty()) "typing..." else if (c?.type == "direct") "Tap for info" else "${c?.members?.size ?: 0} members",
                        color = if (typingNames.isNotEmpty()) Primary else Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (c?.type == "split") RoundIconButton(Icons.Filled.Receipt, "Split ledger", { nav.navigate(Routes.splits(id)) })
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), state = list, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Spacer(Modifier.height(6.dp)) }
            items(msgs, key = { it.id }) { m ->
                if (m.type == "system") Text(m.text, Modifier.fillMaxWidth().padding(vertical = 6.dp), textAlign = TextAlign.Center, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                else Bubble(m, mine = m.senderId == me, group = c?.type != "direct", members = c?.members?.size ?: 2,
                    onLong = { nav.navigate(Routes.messageActions(id, m.id)) },
                    onImage = { nav.navigate(Routes.image(id, m.id)) })
            }
            item { Spacer(Modifier.height(6.dp)) }
        }
        Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).navigationBarsPadding().imePadding().padding(12.dp)) {
            replyTo?.let { r ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PrimaryContainer).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Replying to ${if (r.senderId == me) "yourself" else r.senderName}", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(if (r.type == "image") "Photo" else r.text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.Close, "Cancel reply", Modifier.clip(CircleShape).clickable { replyTo = null }.padding(4.dp), tint = Muted)
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(Icons.Filled.Image, "Send a photo", { picker.launch("image/*") }, tint = Primary, bg = PrimaryContainer)
                androidx.compose.material3.OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("Message") },
                    shape = RoundedCornerShape(24.dp), maxLines = 4,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedContainerColor = Bg, unfocusedContainerColor = Bg, unfocusedBorderColor = Color.Transparent, focusedBorderColor = Primary))
                RoundIconButton(Icons.AutoMirrored.Filled.Send, "Send", {
                    val t = text.trim()
                    if (t.isNotEmpty()) { val r = replyTo; text = ""; replyTo = null; scope.launchIO { Repo.sendText(id, t, r) } }
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
private fun Bubble(m: Message, mine: Boolean, group: Boolean, members: Int, onLong: () -> Unit, onImage: () -> Unit) {
    val shape = if (mine) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        if (!mine && group) Text(m.senderName, color = colorSpec(colorKeyFor(m.senderId)).solid, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        Column(
            Modifier.widthIn(max = 290.dp).clip(shape)
                .background(if (m.deleted) Color(0xFFE5EAF3) else if (mine) Primary else Surface)
                .combinedClickable(onClick = { if (m.type == "image" && !m.deleted) onImage() }, onLongClick = { if (!m.deleted) onLong() })
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val ink = if (mine && !m.deleted) Color.White else Ink
            if (m.replyPreview.isNotBlank() && !m.deleted) Text(m.replyPreview, color = ink.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ink.copy(alpha = 0.12f)).padding(6.dp))
            when {
                m.deleted -> Text("This message was deleted", color = Muted, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 14.sp)
                m.type == "image" -> AsyncImage(m.imageUrl, "Photo", Modifier.size(220.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                else -> Text(m.text, color = ink, fontSize = 15.sp)
            }
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(timeOf(m.millis()), color = ink.copy(alpha = 0.7f), fontSize = 10.sp)
                if (mine && !m.deleted) Icon(if (m.readBy.size >= members) Icons.Filled.DoneAll else Icons.Filled.Done, if (m.readBy.size >= members) "Seen" else "Sent",
                    tint = if (m.readBy.size >= members) Color(0xFF93C5FD) else ink.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
        }
        if (m.reactions.isNotEmpty() && !m.deleted) Row(Modifier.padding(horizontal = 6.dp).offset(y = (-6).dp).clip(CircleShape).background(Surface).padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            m.reactions.values.distinct().forEach { k -> reactionIcon(k)?.let { Icon(it.second, k, tint = it.third, modifier = Modifier.size(14.dp)) } }
            if (m.reactions.size > 1) Text("${m.reactions.size}", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MessageActionsScreen(nav: NavController, chatId: String, msgId: String) {
    val me = Repo.uid
    val msgs by remember(chatId) { Repo.messages(chatId) }.collectAsState(initial = emptyList())
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(chatId) { Repo.chat(chatId).collect { chat = it } }
    val m = msgs.firstOrNull { it.id == msgId }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold("Message", { nav.popBackStack() }) {
        if (m == null) { EmptyState(Icons.Filled.SearchOff, "Message gone", "It was deleted."); return@ScreenScaffold }
        val mine = m.senderId == me
        val left = 5 * 60_000L - (now - m.millis())
        val canDeleteAll = mine && left > 0 && !m.deleted
        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Box(Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(20.dp)).background(if (mine) Primary else Surface).padding(14.dp)) {
                Text(if (m.type == "image") "Photo" else m.text, color = if (mine) Color.White else Ink, fontSize = 15.sp)
            }
            Hint(timeOf(m.millis()) + " · " + fullDate(m.millis()))
        }
        Row(Modifier.fillMaxWidth().clip(CircleShape).background(Surface).padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            reactionIcons.forEach { (k, icon, tint) ->
                val on = m.reactions[me] == k
                Box(Modifier.size(46.dp).clip(CircleShape).background(if (on) tint.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { scope.launchIO { Repo.react(chatId, m, k) }; nav.popBackStack() }, contentAlignment = Alignment.Center) {
                    Icon(icon, k, tint = tint, modifier = Modifier.size(26.dp))
                }
            }
        }
        Card(padding = 12.dp) {
            SettingRow("Reply", icon = Icons.AutoMirrored.Filled.Reply, iconColor = Primary, onClick = { nav.setPicked("replyTo", m.id); nav.popBackStack() })
            if (m.type == "text") SettingRow("Copy text", icon = Icons.Filled.ContentCopy, iconColor = Muted, onClick = { clip.setText(AnnotatedString(m.text)); nav.popBackStack() })
            val others = (chat?.members?.size ?: 2) - 1
            SettingRow("Message info", if (mine) "Seen by ${(m.readBy.size - 1).coerceAtLeast(0)} of $others" else "Sent by ${m.senderName}", icon = Icons.Filled.Info, iconColor = Muted)
            if (canDeleteAll) SettingRow("Delete for everyone", "%d:%02d left".format(left / 60000, (left / 1000) % 60), icon = Icons.Filled.Delete, iconColor = Danger,
                onClick = { confirm = true })
            SettingRow("Delete for me", icon = Icons.Filled.DeleteOutline, iconColor = Danger, onClick = { scope.launchIO { Repo.deleteForMe(m) }; nav.popBackStack() }, divider = false)
        }
        Hint("You can delete for everyone within 5 minutes of sending. After that, only for yourself.", center = true)
    }
    if (confirm && m != null) ConfirmDialog("Delete for everyone?", "Everyone in the chat will see that the message was deleted.", "Delete", {
        scope.launchIO { Repo.deleteForEveryone(chatId, m) }; nav.popBackStack()
    }, { confirm = false }, danger = true)
}

@Composable
fun ImageViewerScreen(nav: NavController, chatId: String, msgId: String) {
    val msgs by remember(chatId) { Repo.messages(chatId) }.collectAsState(initial = emptyList())
    val images = msgs.filter { it.type == "image" && !it.deleted }
    var index by remember(images.size) { mutableIntStateOf(images.indexOfFirst { it.id == msgId }.coerceAtLeast(0)) }
    val cur = images.getOrNull(index)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Color(0xFF0B0F14)).statusBarsPadding().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Filled.Close, "Close", { nav.popBackStack() }, tint = Color.White, bg = Color.White.copy(alpha = 0.12f))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cur?.senderName.orEmpty(), color = Color.White, fontWeight = FontWeight.ExtraBold)
                if (cur != null) Text("${timeOf(cur.millis())} · ${index + 1} of ${images.size}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
            }
            Spacer(Modifier.size(40.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF111827)), contentAlignment = Alignment.Center) {
            if (cur != null) AsyncImage(cur.imageUrl, "Photo", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        if (images.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            images.take(7).forEachIndexed { i, m ->
                AsyncImage(m.imageUrl, null, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).clickable { index = i }.background(if (i == index) Color.White else Color.Transparent).padding(if (i == index) 2.dp else 0.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop, alpha = if (i == index) 1f else 0.6f)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            ViewerAction("Share", Icons.Filled.Share, Color.White) {
                cur?.let { val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, it.imageUrl) }; ctx.startActivity(Intent.createChooser(i, "Share photo")) }
            }
            ViewerAction("Open", Icons.Filled.OpenInBrowser, Color.White) {
                cur?.let { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it.imageUrl))) }
            }
            ViewerAction("Hide", Icons.Filled.VisibilityOff, Color(0xFFFCA5A5)) {
                cur?.let { scope.launchIO { Repo.deleteForMe(it) }; nav.popBackStack() }
            }
        }
    }
}

@Composable
private fun ViewerAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(icon, label, tint = tint) }
        Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ Group info, report

@Composable
fun ChatInfoScreen(nav: NavController, id: String) {
    val me = Repo.uid
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    val profile by Repo.profile.collectAsState()
    var rename by remember(chat?.name) { mutableStateOf(chat?.name ?: "") }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    val c = chat
    ScreenScaffold(if (c?.type == "direct") "Contact info" else "Group info", { nav.popBackStack() }) {
        if (c == null) return@ScreenScaffold
        val direct = c.type == "direct"
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (direct) Avatar(c.title(me), colorKeyFor(c.otherId(me)), 88.dp) else IconCircle(if (c.type == "split") "wallet" else "kids", if (c.type == "split") "teal" else "purple", 88.dp)
            Text(c.title(me), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink)
            if (!direct) Hint("${c.members.size} members · ${if (c.type == "split") "Expense split group" else "Chat group"}")
        }
        if (!direct) {
            AppTextField(rename, { rename = it }, "Group name")
            if (rename.trim() != c.name && rename.isNotBlank()) PrimaryButton("Save name", { saver.run(scope, {}) { Repo.renameChat(id, rename.trim()) } }, busy = saver.busy)
        }
        if (c.type == "split") Card(padding = 12.dp) { SettingRow("Split ledger", "Balances and expenses", icon = Icons.Filled.Receipt, iconColor = Color(0xFF0F766E), onClick = { nav.navigate(Routes.splits(id)) }, divider = false) }
        SectionTitle("Members")
        Card(padding = 12.dp) {
            c.members.forEachIndexed { i, uid ->
                val n = c.memberNames[uid] ?: "Member"
                SettingRow(if (uid == me) "$n (you)" else n, if (uid == c.createdBy) "Admin" else "", leading = { Avatar(n, colorKeyFor(uid)) },
                    onClick = if (uid != me && !direct) ({ nav.navigate(Routes.reportUser(id, uid)) }) else null,
                    trailing = if (uid != me && !direct) ({ Text("Report", color = Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp) }) else null,
                    divider = i < c.members.lastIndex || !direct)
            }
            if (!direct) SettingRow("Add members", icon = Icons.Filled.PersonAdd, iconColor = Primary, onClick = { nav.navigate(Routes.addMembers(id)) }, divider = false)
        }
        if (direct) {
            val other = c.otherId(me)
            Card(padding = 12.dp) {
                SettingRow(if (other in profile.blocked) "Unblock" else "Block", icon = Icons.Filled.Block, iconColor = Danger,
                    onClick = { if (other in profile.blocked) saver.run(scope, {}) { Repo.unblock(other) } else confirmBlock = true })
                SettingRow("Report", icon = Icons.Filled.Flag, iconColor = Danger, onClick = { nav.navigate(Routes.reportUser(id, other)) }, divider = false)
            }
        } else SecondaryButton("Leave group", { confirmLeave = true }, danger = true)
        saver.error?.let { Text(it, color = Danger) }
    }
    if (confirmLeave) ConfirmDialog("Leave this group?", "You will no longer get its messages.", "Leave", {
        saver.run(scope, onDone = { nav.popBackStack(Routes.CHATS, false) }) { Repo.leaveChat(id) }
    }, { confirmLeave = false }, danger = true)
    if (confirmBlock) ConfirmDialog("Block this person?", "You will stop seeing their messages.", "Block", {
        saver.run(scope, onDone = {}) { chat?.otherId(me)?.let { Repo.block(it) } }
    }, { confirmBlock = false }, danger = true)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportUserScreen(nav: NavController, chatId: String, uid: String) {
    var reason by remember { mutableStateOf("Spam") }
    var details by remember { mutableStateOf("") }
    var alsoBlock by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    val saver = rememberSaver(); val scope = rememberCoroutineScope()
    ScreenScaffold("Report", { nav.popBackStack() }, bottomBar = {
        SavedBar(if (done) "Done" else "Send report", saver, true, onSave = {
            if (done) { nav.popBackStack(); return@SavedBar }
            saver.run(scope, onDone = { done = true }) {
                Repo.report(uid, chatId, if (details.isBlank()) reason else "$reason: ${details.trim()}")
                if (alsoBlock) Repo.block(uid)
            }
        })
    }) {
        if (done) { EmptyState(Icons.Filled.CheckCircle, "Report sent", "Thank you. We will look into it."); return@ScreenScaffold }
        Hint("Tell us what is wrong. The person will not know you reported them.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Spam", "Harassment", "Scam", "Inappropriate", "Other").forEach { SelectChip(it, reason == it) { reason = it } }
        }
        AppTextField(details, { details = it }, "More detail (optional)", singleLine = false, minLines = 3)
        Card(padding = 12.dp) { SettingRow("Also block this person", icon = Icons.Filled.Block, iconColor = Danger, trailing = { androidx.compose.material3.Switch(alsoBlock, { alsoBlock = it }) }, divider = false) }
    }
}
