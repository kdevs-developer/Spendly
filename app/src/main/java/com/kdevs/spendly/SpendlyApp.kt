package com.kdevs.spendly

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.Res

class SpendlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Res.init(this) // first: strings and colours are read from XML by everything below
        // Offline-first: keep a local copy of Firestore data (default 100 MB cache).
        FirebaseFirestore.getInstance().firestoreSettings =
            firestoreSettings {
                setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            }
        Prefs.init(this)
        Repo.init(this)
    }
}
