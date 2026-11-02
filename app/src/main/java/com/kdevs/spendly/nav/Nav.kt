package com.kdevs.spendly.nav

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kdevs.spendly.data.Inbox
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.ui.components.MessageBanner
import com.kdevs.spendly.ui.screens.auth.AuthScreen
import com.kdevs.spendly.ui.screens.auth.LegalScreen
import com.kdevs.spendly.ui.screens.auth.WelcomeScreen
import com.kdevs.spendly.ui.screens.chat.AddFriendScreen
import com.kdevs.spendly.ui.screens.chat.AddMembersScreen
import com.kdevs.spendly.ui.screens.chat.BlockedScreen
import com.kdevs.spendly.ui.screens.chat.ChatInfoScreen
import com.kdevs.spendly.ui.screens.chat.ChatListScreen
import com.kdevs.spendly.ui.screens.chat.ConversationScreen
import com.kdevs.spendly.ui.screens.chat.CreateGroupScreen
import com.kdevs.spendly.ui.screens.chat.ImageViewerScreen
import com.kdevs.spendly.ui.screens.chat.MessageActionsScreen
import com.kdevs.spendly.ui.screens.chat.NewChatScreen
import com.kdevs.spendly.ui.screens.chat.ReportUserScreen
import com.kdevs.spendly.ui.screens.chat.RequestsScreen
import com.kdevs.spendly.ui.screens.export.ExportScreen
import com.kdevs.spendly.ui.screens.home.AccountsScreen
import com.kdevs.spendly.ui.screens.home.ActivityScreen
import com.kdevs.spendly.ui.screens.home.HomeScreen
import com.kdevs.spendly.ui.screens.home.ReportCategoryScreen
import com.kdevs.spendly.ui.screens.home.ReportsScreen
import com.kdevs.spendly.ui.screens.money.AccountDetailScreen
import com.kdevs.spendly.ui.screens.money.AccountEditScreen
import com.kdevs.spendly.ui.screens.money.AccountTypeScreen
import com.kdevs.spendly.ui.screens.money.BudgetEditScreen
import com.kdevs.spendly.ui.screens.money.BudgetsScreen
import com.kdevs.spendly.ui.screens.money.GoalAddScreen
import com.kdevs.spendly.ui.screens.money.GoalEditScreen
import com.kdevs.spendly.ui.screens.money.GoalsScreen
import com.kdevs.spendly.ui.screens.money.RecurringEditScreen
import com.kdevs.spendly.ui.screens.money.RecurringScreen
import com.kdevs.spendly.ui.screens.money.TemplateEditScreen
import com.kdevs.spendly.ui.screens.money.TemplatesScreen
import com.kdevs.spendly.ui.screens.money.TransferScreen
import com.kdevs.spendly.ui.screens.money.TxnDetailScreen
import com.kdevs.spendly.ui.screens.money.TxnFormScreen
import com.kdevs.spendly.ui.screens.pickers.AccountPickerScreen
import com.kdevs.spendly.ui.screens.pickers.BulkScreen
import com.kdevs.spendly.ui.screens.pickers.CategoryEditScreen
import com.kdevs.spendly.ui.screens.pickers.CategoryPickerScreen
import com.kdevs.spendly.ui.screens.pickers.ColorPickerScreen
import com.kdevs.spendly.ui.screens.pickers.DatePickerScreen
import com.kdevs.spendly.ui.screens.pickers.FilterScreen
import com.kdevs.spendly.ui.screens.pickers.IconPickerScreen
import com.kdevs.spendly.ui.screens.pickers.ReportFilterScreen
import com.kdevs.spendly.ui.screens.pickers.SelectScreen
import com.kdevs.spendly.ui.screens.pickers.SubcategoryEditScreen
import com.kdevs.spendly.ui.screens.pickers.TimePickerScreen
import com.kdevs.spendly.ui.screens.settings.DeleteAccountScreen
import com.kdevs.spendly.ui.screens.settings.EditNameScreen
import com.kdevs.spendly.ui.screens.settings.SettingsScreen
import com.kdevs.spendly.ui.screens.splits.AddSplitScreen
import com.kdevs.spendly.ui.screens.splits.SettleUpScreen
import com.kdevs.spendly.ui.screens.splits.SplitEntryScreen
import com.kdevs.spendly.ui.screens.splits.SplitsScreen
import kotlinx.coroutines.launch

