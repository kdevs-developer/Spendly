package com.kdevs.spendly.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

// All money is stored as whole paise (Long). Never Double.

data class Account(
    @DocumentId val id: String = "",
    val name: String = "",
    val type: String = "bank", // bank | wallet | cash | card
    val icon: String = "bank",
    val colorKey: String = "blue",
    val openingPaise: Long = 0,
    val archived: Boolean = false,
    val createdAt: Long = 0,
)

data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val icon: String = "other",
    val colorKey: String = "slate",
    val parentId: String = "", // empty = top-level category
    val kind: String = "expense", // expense | income
)

data class Txn(
    @DocumentId val id: String = "",
    val kind: String = "expense", // expense | income | transfer
    val amountPaise: Long = 0,
    val accountId: String = "",
    val toAccountId: String = "", // transfers only
    val categoryId: String = "",
    val subcategoryId: String = "",
    val note: String = "",
    val dateMillis: Long = 0,
)

data class Budget(
    @DocumentId val id: String = "",
    val categoryId: String = "", // empty = overall monthly budget
    val limitPaise: Long = 0,
)

data class Goal(
    @DocumentId val id: String = "",
    val name: String = "",
    val icon: String = "goal",
    val colorKey: String = "green",
    val targetPaise: Long = 0,
    val savedPaise: Long = 0,
    val deadlineMillis: Long = 0,
)

data class Template(
    @DocumentId val id: String = "",
    val name: String = "",
    val kind: String = "expense",
    val amountPaise: Long = 0,
    val accountId: String = "",
    val categoryId: String = "",
    val note: String = "",
)

data class Recurring(
    @DocumentId val id: String = "",
    val name: String = "",
    val kind: String = "expense",
    val amountPaise: Long = 0,
    val accountId: String = "",
    val categoryId: String = "",
    val subcategoryId: String = "",
    val every: String = "monthly", // daily | weekly | monthly | yearly
    val interval: Int = 1, // repeat every N units of [every]; 1 = plain daily/weekly/monthly/yearly
    val nextMillis: Long = 0,
    val active: Boolean = true,
    val anchorDay: Int = 0,
)

data class Friend(
    @DocumentId val id: String = "", // friend's uid
    val name: String = "",
    val code: String = "",
)

data class FriendRequest(
    @DocumentId val id: String = "",
    val from: String = "",
    val fromName: String = "",
    val fromCode: String = "", // sender's friend code, so the receiver need not read the sender's profile
    val to: String = "",
    val toName: String = "", // receiver's name and code, copied from the lookup document when sending
    val toCode: String = "",
    val status: String = "pending", // pending | accepted | declined
)

data class Chat(
    @DocumentId val id: String = "",
    val type: String = "direct", // direct | group | split
    val name: String = "",
    val members: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    @ServerTimestamp val lastAt: Timestamp? = null,
    val typing: Map<String, Long> = emptyMap(), // uid -> last typing millis
    val createdBy: String = "",
    val lastSenderId: String = "", // who wrote the last message, so other phones can notify
    val lastSenderName: String = "",
    val unread: Map<String, Long> = emptyMap(), // uid -> messages that person has not opened yet
)

data class Message(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "text", // text | image | video | system
    val text: String = "",
    val imageUrl: String = "", // download link for both photos and videos
    val deliveredTo: List<String> = emptyList(), // phones that have received the message
    val replyToId: String = "",
    val replyPreview: String = "",
    val reactions: Map<String, String> = emptyMap(), // uid -> reaction key
    val readBy: List<String> = emptyList(),
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    /** True while the message is only on this phone and not yet confirmed by the server. Never stored. */
    @get:Exclude var pending: Boolean = false
}

data class SplitEntry(
    @DocumentId val id: String = "",
    val kind: String = "expense", // expense | settlement
    val description: String = "",
    val totalPaise: Long = 0,
    val paidBy: String = "",
    val settleTo: String = "", // settlements only
    val splitType: String = "equal", // equal | exact | percent | shares
    val owed: Map<String, Long> = emptyMap(), // uid -> paise owed
    val dateMillis: Long = 0,
    val createdBy: String = "",
)

data class Profile(
    val name: String = "",
    val code: String = "",
    val seeded: Boolean = false,
    val blocked: List<String> = emptyList(),
)

/** Marker document for "delete chat". Its id is the chat id; [at] is when it was deleted. */
data class HiddenChat(
    @DocumentId val id: String = "",
    val at: Long = 0,
)

/** Marker document for "delete for me". Its id is the message id. */
data class HiddenMsg(
    @DocumentId val id: String = "",
    val at: Long = 0,
)
