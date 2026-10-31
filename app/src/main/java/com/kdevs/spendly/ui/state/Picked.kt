package com.kdevs.spendly.ui.state

import androidx.navigation.NavController

/**
 * Pickers send their result back to the previous screen through the saved state handle.
 * Keys: pickedColor, pickedIcon, pickedAccount, pickedAccount2, pickedCategory, pickedSub, pickedDate.
 */
fun NavController.setPicked(
    key: String,
    value: Any?,
) {
    previousBackStackEntry?.savedStateHandle?.set(key, value)
}
