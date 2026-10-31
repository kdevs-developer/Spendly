package com.kdevs.spendly.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

/** Multi-select used by the Select screen and Bulk actions screen. */
object Selection {
    val ids = mutableStateListOf<String>()

    fun toggle(id: String) {
        if (id in ids) ids.remove(id) else ids.add(id)
    }

    fun clear() = ids.clear()
}
