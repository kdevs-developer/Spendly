package com.kdevs.spendly.ui.screens.splits

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

@Composable
fun rememberChat(id: String): Chat? {
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    return chat
}

@Composable
fun rememberSplits(id: String): List<SplitEntry> {
    val l by remember(id) { Repo.splits(id) }.collectAsState(initial = emptyList())
    return l
}

fun Chat.nameOf(
    uid: String,
    me: String,
) = if (uid == me) str(R.string.you) else memberNames[uid] ?: str(R.string.member)
