
package com.kdevs.spendly.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.kdevs.spendly.R

enum class ThemeMode(
    val label: String,
) {
    SYSTEM(str(R.string.system)),
    LIGHT(str(R.string.light)),
    DARK(str(R.string.dark)),
}

/**
 * Small on-device settings: theme and app lock.
 * They belong to the phone, not the account, so they live in SharedPreferences.
 * The values are Compose state, so screens update immediately.
 */
object Prefs {
    private lateinit var sp: SharedPreferences

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var biometric by mutableStateOf(false)
        private set

    /**
     * Kept up to date by MainActivity so "System" can follow the phone.
     */
    var systemDark by mutableStateOf(false)

    val isDark: Boolean
        get() =
            when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

    fun init(ctx: Context) {
        sp =
            ctx.applicationContext.getSharedPreferences(
                "spendly_prefs",
                Context.MODE_PRIVATE,
            )

        themeMode =
            runCatching {
                ThemeMode.valueOf(
                    sp.getString("theme", ThemeMode.SYSTEM.name).orEmpty(),
                )
            }.getOrDefault(ThemeMode.SYSTEM)

        biometric = sp.getBoolean("biometric", false)
    }

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        sp.edit {
            putString("theme", mode.name)
        }
    }

    /**
     * Updates the biometric app-lock preference.
     */
    fun updateBiometric(on: Boolean) {
        biometric = on
        sp.edit {
            putBoolean("biometric", on)
        }
    }
}
