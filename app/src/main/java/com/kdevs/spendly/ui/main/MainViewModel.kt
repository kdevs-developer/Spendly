package com.kdevs.spendly.ui.main

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.Res
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Holds what MainActivity used to keep in fields: who is signed in and whether the app lock is showing.
 * It lives through rotation, so the lock does not flicker or reset when the phone turns.
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor() : ViewModel() {
        /** The signed-in user id, or null. (Module 2 will read this from an injected AuthRepository.) */
        val uid: StateFlow<String?> = Repo.uidFlow

        private val _locked = MutableStateFlow(false)
        val locked: StateFlow<Boolean> = _locked.asStateFlow()

        private var started = false
        private var leftAt = 0L

        /** Call once from onCreate. A fresh launch is locked when the lock is on and the phone can ask. */
        fun startSession(biometricAvailable: Boolean) {
            if (started) return // rotation: keep the state we already have
            started = true
            _locked.value = Prefs.biometric && Repo.isSignedIn && biometricAvailable
        }

        fun unlock() {
            _locked.value = false
            leftAt = 0L
        }

        fun onAppStopped() {
            leftAt = SystemClock.elapsedRealtime()
        }

        /** Coming back after more than app_lock_after_seconds (res/values/config.xml) away asks again. */
        fun onAppStarted(biometricAvailable: Boolean) {
            val awayMillis = SystemClock.elapsedRealtime() - leftAt
            val limit = Res.integer(R.integer.app_lock_after_seconds) * 1000L
            if (leftAt != 0L && Prefs.biometric && Repo.isSignedIn && awayMillis > limit && biometricAvailable) {
                _locked.value = true
            }
        }
    }
