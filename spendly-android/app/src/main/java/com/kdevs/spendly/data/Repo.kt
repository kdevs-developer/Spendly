package com.kdevs.spendly.data

import android.content.Context
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.kdevs.spendly.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * One place for every read and write. Screens only collect the StateFlows below
 * and call the suspend functions. Firestore keeps a local cache, so reads work
 * offline and writes sync when the network is back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object Repo {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var appContext: Context

    fun init(ctx: Context) { appContext = ctx.applicationContext }

    // ---------- Auth ----------
    val uidFlow: StateFlow<String?> = callbackFlow<String?> {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        FirebaseAuth.getInstance().addAuthStateListener(l)
        awaitClose { FirebaseAuth.getInstance().removeAuthStateListener(l) }
    }.stateIn(scope, SharingStarted.Eagerly, FirebaseAuth.getInstance().currentUser?.uid)

    val uid: String get() = auth.currentUser?.uid.orEmpty()
    val isSignedIn: Boolean get() = auth.currentUser != null
    val email: String get() = auth.currentUser?.email.orEmpty()

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        onSignedIn(null)
    }

    suspend fun signUp(name: String, email: String, password: String) {
        val r = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        r.user?.updateProfile(userProfileChangeRequest { displayName = name.trim() })?.await()
        onSignedIn(name.trim())
    }

    suspend fun signInGuest() {
        auth.signInAnonymously().await()
        onSignedIn("Guest")
    }

    /**
     * Google account sign in through the system account sheet. Returns false when the person
     * closed the sheet (nothing to show), and throws a short message for real problems.
     */
    suspend fun signInWithGoogle(context: android.content.Context): Boolean {
        val resId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        if (resId == 0) error("Google sign-in is not set up yet. Turn on Google in Firebase Authentication, add your SHA-1, and download google-services.json again.")
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(appContext.getString(resId))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (e: GetCredentialCancellationException) {
            return false
        } catch (e: NoCredentialException) {
            error("No Google account found on this phone. Add one in your phone settings and try again.")
        }
        val cred = result.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Google sign-in did not return an account. Please try again.")
        }
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onSignedIn(null)
        return true
    }

    suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email.trim()).await() }

    fun signOut() { auth.signOut() }

    /** Deletes all the user's data, then the login itself. */
    suspend fun deleteEverything() {
        val u = uid
        listOf("accounts", "categories", "txns", "budgets", "goals", "templates", "recurring", "friends", "hidden").forEach { c ->
            val snap = db.collection("users").document(u).collection(c).get().await()
            snap.documents.forEach { it.reference.delete().await() }
        }
        profile.value.code.takeIf { it.isNotBlank() }?.let { db.collection("userCodes").document(it.replace(" ", "")).delete().await() }
        db.collection("users").document(u).delete().await()
        auth.currentUser?.delete()?.await()
    }

    private suspend fun onSignedIn(name: String?) {
        ensureProfile(name)
        runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()?.let { saveToken(it) }
    }

    private val profileLock = Mutex()

    /**
     * Makes sure the signed-in person has a name, a friend code (registered for lookup) and the
     * starter categories. Safe to call any number of times: it only fills what is missing, so an
     * account that was cut short the first time is repaired the next time the app opens.
     * [nameHint] is what the person typed on Create account.
     */
    suspend fun ensureProfile(nameHint: String? = null) {
        profileLock.withLock { fillProfile(nameHint) }
    }

    private suspend fun fillProfile(nameHint: String?) {
        if (!isSignedIn) return
        val u = uid
        val ref = db.collection("users").document(u)
        val snap = runCatching { ref.get().await() }.getOrNull() ?: return
        val p = snap.toObject(Profile::class.java) ?: Profile()
        val user = auth.currentUser
        val typed = nameHint?.trim().orEmpty()
        val fallback = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: if (user?.isAnonymous == true) "Guest" else "Me"
        val name = when {
            typed.isNotEmpty() -> typed
            p.name.isNotBlank() -> p.name
            else -> fallback
        }
        val code = p.code.ifBlank { newFriendCode() }

        val changes = mutableMapOf<String, Any>()
        if (name != p.name) changes["name"] = name
        if (code != p.code) changes["code"] = code
        if (changes.isNotEmpty()) ref.set(changes, com.google.firebase.firestore.SetOptions.merge()).await()

        // Friends find you through this lookup document, so keep it in step with the profile.
        val lookup = db.collection("userCodes").document(code.replace(" ", ""))
        val existing = runCatching { lookup.get().await() }.getOrNull()
        if (existing == null || !existing.exists() || existing.getString("name") != name) {
            runCatching { lookup.set(mapOf("uid" to u, "name" to name)).await() }
        }

        // The starter categories are added once. "seeded" is only set after they are all in,
        // so a first run that was cut short tries again instead of leaving an empty list.
        if (!p.seeded) {
            seedCategories()
            ref.set(mapOf("seeded" to true), com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    fun saveToken(token: String) {
        if (!isSignedIn) return
        db.collection("users").document(uid).set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
    }

    private fun newFriendCode(): String {
        val chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val s = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return s.take(4) + " " + s.drop(4)
    }

    private suspend fun seedCategories() {
        val parents = listOf(
            Triple("Food", "food", "orange") to listOf("Groceries" to "grocery", "Eating out" to "food", "Coffee" to "coffee"),
            Triple("Transport", "transport", "blue") to listOf("Fuel" to "fuel", "Bus and metro" to "transport", "Flights" to "flight"),
            Triple("Bills", "bills", "purple") to listOf("Rent" to "rent", "Internet" to "wifi", "Electricity" to "bills"),
            Triple("Shopping", "shopping", "pink") to listOf("Clothes" to "clothes", "Gifts" to "gift"),
            Triple("Health", "health", "red") to listOf("Doctor" to "health", "Fitness" to "fitness"),
            Triple("Entertainment", "movie", "teal") to listOf("Movies" to "movie"),
            Triple("Other", "other", "slate") to emptyList(),
        )
        val col = db.collection("users").document(uid).collection("categories")
        parents.forEach { (p, subs) ->
            val ref = col.document()
            ref.set(Category(name = p.first, icon = p.second, colorKey = p.third, kind = "expense")).await()
            subs.forEach { (n, i) -> col.add(Category(name = n, icon = i, colorKey = p.third, parentId = ref.id, kind = "expense")).await() }
        }
        listOf("Salary" to "salary", "Interest" to "savings", "Gifts received" to "gift", "Other income" to "other").forEach { (n, i) ->
            col.add(Category(name = n, icon = i, colorKey = "green", kind = "income")).await()
        }
    }

    // ---------- Generic helpers ----------
    private fun userCol(name: String): CollectionReference = db.collection("users").document(uid).collection(name)

    private inline fun <reified T : Any> live(name: String, crossinline order: (CollectionReference) -> Query = { it }): Flow<List<T>> =
        uidFlow.flatMapLatest { u ->
            if (u == null) flowOf(emptyList())
            else callbackFlow {
                val q = order(db.collection("users").document(u).collection(name))
                val reg = q.addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(T::class.java)) }
                awaitClose { reg.remove() }
            }
        }

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    /** Save or update. A blank id creates a new document and returns its id. */
    suspend fun put(collection: String, id: String, data: Any): String {
        val ref = if (id.isBlank()) userCol(collection).document() else userCol(collection).document(id)
        ref.set(data).await()
        return ref.id
    }

    suspend fun remove(collection: String, id: String) { userCol(collection).document(id).delete().await() }
    suspend fun removeMany(collection: String, ids: Collection<String>) {
        val batch = db.batch()
        ids.forEach { batch.delete(userCol(collection).document(it)) }
        batch.commit().await()
    }

    // ---------- Money data ----------
    val profile: StateFlow<Profile> = uidFlow.flatMapLatest { u ->
        if (u == null) flowOf(Profile()) else callbackFlow {
            val reg = db.collection("users").document(u).addSnapshotListener { s, _ -> trySend(s?.toObject(Profile::class.java) ?: Profile()) }
            awaitClose { reg.remove() }
        }
    }.hot(Profile())

    val accounts: StateFlow<List<Account>> = live<Account>("accounts") { it.orderBy("createdAt") }.hot(emptyList())
    val categories: StateFlow<List<Category>> = live<Category>("categories").hot(emptyList())
    val txns: StateFlow<List<Txn>> = live<Txn>("txns") { it.orderBy("dateMillis", Query.Direction.DESCENDING) }.hot(emptyList())
    val budgets: StateFlow<List<Budget>> = live<Budget>("budgets").hot(emptyList())
    val goals: StateFlow<List<Goal>> = live<Goal>("goals").hot(emptyList())
    val templates: StateFlow<List<Template>> = live<Template>("templates").hot(emptyList())
    val recurring: StateFlow<List<Recurring>> = live<Recurring>("recurring").hot(emptyList())
    val friends: StateFlow<List<Friend>> = live<Friend>("friends").hot(emptyList())
    private val hiddenIds: StateFlow<List<String>> =
        live<HiddenMsg>("hidden").map { l -> l.map { it.id } }.hot(emptyList())

    /** Money in an account right now: opening balance plus all its transactions. */
    fun balanceOf(accountId: String, accounts: List<Account>, txns: List<Txn>): Long {
        val opening = accounts.firstOrNull { it.id == accountId }?.openingPaise ?: 0
        var b = opening
        for (t in txns) {
            when (t.kind) {
                "income" -> if (t.accountId == accountId) b += t.amountPaise
                "expense" -> if (t.accountId == accountId) b -= t.amountPaise
                "transfer" -> {
                    if (t.accountId == accountId) b -= t.amountPaise
                    if (t.toAccountId == accountId) b += t.amountPaise
                }
            }
        }
        return b
    }

    /** Archive instead of deleting when the account has transactions, so history stays correct. */
    suspend fun deleteAccount(id: String) {
        val used = txns.value.any { it.accountId == id || it.toAccountId == id }
        if (used) {
            val a = accounts.value.firstOrNull { it.id == id } ?: return
            put("accounts", id, a.copy(archived = true))
        } else remove("accounts", id)
    }

    suspend fun deleteCategory(id: String) {
        categories.value.filter { it.parentId == id }.forEach { remove("categories", it.id) }
        remove("categories", id)
    }

    suspend fun updateProfileName(name: String) {
        db.collection("users").document(uid).set(mapOf("name" to name), com.google.firebase.firestore.SetOptions.merge()).await()
        db.collection("userCodes").document(profile.value.code.replace(" ", "")).set(mapOf("uid" to uid, "name" to name)).await()
    }

    /** Creates the next expense for every recurring item that is due. Call when the app opens. */
    suspend fun runRecurring() {
        val now = System.currentTimeMillis()
        for (r in recurring.value.filter { it.active && it.nextMillis in 1..now }) {
            var next = r.nextMillis
            var guard = 0
            while (next <= now && guard++ < 60) {
                put("txns", "", Txn(kind = r.kind, amountPaise = r.amountPaise, accountId = r.accountId, categoryId = r.categoryId, note = r.name, dateMillis = next))
                next = addPeriod(next, r.every)
            }
            put("recurring", r.id, r.copy(nextMillis = next))
        }
    }

    // ---------- Friends ----------
    data class RequestView(val id: String, val fromUid: String, val fromName: String)

    val incomingRequests: StateFlow<List<FriendRequest>> = uidFlow.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else callbackFlow {
            val reg = db.collection("requests").whereEqualTo("to", u).whereEqualTo("status", "pending")
                .addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(FriendRequest::class.java)) }
            awaitClose { reg.remove() }
        }
    }.hot(emptyList())

    val outgoingRequests: StateFlow<List<FriendRequest>> = uidFlow.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else callbackFlow {
            val reg = db.collection("requests").whereEqualTo("from", u).whereEqualTo("status", "pending")
                .addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(FriendRequest::class.java)) }
            awaitClose { reg.remove() }
        }
    }.hot(emptyList())

    /** Returns null on success or a short message the screen can show. */
    suspend fun sendFriendRequest(rawCode: String): String? {
        val code = rawCode.uppercase().filter { it.isLetterOrDigit() }
        if (code.length != 8) return "A friend code has 8 letters and numbers."
        val doc = db.collection("userCodes").document(code).get().await()
        if (!doc.exists()) return "No one has that code."
        val other = doc.getString("uid").orEmpty()
        if (other == uid) return "That is your own code."
        if (friends.value.any { it.id == other }) return "Already friends."
        if (other in profile.value.blocked) return "Unblock this person first."
        val id = "${uid}_$other"
        db.collection("requests").document(id).set(
            FriendRequest(from = uid, fromName = profile.value.name, to = other, status = "pending")
        ).await()
        return null
    }

    suspend fun answerRequest(req: FriendRequest, accept: Boolean) {
        db.collection("requests").document(req.id).update("status", if (accept) "accepted" else "declined").await()
        if (accept) {
            val code = db.collection("users").document(req.from).get().await().getString("code").orEmpty()
            put("friends", req.from, Friend(name = req.fromName, code = code))
            // The sender adds us on their side when they next open the app (see syncAccepted).
        }
    }

    /** Adds friends whose requests we sent and they accepted. */
    suspend fun syncAccepted() {
        val snap = db.collection("requests").whereEqualTo("from", uid).whereEqualTo("status", "accepted").get().await()
        for (d in snap.documents) {
            val other = d.getString("to").orEmpty()
            if (friends.value.none { it.id == other }) {
                val u = db.collection("users").document(other).get().await()
                put("friends", other, Friend(name = u.getString("name").orEmpty(), code = u.getString("code").orEmpty()))
            }
            d.reference.delete().await()
        }
    }

    suspend fun removeFriend(id: String) { remove("friends", id) }

    suspend fun block(id: String) {
        db.collection("users").document(uid).update("blocked", FieldValue.arrayUnion(id)).await()
    }
    suspend fun unblock(id: String) {
        db.collection("users").document(uid).update("blocked", FieldValue.arrayRemove(id)).await()
    }

    suspend fun report(targetUid: String, chatId: String, reason: String) {
        db.collection("reports").add(
            mapOf("reporter" to uid, "target" to targetUid, "chatId" to chatId, "reason" to reason, "at" to Timestamp.now())
        ).await()
    }

    // ---------- Chats ----------
    val chats: StateFlow<List<Chat>> = uidFlow.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else callbackFlow {
            val reg = db.collection("chats").whereArrayContains("members", u)
                .addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(Chat::class.java).sortedByDescending { it.lastAt?.toDate()?.time ?: Long.MAX_VALUE }) }
            awaitClose { reg.remove() }
        }
    }.hot(emptyList())

    fun chat(id: String): Flow<Chat?> = callbackFlow {
        val reg = db.collection("chats").document(id).addSnapshotListener { s, _ -> trySend(s?.toObject(Chat::class.java)) }
        awaitClose { reg.remove() }
    }

    suspend fun openDirectChat(friend: Friend): String {
        val id = listOf(uid, friend.id).sorted().joinToString("_")
        val ref = db.collection("chats").document(id)
        val existing = runCatching { ref.get().await() }.getOrNull()
        if (existing == null || !existing.exists()) {
            ref.set(
                Chat(type = "direct", name = "", members = listOf(uid, friend.id),
                    memberNames = mapOf(uid to profile.value.name, friend.id to friend.name), createdBy = uid)
            ).await()
        }
        return id
    }

    /** type is "group" or "split". */
    suspend fun createGroup(name: String, type: String, members: List<Friend>): String {
        val ref = db.collection("chats").document()
        val names = mutableMapOf(uid to profile.value.name)
        members.forEach { names[it.id] = it.name }
        ref.set(Chat(type = type, name = name, members = listOf(uid) + members.map { it.id }, memberNames = names, createdBy = uid)).await()
        sendSystem(ref.id, "${profile.value.name} created the group")
        return ref.id
    }

    suspend fun addMembers(chatId: String, add: List<Friend>) {
        val ref = db.collection("chats").document(chatId)
        val updates = mutableMapOf<String, Any>("members" to FieldValue.arrayUnion(*add.map { it.id }.toTypedArray()))
        add.forEach { updates["memberNames.${it.id}"] = it.name }
        ref.update(updates).await()
        add.forEach { sendSystem(chatId, "${profile.value.name} added ${it.name}") }
    }

    suspend fun leaveChat(chatId: String) {
        val ref = db.collection("chats").document(chatId)
        ref.update("members", FieldValue.arrayRemove(uid)).await()
    }

    suspend fun renameChat(chatId: String, name: String) {
        db.collection("chats").document(chatId).update("name", name).await()
    }

    fun messages(chatId: String): Flow<List<Message>> =
        combine(
            callbackFlow {
                val reg = db.collection("chats").document(chatId).collection("messages")
                    .orderBy("createdAt").addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(Message::class.java)) }
                awaitClose { reg.remove() }
            },
            hiddenIds,
            profile,
        ) { list, hidden, p -> list.filter { it.id !in hidden && it.senderId !in p.blocked } }

    suspend fun sendText(chatId: String, text: String, replyTo: Message? = null) {
        val msg = Message(
            senderId = uid, senderName = profile.value.name, type = "text", text = text.trim(),
            replyToId = replyTo?.id.orEmpty(),
            replyPreview = replyTo?.let { if (it.type == "image") "Photo" else it.text.take(80) }.orEmpty(),
            readBy = listOf(uid),
        )
        val chatRef = db.collection("chats").document(chatId)
        chatRef.collection("messages").add(msg).await()
        chatRef.update(mapOf("lastMessage" to text.trim().take(80), "lastAt" to FieldValue.serverTimestamp(), "typing.$uid" to 0L)).await()
    }

    suspend fun sendImage(chatId: String, uri: Uri) {
        val path = "chats/$chatId/${System.currentTimeMillis()}_$uid.jpg"
        val ref = FirebaseStorage.getInstance().reference.child(path)
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        val chatRef = db.collection("chats").document(chatId)
        chatRef.collection("messages").add(
            Message(senderId = uid, senderName = profile.value.name, type = "image", imageUrl = url, readBy = listOf(uid))
        ).await()
        chatRef.update(mapOf("lastMessage" to "Photo", "lastAt" to FieldValue.serverTimestamp())).await()
    }

    private suspend fun sendSystem(chatId: String, text: String) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.collection("messages").add(Message(senderId = uid, senderName = "", type = "system", text = text, readBy = listOf(uid))).await()
        chatRef.update(mapOf("lastMessage" to text, "lastAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun react(chatId: String, msg: Message, key: String) {
        val ref = db.collection("chats").document(chatId).collection("messages").document(msg.id)
        if (msg.reactions[uid] == key) ref.update("reactions.$uid", FieldValue.delete()).await()
        else ref.update("reactions.$uid", key).await()
    }

    suspend fun markRead(chatId: String, msgs: List<Message>) {
        val unread = msgs.filter { it.senderId != uid && uid !in it.readBy && it.type != "system" }
        if (unread.isEmpty()) return
        val batch = db.batch()
        unread.forEach {
            batch.update(db.collection("chats").document(chatId).collection("messages").document(it.id), "readBy", FieldValue.arrayUnion(uid))
        }
        batch.commit().await()
    }

    fun setTyping(chatId: String, typing: Boolean) {
        db.collection("chats").document(chatId).update("typing.$uid", if (typing) System.currentTimeMillis() else 0L)
    }

    /** Only works within 5 minutes of sending; Firestore rules enforce the same limit. */
    suspend fun deleteForEveryone(chatId: String, msg: Message) {
        db.collection("chats").document(chatId).collection("messages").document(msg.id)
            .update(mapOf("deleted" to true, "text" to "", "imageUrl" to "")).await()
    }

    suspend fun deleteForMe(msg: Message) { put("hidden", msg.id, HiddenMsg()) }

    // ---------- Splits ----------
    fun splits(chatId: String): Flow<List<SplitEntry>> = callbackFlow {
        val reg = db.collection("chats").document(chatId).collection("splits")
            .orderBy("dateMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(SplitEntry::class.java)) }
        awaitClose { reg.remove() }
    }

    suspend fun addSplit(chatId: String, e: SplitEntry) {
        val ref = db.collection("chats").document(chatId)
        ref.collection("splits").add(e.copy(createdBy = uid)).await()
        val text = if (e.kind == "settlement") "Settled ${formatPaise(e.totalPaise)}" else "${e.description}: ${formatPaise(e.totalPaise)}"
        sendSystem(chatId, text)
    }

    suspend fun deleteSplit(chatId: String, id: String) {
        db.collection("chats").document(chatId).collection("splits").document(id).delete().await()
    }
}

/** Marker document for "delete for me". Its id is the message id. */
data class HiddenMsg(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val at: Long = 0,
)
