package com.kdevs.spendly.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * One message at the bottom of the screen, optionally with an Undo button.
 * [offer] is used after a delete (Undo puts the data back); [say] is a plain notice with no button.
 */
object Undo {
    class Item(
        val message: String,
        val restore: (suspend () -> Unit)?,
        val key: Long = System.nanoTime(),
    )

    val current = MutableStateFlow<Item?>(null)

    fun offer(
        message: String,
        restore: suspend () -> Unit,
    ) {
        current.value = Item(message, restore)
    }

    fun say(message: String) {
        current.value = Item(message, null)
    }
}
