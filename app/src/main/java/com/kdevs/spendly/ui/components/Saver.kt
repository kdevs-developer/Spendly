package com.kdevs.spendly.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import kotlinx.coroutines.launch

/** Dismisses a screen after an async save and shows a failure message if it throws. */
@Composable
fun rememberSaver(): Saver = remember { Saver() }

class Saver {
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun run(
        scope: kotlinx.coroutines.CoroutineScope,
        onDone: () -> Unit,
        block: suspend () -> Unit,
    ) {
        if (busy) return
        busy = true
        error = null
        scope.launchSafe(
            onError = {
                error = it.message ?: str(R.string.something_went_wrong_2)
                busy = false
            },
        ) {
            block()
            busy = false
            onDone()
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchSafe(
    onError: (Throwable) -> Unit,
    block: suspend () -> Unit,
) {
    this.launch {
        try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }
}
