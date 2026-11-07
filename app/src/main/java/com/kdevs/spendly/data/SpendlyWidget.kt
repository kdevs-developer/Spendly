package com.kdevs.spendly.data

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kdevs.spendly.MainActivity
import com.kdevs.spendly.R
import com.kdevs.spendly.nav.Routes
import java.time.LocalDate

/**
 * Home screen widget: what was spent today and this month, and a button that opens Add expense.
 * The app saves the two totals on the phone whenever transactions change ([save]); the widget only reads them.
 */
class SpendlyWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, build(context)) }
    }

    companion object {
        private const val PREFS = "spendly_widget"

        /** Called by the app with the latest totals. Totals are stored with their day so an old day never shows as today. */
        fun save(
            ctx: Context,
            signedIn: Boolean,
            todayPaise: Long,
            monthPaise: Long,
        ) {
            ctx
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("signedIn", signedIn)
                .putString("day", LocalDate.now().toString())
                .putString("month", LocalDate.now().withDayOfMonth(1).toString())
                .putLong("today", todayPaise)
                .putLong("monthTotal", monthPaise)
                .apply()
            refresh(ctx)
        }

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, SpendlyWidget::class.java))
            ids.forEach { mgr.updateAppWidget(it, build(ctx)) }
        }

        private fun open(
            ctx: Context,
            code: Int,
            route: String?,
        ): PendingIntent {
            val i =
                Intent(ctx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    if (route != null) putExtra(DeepLink.EXTRA, route)
                }
            return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun build(ctx: Context): RemoteViews {
            val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val v = RemoteViews(ctx.packageName, R.layout.widget_today)
            val signedIn = sp.getBoolean("signedIn", false)
            val sameDay = sp.getString("day", "") == LocalDate.now().toString()
            val sameMonth = sp.getString("month", "") == LocalDate.now().withDayOfMonth(1).toString()
            if (!signedIn) {
                v.setTextViewText(R.id.widget_amount, ctx.getString(R.string.app_name))
                v.setTextViewText(R.id.widget_month, ctx.getString(R.string.widget_sign_in))
            } else {
                v.setTextViewText(R.id.widget_amount, formatPaise(if (sameDay) sp.getLong("today", 0L) else 0L))
                v.setTextViewText(
                    R.id.widget_month,
                    ctx.getString(R.string.widget_this_month, formatPaise(if (sameMonth) sp.getLong("monthTotal", 0L) else 0L)),
                )
            }
            v.setOnClickPendingIntent(R.id.widget_root, open(ctx, 1, null))
            v.setOnClickPendingIntent(R.id.widget_add, open(ctx, 2, if (signedIn) Routes.txnAdd("expense") else null))
            return v
        }
    }
}
