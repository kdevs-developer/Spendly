package com.kdevs.spendly

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.kdevs.spendly.data.Inbox
import com.kdevs.spendly.data.Insights
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.RecurringScheduler
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.Res
import com.kdevs.spendly.data.SpendlyWidget
import com.kdevs.spendly.data.monthStart
import com.kdevs.spendly.data.nextMonthStart
import com.kdevs.spendly.data.startMillis
import com.kdevs.spendly.data.todayMillis
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidApp
class SpendlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Res.init(this) // first: strings and colors are read from XML by everything below
        // Offline-first: Firestore keeps a copy on the phone, so screens load without internet and saves
        // are queued and sent later. Must be set before Firestore is first used.
        FirebaseFirestore.getInstance().firestoreSettings =
            firestoreSettings {
                setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            }
        Prefs.init(this)
        Repo.init(this)

        // The notification channel must exist before any push arrives, even when the app is closed.
        Inbox.createChannel(this)
        Inbox.start(this, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))

        // Adds recurring transactions when their day and time comes, even with the app closed.
        RecurringScheduler.start(this, CoroutineScope(SupervisorJob() + Dispatchers.Default))

        // Keeps the home screen widget's totals current.
        val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        widgetScope.launch {
            Repo.uidFlow
                .flatMapLatest { uid ->
                    if (uid == null) {
                        flowOf(Triple(false, 0L, 0L))
                    } else {
                        Repo.loaded.first { "txns" in it }
                        Repo.txns.map { list ->
                            val today = todayMillis()
                            Triple(
                                true,
                                Insights.totalSpent(
                                    list,
                                    today,
                                    java.time.LocalDate
                                        .now()
                                        .plusDays(1)
                                        .startMillis(),
                                ),
                                Insights.totalSpent(list, monthStart(), nextMonthStart()),
                            )
                        }
                    }
                }.distinctUntilChanged()
                .collect { (signedIn, day, month) -> SpendlyWidget.save(this@SpendlyApp, signedIn, day, month) }
        }

        // Knows whether any screen is visible, to choose between the in-app banner and the notification bar.
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                private var started = 0

                override fun onActivityStarted(activity: Activity) {
                    started++
                    Inbox.inForeground = true
                }

                override fun onActivityStopped(activity: Activity) {
                    started = (started - 1).coerceAtLeast(0)
                    if (started == 0) Inbox.inForeground = false
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {}

                override fun onActivityResumed(activity: Activity) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) {}

                override fun onActivityDestroyed(activity: Activity) {}
            },
        )
    }
}
