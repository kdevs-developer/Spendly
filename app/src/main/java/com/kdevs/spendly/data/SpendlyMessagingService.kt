package com.kdevs.spendly.data

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives pushes (the app may be closed). It shows the notification, and tells the sender the message
 * reached this phone. The server should send a data-only message (title, body, chatId) so this always runs.
 */
class SpendlyMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Repo.saveToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val chatId = message.data[Inbox.EXTRA_CHAT].orEmpty()
        val title = message.notification?.title ?: message.data["title"] ?: "Spendly"
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        if (chatId.isNotBlank()) scope.launch { runCatching { Repo.markDelivered(chatId) } }
        // While the app is on screen the in-app banner already handles it.
        if (Inbox.inForeground) return
        if (chatId == Inbox.openChatId) return
        Inbox.showSystem(this, chatId.ifBlank { "chat" }, title, body)
    }
}