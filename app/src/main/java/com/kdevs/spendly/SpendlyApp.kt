package com.kdevs.spendly

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.kdevs.spendly.data.Repo

class SpendlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Offline-first: keep a local copy of Firestore data (default 100 MB cache).
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        }
        Repo.init(this)
    }
}
