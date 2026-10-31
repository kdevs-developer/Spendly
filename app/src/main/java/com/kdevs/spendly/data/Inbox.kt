package com.kdevs.spendly.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kdevs.spendly.MainActivity
import com.kdevs.spendly.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything about "a new message arrived": the in-app banner, the notification bar, the unread
 * counts, and telling the sender that the message was received (delivered).
 *
 * It works from Firestore snapshots while the app process is alive (open or in the background), so it
 * does not depend on Cloud Functions. Pushes (SpendlyMessagingService) cover the case where the app was closed.
 */
object Inbox {
    const val CHANNEL = "chat"
    const val EXTRA_CHAT = "chatId"

    data class Banner(
        val chatId: String,
        val title: String,
        val body: String,
        val key: Long = System.nanoTime(),
    )

    /** The message strip shown at the top of the app. Cleared by the banner itself. */
    val banner = MutableStateFlow<Banner?>(null)

    /** A chat the person asked to open from a notification. Nav consumes it. */
    val openRequest = MutableStateFlow<String?>(null)

    /** The chat on screen right now (no alert for it). Set by the conversation screen. */
    @Volatile var openChatId: String? = null

    /** Set by SpendlyApp from activity start/stop. */
    @Volatile var inForeground = false

    /** Total unread messages across all chats, for the badge on the Chat tab. */
    val unreadTotal: StateFlow<Int> by lazy {
        Repo.chats
            .map { list ->
                val me = Repo.uid
                list.sumOf { (it.unread[me] ?: 0L).toInt() }
            }.stateIn(CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate), SharingStarted.Eagerly, 0)
    }

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, str(R.string.chat_messages), NotificationManager.IMPORTANCE_HIGH))
        }
    }

    fun notificationsAllowed(ctx: Context): Boolean {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    /** Puts a message in the notification bar. Same chat = same notification, so pushes and local alerts never double up. */
    fun showSystem(
        ctx: Context,
        chatId: String,
        title: String,
        body: String,
    ) {
        if (!notificationsAllowed(ctx)) return
        createChannel(ctx)
        val open =
            Intent(ctx, MainActivity::class.java).apply {
                putExtra(EXTRA_CHAT, chatId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pi = PendingIntent.getActivity(ctx, chatId.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n =
            NotificationCompat
                .Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(ctx, R.color.notification_accent))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(chatId.hashCode(), n)
    }

    fun clear(
        ctx: Context,
        chatId: String,
    ) {
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(chatId.hashCode())
    }

    /** Called once from SpendlyApp. Watches the chat list for messages that arrive while the process is alive. */
    fun start(
        ctx: Context,
        scope: CoroutineScope,
    ) {
        val app = ctx.applicationContext
        val startedAt = System.currentTimeMillis()
        val notified = mutableMapOf<String, Long>() // chatId -> lastAt already handled
        scope.launch {
            Repo.chats.collect { list ->
                val me = Repo.uid
                if (me.isBlank()) return@collect
                val blocked = Repo.profile.value.blocked
                for (c in list) {
                    val at = c.lastAt?.toDate()?.time ?: continue
                    if (c.lastSenderId.isBlank() || c.lastSenderId == me) continue
                    if (at <= (notified[c.id] ?: startedAt)) continue
                    notified[c.id] = at
                    if (c.lastSenderId in blocked) continue
                    // 1. Tell the sender this phone has the message.
                    launch { runCatching { Repo.markDelivered(c.id) } }
                    // 2. Alert, unless that chat is already open on screen.
                    if (c.id == openChatId && inForeground) continue
                    val title = if (c.type == "direct") c.lastSenderName else "${c.name} · ${c.lastSenderName}"
                    if (inForeground) {
                        banner.value = Banner(c.id, title, c.lastMessage)
                    } else {
                        showSystem(app, c.id, title, c.lastMessage)
                    }
                }
            }
        }
    }
}
