package com.kdevs.spendly.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kdevs.spendly.MainActivity
import com.kdevs.spendly.R
import com.kdevs.spendly.data.model.Txn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Adds the recurring transactions that are due, even when the app is closed. */
class RecurringWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // NonCancellable: the scheduler replaces this job as soon as the list changes, which must not cut the run short.
        return withContext(NonCancellable) {
            val created = runCatching { Repo.processRecurring() }.getOrElse { return@withContext Result.retry() }
            if (created.isNotEmpty()) RecurringScheduler.notifyCreated(applicationContext, created)
            runCatching { Repo.flushWrites() }
            Result.success()
        }
    }
}

/**
 * Keeps two jobs alive:
 *  - one that wakes up at the next due time of any active recurring item (the exact moment, within what Android allows),
 *  - a 15 minute safety net in case that one is delayed (battery saver, restart, phone was off).
 */
object RecurringScheduler {
    private const val PERIODIC = "recurring-periodic"
    private const val NEXT_DUE = "recurring-next-due"
    private const val CHANNEL = "recurring"

    /** Called once from SpendlyApp. */
    fun start(
        app: Context,
        scope: CoroutineScope,
    ) {
        createChannel(app)
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RecurringWorker>(15, TimeUnit.MINUTES).build(),
        )
        scope.launch {
            // Before the first snapshot the list is empty, which must not cancel a job that is already waiting.
            Repo.loaded.first { "recurring" in it }
            Repo.recurring
                .map { list -> list.filter { it.active && it.nextMillis > 0 }.minOfOrNull { it.nextMillis } }
                .distinctUntilChanged()
                .collect { due -> scheduleNext(app, due) }
        }
    }

    private fun scheduleNext(
        app: Context,
        dueMillis: Long?,
    ) {
        val wm = WorkManager.getInstance(app)
        if (dueMillis == null) {
            wm.cancelUniqueWork(NEXT_DUE)
            return
        }
        val delay = (dueMillis - System.currentTimeMillis()).coerceAtLeast(0)
        wm.enqueueUniqueWork(
            NEXT_DUE,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RecurringWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build(),
        )
    }

    private fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, str(R.string.recurring_channel), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    /** "Rent · ₹12,000 added" in the notification bar. Skipped while the app is open, the person can see it there. */
    fun notifyCreated(
        ctx: Context,
        created: List<Txn>,
    ) {
        if (Inbox.inForeground || !Inbox.notificationsAllowed(ctx)) return
        createChannel(ctx)
        val single = created.singleOrNull()
        val title = if (single != null) str(R.string.recurring_added) else str(R.string.recurring_added_many, created.size)
        val body =
            if (single != null) {
                str(R.string.recurring_added_body, single.note, formatPaise(single.amountPaise))
            } else {
                created.map { it.note }.distinct().joinToString(", ")
            }
        val open = Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pi = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n =
            NotificationCompat
                .Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(ctx, R.color.notification_accent))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(System.currentTimeMillis().toInt(), n)
    }
}
