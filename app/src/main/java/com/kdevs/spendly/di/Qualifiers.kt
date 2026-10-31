package com.kdevs.spendly.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/** App-wide scope for work that must outlive a screen (sync, recurring items). Never cancelled. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
