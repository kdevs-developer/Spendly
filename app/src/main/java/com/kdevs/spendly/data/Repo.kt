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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.kdevs.spendly.R
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
import kotlinx.coroutines.withTimeout
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

    fun init(ctx: Context) {
        appContext = ctx.applicationContext
    }

    // ---------- Auth ----------
    val uidFlow: StateFlow<String?> =
        callbackFlow<String?> {
            val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
            FirebaseAuth.getInstance().addAuthStateListener(l)
            awaitClose { FirebaseAuth.getInstance().removeAuthStateListener(l) }
        }.stateIn(scope, SharingStarted.Eagerly, FirebaseAuth.getInstance().currentUser?.uid)

    val uid: String get() = auth.currentUser?.uid.orEmpty()
    val isSignedIn: Boolean get() = auth.currentUser != null
    val email: String get() = auth.currentUser?.email.orEmpty()

    suspend fun signIn(
        email: String,
        password: String,
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        onSignedIn(null)
    }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ) {
        val r = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        r.user?.updateProfile(userProfileChangeRequest { displayName = name.trim() })?.await()
        onSignedIn(name.trim())
    }

    suspend fun signInGuest() {
        auth.signInAnonymously().await()
        onSignedIn(str(R.string.guest))
    }

    /**
     * Opens the system Google account sheet and returns a Firebase credential for the chosen account.
     * Returns null when the person closed the sheet (nothing to show), and throws a short message for real problems.
     */
    private suspend fun fetchGoogleCredential(context: android.content.Context): AuthCredential? {
        val resId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        if (resId == 0) error(str(R.string.google_sign_in_is_not_set))
        val option =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(appContext.getString(resId))
                .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result =
            try {
                CredentialManager.create(context).getCredential(context, request)
            } catch (e: GetCredentialCancellationException) {
                return null
            } catch (e: NoCredentialException) {
                error(str(R.string.no_google_account_found_on_this))
            }
        val cred = result.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error(str(R.string.google_sign_in_did_not_return))
        }
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        return GoogleAuthProvider.getCredential(token, null)
    }

    /** Google account sign in. Returns false when the person closed the sheet. */
    suspend fun signInWithGoogle(context: android.content.Context): Boolean {
        val credential = fetchGoogleCredential(context) ?: return false
        auth.signInWithCredential(credential).await()
        onSignedIn(null)
        return true
    }

    /** Which login methods this account uses: "password", "google.com", or empty for a guest. */
    val signInMethods: List<String> get() = auth.currentUser?.providerData.orEmpty().map { it.providerId }.filter { it != "firebase" }
    val isGuest: Boolean get() = auth.currentUser?.isAnonymous == true
    val usesPassword: Boolean get() = "password" in signInMethods

    /**
     * Asks Firebase to send the reset email. Firebase answers "ok" even when the address has no account
     * (email enumeration protection), so the screen must not promise that a mail exists.
     */
    suspend fun resetPassword(email: String) {
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            error(str(R.string.that_email_address_does_not_look))
        } catch (e: FirebaseAuthInvalidUserException) {
            error(str(R.string.no_account_uses_that_email))
        } catch (e: FirebaseTooManyRequestsException) {
            error(str(R.string.too_many_attempts_wait_a_few))
        } catch (e: FirebaseNetworkException) {
            error(str(R.string.no_internet_connection))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /** True while the delete flow runs, so the profile repair below does not rebuild what was just removed. */
    @Volatile private var deleting = false

    /**
     * Firebase only lets an account be deleted shortly after a fresh sign-in. Call this first and the
     * delete below cannot stop half way. Password accounts pass their password; Google accounts pass null
     * and get the Google sheet; guests need nothing.
     */
    suspend fun reauthenticate(
        password: String?,
        context: Context,
    ) {
        val user = auth.currentUser ?: error(str(R.string.you_are_signed_out))
        if (user.isAnonymous) return
        val credential: AuthCredential =
            when {
                "password" in signInMethods -> {
                    val mail = user.email ?: error(str(R.string.this_account_has_no_email))
                    if (password.isNullOrEmpty()) error(str(R.string.enter_your_password))
                    EmailAuthProvider.getCredential(mail, password)
                }
                "google.com" in signInMethods -> fetchGoogleCredential(context) ?: error(str(R.string.google_confirmation_was_cancelled))
                else -> error(str(R.string.sign_out_sign_in_again_then))
            }
        try {
            user.reauthenticate(credential).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            error(str(R.string.those_details_do_not_match_this))
        } catch (e: FirebaseAuthInvalidUserException) {
            error(str(R.string.those_details_do_not_match_this))
        }
    }

    /**
     * Removes everything the person owns, then the login. Needs the internet, because a delete that is only
     * queued on the phone would "finish" without touching the server. Call [reauthenticate] first.
     * What stays: messages already sent in other people's chats, and split entries other members rely on.
     */
    suspend fun deleteEverything() {
        val u = uid
        if (u.isBlank()) return
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val online =
            cm.activeNetwork?.let {
                cm.getNetworkCapabilities(it)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } == true
        if (!online) error(str(R.string.connect_to_the_internet_to_delete))
        deleting = true
        try {
            withTimeout(120_000) { wipe(u) }
            runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            try {
                auth.currentUser?.delete()?.await()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                error(str(R.string.for_your_security_confirm_your_login))
            }
        } finally {
            deleting = false
        }
    }

    private suspend fun wipe(u: String) {
        // 1. Chats: take my name off, leave, and delete the photos I uploaded.
        val chatDocs = db.collection("chats").whereArrayContains("members", u).get(Source.SERVER).await().documents
        for (c in chatDocs) {
            runCatching {
                FirebaseStorage.getInstance().reference.child("chats/${c.id}").listAll().await()
                    .items.filter { it.name.endsWith("_$u.jpg") }.forEach { it.delete().await() }
            }
            c.reference.update(mapOf("members" to FieldValue.arrayRemove(u), "memberNames.$u" to "Deleted user")).await()
        }
        // 2. Friend requests I sent or received.
        for (side in listOf("from", "to")) {
            db.collection("requests").whereEqualTo(side, u).get(Source.SERVER).await().documents.forEach { it.reference.delete().await() }
        }
        // 3. My private collections, in batches (a batch holds at most 500 writes).
        val me = db.collection("users").document(u)
        for (name in listOf("accounts", "categories", "txns", "budgets", "goals", "templates", "recurring", "friends", "hidden")) {
            me.collection(name).get(Source.SERVER).await().documents.chunked(400).forEach { part ->
                val batch = db.batch()
                part.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        // 4. The friend code lookup, then the profile itself.
        val code = me.get(Source.SERVER).await().getString("code").orEmpty().replace(" ", "")
        if (code.isNotBlank()) db.collection("userCodes").document(code).delete().await()
        me.delete().await()
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
        if (!isSignedIn || deleting) return
        val u = uid
        val ref = db.collection("users").document(u)
        val snap = runCatching { ref.get().await() }.getOrNull() ?: return
        val p = snap.toObject(Profile::class.java) ?: Profile()
        val user = auth.currentUser
        val typed = nameHint?.trim().orEmpty()
        val fallback =
            user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                ?: if (user?.isAnonymous == true) str(R.string.guest) else str(R.string.me)
        val name =
            when {
                typed.isNotEmpty() -> typed
                p.name.isNotBlank() -> p.name
                else -> fallback
            }

        // Friends find you through the userCodes lookup document. Register it first and only then save the
        // code on the profile, so the screen never shows a code that nobody can look up.
        val code =
            try {
                claimCode(u, name, p.code).also { codeProblem.value = null }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                codeProblem.value = friendlyFirestoreError(e)
                null
            }

        val changes = mutableMapOf<String, Any>()
        if (name != p.name) changes["name"] = name
        if (code != null && code != p.code) changes["code"] = code
        if (changes.isNotEmpty()) ref.set(changes, SetOptions.merge()).await()

        // The starter categories are added once. "seeded" is only set after they are all in,
        // so a first run that was cut short tries again instead of leaving an empty list.
        if (!p.seeded) {
            seedCategories()
            ref.set(mapOf("seeded" to true), com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    /** Set when the friend code could not be registered on the server (rules, network). Shown on Add friend. */
    val codeProblem = MutableStateFlow<String?>(null)

    private fun friendlyFirestoreError(e: Exception): String =
        if (e is FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> str(R.string.the_server_refused_this_deploy_firesto)
                FirebaseFirestoreException.Code.UNAVAILABLE -> str(R.string.no_internet_connection_try_again_when)
                else -> str(R.string.server_error, e.code)
            }
        } else {
            e.message ?: str(R.string.something_went_wrong)
        }

    /** Registers the code for lookup and returns the code to keep. Picks a new one if someone else holds it. */
    private suspend fun claimCode(
        u: String,
        name: String,
        current: String,
    ): String {
        var code = current.ifBlank { newFriendCode() }
        repeat(6) {
            val ref = db.collection("userCodes").document(code.replace(" ", ""))
            val snap = ref.get(Source.SERVER).await()
            if (!snap.exists() || snap.getString("uid") == u) {
                if (!snap.exists() || snap.getString("name") != name) ref.set(mapOf("uid" to u, "name" to name)).await()
                return code
            }
            code = newFriendCode()
        }
        error(str(R.string.could_not_create_a_friend_code))
    }

    /** Saves this phone's push token. Called on every app start, because a token created while signed out would be lost. */
    suspend fun refreshToken() {
        if (!isSignedIn) return
        FirebaseMessaging.getInstance().token.await().let { saveToken(it) }
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
        val parents =
            listOf(
                Triple(str(R.string.food), "food", "orange") to listOf(str(R.string.groceries) to "grocery", str(R.string.eating_out) to "food", str(R.string.coffee) to "coffee"),
                Triple(str(R.string.transport), "transport", "blue") to listOf(str(R.string.fuel) to "fuel", str(R.string.bus_and_metro) to "transport", str(R.string.flights) to "flight"),
                Triple(str(R.string.bills), "bills", "purple") to listOf(str(R.string.rent) to "rent", str(R.string.internet) to "wifi", str(R.string.electricity) to "bills"),
                Triple(str(R.string.shopping), "shopping", "pink") to listOf(str(R.string.clothes) to "clothes", str(R.string.gifts) to "gift"),
                Triple(str(R.string.health), "health", "red") to listOf(str(R.string.doctor) to "health", str(R.string.fitness) to "fitness"),
                Triple(str(R.string.entertainment), "movie", "teal") to listOf(str(R.string.movies) to "movie"),
                Triple(str(R.string.other), "other", "slate") to emptyList(),
            )
        val col = db.collection("users").document(uid).collection("categories")
        parents.forEach { (p, subs) ->
            val ref = col.document()
            ref.set(Category(name = p.first, icon = p.second, colorKey = p.third, kind = "expense")).await()
            subs.forEach {
                    (n, i) ->
                col.add(Category(name = n, icon = i, colorKey = p.third, parentId = ref.id, kind = "expense")).await()
            }
        }
        listOf(
            str(R.string.salary) to "salary",
            str(R.string.interest) to "savings",
            str(R.string.gifts_received) to "gift",
            str(R.string.other_income) to "other",
        ).forEach {
                (n, i) ->
            col.add(Category(name = n, icon = i, colorKey = "green", kind = "income")).await()
        }
    }

    // ---------- Generic helpers ----------
    private fun userCol(name: String): CollectionReference = db.collection("users").document(uid).collection(name)

    private inline fun <reified T : Any> live(
        name: String,
        crossinline order: (CollectionReference) -> Query = { it },
    ): Flow<List<T>> =
        uidFlow.flatMapLatest { u ->
            if (u == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val q = order(db.collection("users").document(u).collection(name))
                    val reg = q.addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(T::class.java)) }
                    awaitClose { reg.remove() }
                }
            }
        }

    private fun <T> Flow<T>.hot(initial: T): StateFlow<T> = stateIn(scope, SharingStarted.Eagerly, initial)

    /** Save or update. A blank id creates a new document and returns its id. */
    suspend fun put(
        collection: String,
        id: String,
        data: Any,
    ): String {
        val ref = if (id.isBlank()) userCol(collection).document() else userCol(collection).document(id)
        ref.set(data).await()
        return ref.id
    }

    suspend fun remove(
        collection: String,
        id: String,
    ) {
        userCol(collection).document(id).delete().await()
    }

    suspend fun removeMany(
        collection: String,
        ids: Collection<String>,
    ) {
        val batch = db.batch()
        ids.forEach { batch.delete(userCol(collection).document(it)) }
        batch.commit().await()
    }

    // ---------- Money data ----------
    val profile: StateFlow<Profile> =
        uidFlow.flatMapLatest { u ->
            if (u == null) {
                flowOf(Profile())
            } else {
                callbackFlow {
                    val reg =
                        db.collection("users").document(u).addSnapshotListener {
                                s,
                                _,
                            ->
                            trySend(s?.toObject(Profile::class.java) ?: Profile())
                        }
                    awaitClose { reg.remove() }
                }
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
    fun balanceOf(
        accountId: String,
        accounts: List<Account>,
        txns: List<Txn>,
    ): Long {
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
        } else {
            remove("accounts", id)
        }
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
                put(
                    "txns",
                    "",
                    Txn(
                        kind = r.kind,
                        amountPaise = r.amountPaise,
                        accountId = r.accountId,
                        categoryId = r.categoryId,
                        note = r.name,
                        dateMillis = next,
                    ),
                )
                next = addPeriod(next, r.every)
            }
            put("recurring", r.id, r.copy(nextMillis = next))
        }
    }

    // ---------- Friends ----------
    data class RequestView(val id: String, val fromUid: String, val fromName: String)

    val incomingRequests: StateFlow<List<FriendRequest>> =
        uidFlow.flatMapLatest { u ->
            if (u == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val reg =
                        db.collection("requests").whereEqualTo("to", u).whereEqualTo("status", "pending")
                            .addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(FriendRequest::class.java)) }
                    awaitClose { reg.remove() }
                }
            }
        }.hot(emptyList())

    val outgoingRequests: StateFlow<List<FriendRequest>> =
        uidFlow.flatMapLatest { u ->
            if (u == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val reg =
                        db.collection("requests").whereEqualTo("from", u).whereEqualTo("status", "pending")
                            .addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(FriendRequest::class.java)) }
                    awaitClose { reg.remove() }
                }
            }
        }.hot(emptyList())

    /** Returns null on success or a short message the screen can show. */
    suspend fun sendFriendRequest(rawCode: String): String? {
        val code = rawCode.uppercase().filter { it.isLetterOrDigit() }
        if (code.length != 8) return str(R.string.friend_code_length_hint)
        val doc =
            try {
                db.collection("userCodes").document(code).get(Source.SERVER).await()
            } catch (e: FirebaseFirestoreException) {
                return friendlyFirestoreError(e)
            }
        if (!doc.exists()) return str(R.string.no_one_has_that_code)
        val other = doc.getString("uid").orEmpty()
        if (other == uid) return str(R.string.that_is_your_own_code)
        if (friends.value.any { it.id == other }) return str(R.string.already_friends)
        if (other in profile.value.blocked) return str(R.string.unblock_this_person_first)
        val id = "${uid}_$other"
        db.collection("requests").document(id).set(
            FriendRequest(
                from = uid,
                fromName = profile.value.name,
                fromCode = profile.value.code,
                to = other,
                toName = doc.getString("name").orEmpty(),
                toCode = code,
                status = "pending",
            ),
        ).await()
        return null
    }

    suspend fun answerRequest(
        req: FriendRequest,
        accept: Boolean,
    ) {
        // Add the friend first: if the second step fails the person can simply tap Accept again.
        if (accept) put("friends", req.from, Friend(name = req.fromName, code = req.fromCode))
        db.collection("requests").document(req.id).update("status", if (accept) "accepted" else "declined").await()
        // The sender adds us on their side as soon as they are online (see syncAccepted).
    }

    /** Adds friends whose requests we sent and they accepted. */
    suspend fun syncAccepted() {
        val snap = db.collection("requests").whereEqualTo("from", uid).whereEqualTo("status", "accepted").get().await()
        for (d in snap.documents) {
            val other = d.getString("to").orEmpty()
            // Another person's profile is private, so the name and code come from the request itself.
            if (friends.value.none { it.id == other }) {
                put(
                    "friends",
                    other,
                    Friend(
                        name =
                            d.getString("toName").orEmpty().ifBlank {
                                str(R.string.friend)
                            },
                        code = d.getString("toCode").orEmpty(),
                    ),
                )
            }
            d.reference.delete().await()
        }
    }

    /** Withdraws a request we sent. */
    suspend fun cancelRequest(req: FriendRequest) {
        db.collection("requests").document(req.id).delete().await()
    }

    suspend fun removeFriend(id: String) {
        remove("friends", id)
    }

    suspend fun block(id: String) {
        db.collection("users").document(uid).update("blocked", FieldValue.arrayUnion(id)).await()
    }

    suspend fun unblock(id: String) {
        db.collection("users").document(uid).update("blocked", FieldValue.arrayRemove(id)).await()
    }

    suspend fun report(
        targetUid: String,
        chatId: String,
        reason: String,
    ) {
        db.collection("reports").add(
            mapOf("reporter" to uid, "target" to targetUid, "chatId" to chatId, "reason" to reason, "at" to Timestamp.now()),
        ).await()
    }

    // ---------- Chats ----------
    val chats: StateFlow<List<Chat>> =
        uidFlow.flatMapLatest { u ->
            if (u == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val reg =
                        db.collection("chats").whereArrayContains("members", u)
                            .addSnapshotListener {
                                    s,
                                    _,
                                ->
                                if (s != null) {
                                    trySend(
                                        s.toObjects(Chat::class.java).sortedByDescending { it.lastAt?.toDate()?.time ?: Long.MAX_VALUE },
                                    )
                                }
                            }
                    awaitClose { reg.remove() }
                }
            }
        }.hot(emptyList())

    fun chat(id: String): Flow<Chat?> =
        callbackFlow {
            val reg = db.collection("chats").document(id).addSnapshotListener { s, _ -> trySend(s?.toObject(Chat::class.java)) }
            awaitClose { reg.remove() }
        }

    suspend fun openDirectChat(friend: Friend): String {
        val id = listOf(uid, friend.id).sorted().joinToString("_")
        val ref = db.collection("chats").document(id)
        val existing = runCatching { ref.get().await() }.getOrNull()
        if (existing == null || !existing.exists()) {
            ref.set(
                Chat(
                    type = "direct",
                    name = "",
                    members = listOf(uid, friend.id),
                    memberNames = mapOf(uid to profile.value.name, friend.id to friend.name),
                    createdBy = uid,
                ),
            ).await()
        }
        return id
    }

    /** type is "group" or "split". */
    suspend fun createGroup(
        name: String,
        type: String,
        members: List<Friend>,
    ): String {
        val ref = db.collection("chats").document()
        val names = mutableMapOf(uid to profile.value.name)
        members.forEach { names[it.id] = it.name }
        ref.set(Chat(type = type, name = name, members = listOf(uid) + members.map { it.id }, memberNames = names, createdBy = uid)).await()
        sendSystem(ref.id, "${profile.value.name} created the group")
        return ref.id
    }

    suspend fun addMembers(
        chatId: String,
        add: List<Friend>,
    ) {
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

    suspend fun renameChat(
        chatId: String,
        name: String,
    ) {
        db.collection("chats").document(chatId).update("name", name).await()
    }

    fun messages(chatId: String): Flow<List<Message>> =
        combine(
            callbackFlow {
                val reg =
                    db.collection("chats").document(chatId).collection("messages")
                        .orderBy("createdAt")
                        // Metadata changes tell us when a message moves from "on this phone" to "on the server".
                        .addSnapshotListener(MetadataChanges.INCLUDE) { s, _ ->
                            if (s != null) {
                                trySend(
                                    s.documents.mapNotNull { d ->
                                        d.toObject(Message::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                                            ?.also { it.pending = d.metadata.hasPendingWrites() }
                                    },
                                )
                            }
                        }
                awaitClose { reg.remove() }
            },
            hiddenIds,
            profile,
        ) { list, hidden, p -> list.filter { it.id !in hidden && it.senderId !in p.blocked } }

    /** The fields every new message sets on the chat: preview, sender, time, and +1 unread for everyone else. */
    private fun chatBump(
        chatId: String,
        preview: String,
    ): Map<String, Any> {
        val members = chats.value.firstOrNull { it.id == chatId }?.members.orEmpty()
        val m =
            mutableMapOf<String, Any>(
                "lastMessage" to preview,
                "lastAt" to FieldValue.serverTimestamp(),
                "lastSenderId" to uid,
                "lastSenderName" to profile.value.name,
                "typing.$uid" to 0L,
            )
        members.filter { it != uid }.forEach { m["unread.$it"] = FieldValue.increment(1) }
        return m
    }

    /**
     * Writes are not awaited: Firestore keeps them on the phone and sends them when online, so a message
     * shows at once with a clock icon (pending), then a tick (sent) when the server has it.
     */
    fun sendText(
        chatId: String,
        text: String,
        replyTo: Message? = null,
    ) {
        val msg =
            Message(
                senderId = uid,
                senderName = profile.value.name,
                type = "text",
                text = text.trim(),
                replyToId = replyTo?.id.orEmpty(),
                replyPreview =
                    replyTo?.let {
                        when (it.type) {
                            "image" -> str(R.string.photo)
                            "video" -> "Video"
                            else -> it.text.take(80)
                        }
                    }.orEmpty(),
                readBy = listOf(uid),
                deliveredTo = listOf(uid),
            )
        val chatRef = db.collection("chats").document(chatId)
        chatRef.collection("messages").add(msg)
        chatRef.update(chatBump(chatId, text.trim().take(80)))
    }

    private suspend fun sendSystem(
        chatId: String,
        text: String,
    ) {
        val chatRef = db.collection("chats").document(chatId)
        chatRef.collection(
            "messages",
        ).add(Message(senderId = uid, senderName = "", type = "system", text = text, readBy = listOf(uid))).await()
        // lastSenderId is cleared so a system line (member added, expense split) never triggers a message alert.
        chatRef.update(mapOf("lastMessage" to text, "lastAt" to FieldValue.serverTimestamp(), "lastSenderId" to "")).await()
    }

    suspend fun react(
        chatId: String,
        msg: Message,
        key: String,
    ) {
        val ref = db.collection("chats").document(chatId).collection("messages").document(msg.id)
        if (msg.reactions[uid] == key) {
            ref.update("reactions.$uid", FieldValue.delete()).await()
        } else {
            ref.update("reactions.$uid", key).await()
        }
    }

    /** Opening a chat: mark messages as received and read, and zero this person's unread counter. */
    suspend fun markRead(
        chatId: String,
        msgs: List<Message>,
    ) {
        val unread = msgs.filter { it.senderId != uid && it.type != "system" && (uid !in it.readBy || uid !in it.deliveredTo) }
        val chatRef = db.collection("chats").document(chatId)
        if (unread.isNotEmpty()) {
            val batch = db.batch()
            unread.forEach {
                batch.update(
                    chatRef.collection("messages").document(it.id),
                    mapOf("readBy" to FieldValue.arrayUnion(uid), "deliveredTo" to FieldValue.arrayUnion(uid)),
                )
            }
            batch.commit().await()
        }
        if ((chats.value.firstOrNull { it.id == chatId }?.unread?.get(uid) ?: 0L) > 0L) chatRef.update("unread.$uid", 0L).await()
    }

    /** A message reached this phone (chat not open): lets the sender see the double grey tick. */
    suspend fun markDelivered(chatId: String) {
        val me = uid
        if (me.isBlank()) return
        val col = db.collection("chats").document(chatId).collection("messages")
        val recent = col.orderBy("createdAt", Query.Direction.DESCENDING).limit(30).get().await()
        val todo =
            recent.documents.filter { d ->
                d.getString("senderId") != me && d.getString("type") != "system" &&
                        (d.get("deliveredTo") as? List<*>)?.contains(me) != true
            }
        if (todo.isEmpty()) return
        val batch = db.batch()
        todo.forEach { batch.update(it.reference, "deliveredTo", FieldValue.arrayUnion(me)) }
        batch.commit().await()
    }

    fun setTyping(
        chatId: String,
        typing: Boolean,
    ) {
        db.collection("chats").document(chatId).update("typing.$uid", if (typing) System.currentTimeMillis() else 0L)
    }

    /** Only works within 5 minutes of sending; Firestore rules enforce the same limit. */
    suspend fun deleteForEveryone(
        chatId: String,
        msg: Message,
    ) {
        db.collection("chats").document(chatId).collection("messages").document(msg.id)
            .update(mapOf("deleted" to true, "text" to "", "imageUrl" to "")).await()
    }

    suspend fun deleteForMe(msg: Message) {
        put("hidden", msg.id, HiddenMsg())
    }

    // ---------- Splits ----------
    fun splits(chatId: String): Flow<List<SplitEntry>> =
        callbackFlow {
            val reg =
                db.collection("chats").document(chatId).collection("splits")
                    .orderBy("dateMillis", Query.Direction.DESCENDING)
                    .addSnapshotListener { s, _ -> if (s != null) trySend(s.toObjects(SplitEntry::class.java)) }
            awaitClose { reg.remove() }
        }

    suspend fun addSplit(
        chatId: String,
        e: SplitEntry,
    ) {
        val ref = db.collection("chats").document(chatId)
        ref.collection("splits").add(e.copy(createdBy = uid)).await()
        val text =
            if (e.kind == "settlement") {
                str(
                    R.string.settled,
                    formatPaise(e.totalPaise),
                )
            } else {
                "${e.description}: ${formatPaise(e.totalPaise)}"
            }
        sendSystem(chatId, text)
    }

    suspend fun deleteSplit(
        chatId: String,
        id: String,
    ) {
        db.collection("chats").document(chatId).collection("splits").document(id).delete().await()
    }
}

/** Marker document for "delete for me". Its id is the message id. */
data class HiddenMsg(
    @com.google.firebase.firestore.DocumentId val id: String = "",
    val at: Long = 0,
)