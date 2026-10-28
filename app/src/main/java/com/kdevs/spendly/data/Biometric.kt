package com.kdevs.spendly.data

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Fingerprint, face or the phone's screen lock, whichever the phone has set up. */
object Biometric {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun available(ctx: Context): Boolean = BiometricManager.from(ctx).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the system prompt. [onResult] gets true only when the person really authenticated. */
    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (Boolean) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val bp =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onResult(true)
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        onResult(false)
                    }
                },
            )
        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title).setSubtitle(subtitle)
                .setAllowedAuthenticators(AUTH)
                .build()
        bp.authenticate(info)
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}