/** Every screen in the app. Empty ids are sent as "-" because a path segment cannot be blank. */
object Routes {
    const val WELCOME = "welcome"
    const val AUTH = "auth"
    const val PRIVACY = "privacy"
    const val TERMS = "terms"

    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val REPORTS = "reports"
    const val ACCOUNTS = "accounts"
    const val CHATS = "chats"
    const val SETTINGS = "settings"

    fun txnAdd(kind: String = "expense") = "txn/add/$kind"

    fun txnEdit(id: String) = "txn/edit/$id"

    fun txnDetail(id: String) = "txn/$id"

    const val TRANSFER = "transfer"

    const val ACCOUNT_TYPE = "account/type"

    fun accountEdit(
        id: String,
        type: String = "-",
    ) = "account/edit/${id.ifBlank { "-" }}/$type"

    fun accountDetail(id: String) = "account/$id"

    const val FILTER = "filter"
    const val SELECT = "select"
    const val BULK = "bulk"
    const val REPORT_FILTER = "reportFilter"

    fun reportCategory(id: String) = "report/category/$id"

    fun categoryPicker(
        kind: String,
        slot: String = "category",
    ) = "pick/category/$kind/$slot"

    fun categoryEdit(
        id: String,
        kind: String = "expense",
    ) = "category/edit/${id.ifBlank { "-" }}/$kind"

    fun subEdit(
        id: String,
        parentId: String,
    ) = "category/sub/${id.ifBlank { "-" }}/$parentId"

    fun colorPicker(
        current: String,
        locked: String = "",
    ) = "pick/color/${current.ifBlank { "-" }}/${locked.ifBlank { "-" }}"

    fun iconPicker(
        group: String,
        current: String,
    ) = "pick/icon/$group/${current.ifBlank { "-" }}"

    fun accountPicker(
        slot: String,
        exclude: String = "",
    ) = "pick/account/$slot/${exclude.ifBlank { "-" }}"

    fun datePicker(millis: Long) = "pick/date/$millis"

    fun timePicker(minutes: Int) = "pick/time/$minutes"

    const val BUDGETS = "budgets"

    fun budgetEdit(id: String) = "budget/${id.ifBlank { "-" }}"

    const val GOALS = "goals"

    fun goalEdit(id: String) = "goal/${id.ifBlank { "-" }}"

    fun goalAdd(id: String) = "goal/add/$id"

    const val TEMPLATES = "templates"

    fun templateEdit(id: String) = "template/${id.ifBlank { "-" }}"

    const val RECURRING = "recurring"

    fun recurringEdit(id: String) = "recurring/${id.ifBlank { "-" }}"

    const val NEW_CHAT = "chat/new"
    const val ADD_FRIEND = "chat/addFriend"
    const val REQUESTS = "chat/requests"
    const val BLOCKED = "chat/blocked"
    const val CREATE_GROUP = "chat/createGroup"

    fun chat(id: String) = "chat/$id"

    fun chatInfo(id: String) = "chat/info/$id"

    fun messageActions(
        chatId: String,
        msgId: String,
    ) = "chat/actions/$chatId/$msgId"

    fun image(
        chatId: String,
        msgId: String,
    ) = "chat/image/$chatId/$msgId"

    fun splits(chatId: String) = "chat/splits/$chatId"

    fun addSplit(chatId: String) = "chat/addSplit/$chatId"

    fun settle(chatId: String) = "chat/settle/$chatId"

    fun splitEntry(
        chatId: String,
        entryId: String,
    ) = "chat/splitEntry/$chatId/$entryId"

    fun addMembers(chatId: String) = "chat/addMembers/$chatId"

    fun reportUser(
        chatId: String,
        uid: String,
    ) = "chat/report/$chatId/$uid"

    const val EXPORT = "export"
    const val EDIT_NAME = "settings/name"
    const val DELETE_ACCOUNT = "settings/delete"
}

fun String.arg(): String = if (this == "-") "" else this

/** Reads a value a picker screen left for us (see NavController.setPicked) and hands it over once. */
@Composable
fun <T : Any> NavBackStackEntry.Picked(
    key: String,
    onPicked: (T) -> Unit,
) {
    val handle = savedStateHandle
    val flow = remember(handle, key) { handle.getStateFlow<T?>(key, null) }
    val v by flow.collectAsState()
    LaunchedEffect(v) {
        v?.let {
            onPicked(it)
            handle.remove<T>(key)
        }
    }
}

private const val MS = 280
private val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(MS)) + fadeIn(tween(MS)) }
private val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(MS), targetOffset = { it / 4 }) + fadeOut(tween(MS)) }
private val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(MS), initialOffset = { it / 4 }) + fadeIn(tween(MS)) }
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(MS)) + fadeOut(tween(MS)) }
private val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { fadeIn(tween(200)) }
private val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { fadeOut(tween(200)) }

