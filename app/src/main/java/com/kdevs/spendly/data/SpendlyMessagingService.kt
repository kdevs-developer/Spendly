package com.kdevs.spendly.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kdevs.spendly.R

/** Shows a notification for new chat messages and saves the device token for the signed-in user. */
class SpendlyMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Repo.saveToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("chat", str(R.string.chat_messages), NotificationManager.IMPORTANCE_HIGH))
        }
        val title = message.notification?.title ?: message.data["title"] ?: str(R.string.spendly)
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val n =
            NotificationCompat.Builder(this, "chat")
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(androidx.core.content.ContextCompat.getColor(this, R.color.notification_accent))
                .setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        nm.notify(System.currentTimeMillis().toInt(), n)
    }
}
