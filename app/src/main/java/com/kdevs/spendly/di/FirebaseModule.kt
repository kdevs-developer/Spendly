package com.kdevs.spendly.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The only place that calls getInstance(). Repositories ask for these in their constructor. */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun auth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton fun storage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton fun messaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}
