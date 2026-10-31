package com.kdevs.spendly.ui.screens.chat

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Picked
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun reactionIcon(key: String) = reactionIcons.firstOrNull { it.first == key }

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
        chat
            ?.typing
            ?.filter { it.key != me && now - it.value < 5000 }
            ?.keys
            ?.mapNotNull {
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
            Modifier
                .fillMaxWidth()
                .background(
                    Surface,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ).navigationBarsPadding()
                .imePadding()
                .padding(12.dp),
        ) {
            replyTo?.let { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryContainer)
                        .padding(10.dp),
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
                            if (r.type == "image") {
                                str(R.string.photo)
                            } else if (r.type == "video") {
                                str(R.string.video)
                            } else {
                                r.text
                            },
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

fun kotlinx.coroutines.CoroutineScope.launchIO(block: suspend () -> Unit) {
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
            Modifier
                .widthIn(max = 290.dp)
                .clip(shape)
                .background(
                    if (m.deleted) {
                        Track
                    } else if (mine) {
                        Brand
                    } else {
                        Surface
                    },
                ).combinedClickable(
                    onClick = { if ((m.type == "image" || m.type == "video") && !m.deleted) onImage() },
                    onLongClick = { if (!m.deleted) onLong() },
                ).padding(horizontal = 12.dp, vertical = 8.dp),
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
                        Box(
                            Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, str(R.string.play_video), tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Text(
                            str(R.string.video),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                        )
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
                Modifier
                    .padding(
                        horizontal = 6.dp,
                    ).offset(y = (-6).dp)
                    .clip(CircleShape)
                    .background(Surface)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                m.reactions.values.distinct().forEach { k ->
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
