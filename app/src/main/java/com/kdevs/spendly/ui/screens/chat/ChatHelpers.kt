package com.kdevs.spendly.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

fun Chat.title(me: String): String =
    if (type == "direct") {
        memberNames.entries.firstOrNull {
            it.key != me
        }?.value ?: str(R.string.chat)
    } else {
        name.ifBlank { str(R.string.group) }
    }

fun Chat.otherId(me: String): String = members.firstOrNull { it != me } ?: ""

fun Message.millis(): Long = createdAt?.toDate()?.time ?: System.currentTimeMillis()

/** Pending = still on this phone. Sent = on the server. Delivered = on every other phone. Read = opened by everyone. */
enum class Delivery { Pending, Sent, Delivered, Read }

fun Message.delivery(
    me: String,
    members: List<String>,
): Delivery {
    if (pending) return Delivery.Pending
    val others = members.filter { it != me }
    if (others.isEmpty()) return Delivery.Sent
    if (others.all { it in readBy }) return Delivery.Read
    if (others.all { it in deliveredTo || it in readBy }) return Delivery.Delivered
    return Delivery.Sent
}

/** Reaction keys and their icons. Drawn as icons, not emoji, so they match the app. */
val reactionIcons =
    listOf(
        Triple("like", Icons.Filled.ThumbUp, clr(R.color.swatch_blue)),
        Triple("love", Icons.Filled.Favorite, clr(R.color.swatch_red)),
        Triple("laugh", Icons.Filled.SentimentVerySatisfied, clr(R.color.swatch_amber)),
        Triple("wow", Icons.Filled.Bolt, clr(R.color.swatch_amber)),
        Triple("sad", Icons.Filled.SentimentDissatisfied, clr(R.color.swatch_amber)),
        Triple("star", Icons.Filled.Star, clr(R.color.swatch_amber)),
    )