private fun androidx.navigation.NavGraphBuilder.tab(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit,
) = composable(route, enterTransition = tabEnter, exitTransition = tabExit, popEnterTransition = tabEnter, popExitTransition = tabExit) {
    content(it)
}

private fun androidx.navigation.NavGraphBuilder.screen(
    route: String,
    args: List<String> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) = composable(
    route,
    arguments =
        args.map {
            navArgument(it) {
                type = NavType.StringType
                defaultValue = "-"
            }
        },
    enterTransition = enter,
    exitTransition = exit,
    popEnterTransition = popEnter,
    popExitTransition = popExit,
) { content(it) }

private fun NavBackStackEntry.s(name: String) = (arguments?.getString(name) ?: "-").arg()

@Composable
fun SpendlyNav() {
    val nav = rememberNavController()
    val uid by Repo.uidFlow.collectAsState()
    val start = if (uid == null) Routes.WELCOME else Routes.HOME

    // Move between the signed-out and signed-in halves of the app when the login changes.
    var seen by remember { mutableStateOf(uid) }
    LaunchedEffect(uid) {
        if (uid != seen) {
            seen = uid
            val target = if (uid == null) Routes.WELCOME else Routes.HOME
            nav.navigate(target) { popUpTo(0) { inclusive = true } }
        }
    }
    LaunchedEffect(uid) {
        if (uid != null) {
            runCatching { Repo.ensureProfile() }
            runCatching { Repo.runRecurring() }
            runCatching { Repo.refreshToken() } // so pushes reach this phone even after a reinstall or sign in
        }
    }
    val resumeScope = rememberCoroutineScope()
    // Coming back to the app after the due time also adds anything that is due (the launch call above only runs once).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (uid != null) {
            resumeScope.launch { runCatching { Repo.runRecurring() } }
        }
    }
    // A tapped notification asks for a chat to open.
    val openChat by Inbox.openRequest.collectAsState()
    LaunchedEffect(openChat, uid) {
        val target = openChat
        if (target != null && uid != null) {
            Inbox.openRequest.value = null
            nav.navigate(Routes.chat(target)) { launchSingleTop = true }
        }
    }
    // When a friend accepts, our pending request disappears from this list. That is the moment to add them.
    val outgoing by Repo.outgoingRequests.collectAsState()
    LaunchedEffect(uid, outgoing.size) {
        if (uid != null) runCatching { Repo.syncAccepted() }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = start) {
            // ----- Signed out -----
            screen(Routes.WELCOME) { WelcomeScreen(nav) }
            screen(Routes.AUTH) { AuthScreen(nav) }
            screen(Routes.PRIVACY) { LegalScreen(nav, privacy = true) }
            screen(Routes.TERMS) { LegalScreen(nav, privacy = false) }

            // ----- Main tabs -----
            tab(Routes.HOME) { HomeScreen(nav) }
            tab(Routes.ACTIVITY) { ActivityScreen(nav) }
            tab(Routes.REPORTS) { ReportsScreen(nav) }
            tab(Routes.ACCOUNTS) { AccountsScreen(nav) }
            tab(Routes.CHATS) { ChatListScreen(nav) }
            screen(Routes.SETTINGS) { SettingsScreen(nav) }
            screen(Routes.EDIT_NAME) { EditNameScreen(nav) }
            screen(Routes.DELETE_ACCOUNT) { DeleteAccountScreen(nav) }

            // ----- Transactions -----
            screen("txn/add/{kind}", listOf("kind")) { e -> TxnFormScreen(nav, e, id = "", kind = e.s("kind").ifBlank { "expense" }) }
            screen("txn/edit/{id}", listOf("id")) { e -> TxnFormScreen(nav, e, id = e.s("id"), kind = "") }
            screen("txn/{id}", listOf("id")) { e -> TxnDetailScreen(nav, e.s("id")) }
            screen(Routes.TRANSFER) { TransferScreen(nav, it) }

            // ----- Accounts -----
            screen(Routes.ACCOUNT_TYPE) { AccountTypeScreen(nav) }
            screen("account/edit/{id}/{type}", listOf("id", "type")) { e -> AccountEditScreen(nav, e, e.s("id"), e.s("type")) }
            screen("account/{id}", listOf("id")) { e -> AccountDetailScreen(nav, e.s("id")) }

            // ----- Filter, select, bulk, reports -----
            screen(Routes.FILTER) { FilterScreen(nav, it) }
            screen(Routes.SELECT) { SelectScreen(nav) }
            screen(Routes.BULK) { BulkScreen(nav, it) }
            screen(Routes.REPORT_FILTER) { ReportFilterScreen(nav, it) }
            screen("report/category/{id}", listOf("id")) { e -> ReportCategoryScreen(nav, e.s("id")) }

            // ----- Pickers -----
            screen("pick/category/{kind}/{slot}", listOf("kind", "slot")) { e -> CategoryPickerScreen(nav, e.s("kind"), e.s("slot")) }
            screen("category/edit/{id}/{kind}", listOf("id", "kind")) { e -> CategoryEditScreen(nav, e, e.s("id"), e.s("kind")) }
            screen("category/sub/{id}/{parent}", listOf("id", "parent")) { e -> SubcategoryEditScreen(nav, e, e.s("id"), e.s("parent")) }
            screen(
                "pick/color/{current}/{locked}",
                listOf("current", "locked"),
            ) { e -> ColorPickerScreen(nav, e.s("current"), e.s("locked")) }
            screen("pick/icon/{group}/{current}", listOf("group", "current")) { e -> IconPickerScreen(nav, e.s("group"), e.s("current")) }
            screen(
                "pick/account/{slot}/{exclude}",
                listOf("slot", "exclude"),
            ) { e -> AccountPickerScreen(nav, e.s("slot"), e.s("exclude")) }
            screen("pick/date/{millis}", listOf("millis")) { e -> DatePickerScreen(nav, e.s("millis").toLongOrNull() ?: 0L) }
            screen("pick/time/{minutes}", listOf("minutes")) { e -> TimePickerScreen(nav, e.s("minutes").toIntOrNull() ?: 0) }

            // ----- Budgets, goals, templates, recurring -----
            screen(Routes.BUDGETS) { BudgetsScreen(nav) }
            screen("budget/{id}", listOf("id")) { e -> BudgetEditScreen(nav, e, e.s("id")) }
            screen(Routes.GOALS) { GoalsScreen(nav) }
            screen("goal/add/{id}", listOf("id")) { e -> GoalAddScreen(nav, e.s("id")) }
            screen("goal/{id}", listOf("id")) { e -> GoalEditScreen(nav, e, e.s("id")) }
            screen(Routes.TEMPLATES) { TemplatesScreen(nav) }
            screen("template/{id}", listOf("id")) { e -> TemplateEditScreen(nav, e, e.s("id")) }
            screen(Routes.RECURRING) { RecurringScreen(nav) }
            screen("recurring/{id}", listOf("id")) { e -> RecurringEditScreen(nav, e, e.s("id")) }

            // ----- Chat -----
            screen(Routes.NEW_CHAT) { NewChatScreen(nav) }
            screen(Routes.ADD_FRIEND) { AddFriendScreen(nav) }
            screen(Routes.REQUESTS) { RequestsScreen(nav) }
            screen(Routes.BLOCKED) { BlockedScreen(nav) }
            screen(Routes.CREATE_GROUP) { CreateGroupScreen(nav) }
            screen("chat/info/{id}", listOf("id")) { e -> ChatInfoScreen(nav, e.s("id")) }
            screen("chat/actions/{chat}/{msg}", listOf("chat", "msg")) { e -> MessageActionsScreen(nav, e.s("chat"), e.s("msg")) }
            screen("chat/image/{chat}/{msg}", listOf("chat", "msg")) { e -> ImageViewerScreen(nav, e.s("chat"), e.s("msg")) }
            screen("chat/splits/{id}", listOf("id")) { e -> SplitsScreen(nav, e.s("id")) }
            screen("chat/addSplit/{id}", listOf("id")) { e -> AddSplitScreen(nav, e, e.s("id")) }
            screen("chat/settle/{id}", listOf("id")) { e -> SettleUpScreen(nav, e, e.s("id")) }
            screen("chat/report/{chat}/{uid}", listOf("chat", "uid")) { e -> ReportUserScreen(nav, e.s("chat"), e.s("uid")) }
            screen("chat/addMembers/{id}", listOf("id")) { e -> AddMembersScreen(nav, e.s("id")) }
            screen("chat/{id}", listOf("id")) { e -> ConversationScreen(nav, e, e.s("id")) }
            screen("chat/splitEntry/{chat}/{entry}", listOf("chat", "entry")) { e -> SplitEntryScreen(nav, e.s("chat"), e.s("entry")) }

            // ----- Export -----
            screen(Routes.EXPORT) { ExportScreen(nav, it) }
        }
        if (uid != null) MessageBanner(nav)
    }
}
