package com.kdevs.spendly.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kdevs.spendly.MainActivity
import com.kdevs.spendly.R
import com.kdevs.spendly.data.model.Bill
import com.kdevs.spendly.nav.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Tells the person a bill is due soon. Runs from the same background job as recurring items (every 15 minutes), so it
 * works with the app closed. Each bill and due date is announced once; paying a repeating bill moves it to a new date,
 * which earns its own reminder.
 */
object BillReminders {
    private const val CHANNEL = "bills"

    /** When the reminder for [b] is due: 9:00 AM, [Bill.remindDays] days before the due date. */
    fun remindAt(b: Bill): Long = withMinutes(toLocalDate(b.dueMillis).minusDays(b.remindDays.toLong()).startMillis(), 9 * 60)

    suspend fun check(ctx: Context) {
        if (Repo.uid.isBlank()) return
        withTimeoutOrNull(10_000) { Repo.loaded.first { "bills" in it } } ?: return
        if (Inbox.inForeground || !Inbox.notificationsAllowed(ctx)) return
        val sp = ctx.getSharedPreferences("spendly_bill_reminders", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        for (b in Repo.bills.value) {
            if (b.paid || now < remindAt(b)) continue
            val key = "${b.id}_${b.dueMillis}"
            if (sp.getBoolean(key, false)) continue
            notify(ctx, b)
            sp.edit().putBoolean(key, true).apply()
        }
    }

    private fun notify(
        ctx: Context,
        b: Bill,
    ) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, str(R.string.bills_channel), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open =
            Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DeepLink.EXTRA, Routes.BILLS)
            }
        val pi = PendingIntent.getActivity(ctx, b.id.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val days =
            java.time.temporal.ChronoUnit.DAYS
                .between(java.time.LocalDate.now(), toLocalDate(b.dueMillis))
                .toInt()
        val whenText =
            when {
                days < 0 -> str(R.string.overdue_by_days, -days)
                days == 0 -> str(R.string.due_today)
                days == 1 -> str(R.string.due_tomorrow)
                else -> str(R.string.due_in_days, days)
            }
        val n =
            NotificationCompat
                .Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(ctx, R.color.notification_accent))
                .setContentTitle(str(R.string.bill_reminder_title, b.name))
                .setContentText("$whenText · ${formatPaise(b.amountPaise)}")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(b.id.hashCode(), n)
    }
}
