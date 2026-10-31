package com.kdevs.spendly

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.kdevs.spendly.data.Inbox
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.Res
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
