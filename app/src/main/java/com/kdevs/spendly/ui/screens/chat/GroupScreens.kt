package com.kdevs.spendly.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.common.TickBox
import com.kdevs.spendly.ui.screens.money.SavedBar
import com.kdevs.spendly.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(nav: NavController) {
    val friends by Repo.friends.collectAsState()
    var name by remember { mutableStateOf("") }
    var split by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    ScreenScaffold(str(R.string.new_group), { nav.popBackStack() }, bottomBar = {
        SavedBar(str(R.string.create_group), saver, name.isNotBlank() && picked.isNotEmpty(), onSave = {
            saver.run(scope, onDone = {}) {
                val id = Repo.createGroup(name.trim(), if (split == 1) "split" else "group", friends.filter { it.id in picked })
                nav.navigate(Routes.chat(id)) { popUpTo(Routes.CHATS) }
            }
        })
    }) {
        SegmentedControl(listOf(str(R.string.chat_group), str(R.string.expense_split)), split, { split = it })
        Hint(if (split == 1) str(R.string.chat_plus_a_shared_ledger_add) else str(R.string.group_chat_hint))
        AppTextField(name, { name = it }, str(R.string.group_name))
        SectionTitle(str(R.string.add_members, picked.size))
        if (friends.isEmpty()) Hint(str(R.string.add_friends_first_from_the_chat))
        Card(padding = 12.dp) {
            if (friends.isEmpty()) {
                EmptyState(Icons.Filled.People, str(R.string.no_friends_yet), str(R.string.add_friends_first_from_the_chat))
                PrimaryButton(str(R.string.add_a_friend), { nav.navigate(Routes.ADD_FRIEND) })
            } else {
                friends.forEachIndexed { i, f ->
                    SettingRow(
                        f.name,
                        leading = { Avatar(f.name, colorKeyFor(f.id)) },
                        onClick = { picked = if (f.id in picked) picked - f.id else picked + f.id },
                        trailing = { TickBox(f.id in picked) },
                        divider = i < friends.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
fun AddMembersScreen(
    nav: NavController,
    id: String,
) {
    val friends by Repo.friends.collectAsState()
    var chat by remember { mutableStateOf<Chat?>(null) }
    LaunchedEffect(id) { Repo.chat(id).collect { chat = it } }
    var picked by remember { mutableStateOf(setOf<String>()) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val candidates = friends.filter { f -> chat?.members?.contains(f.id) != true }
    ScreenScaffold(str(R.string.add_members_2), { nav.popBackStack() }, bottomBar = {
        SavedBar(str(R.string.add_people, picked.size), saver, picked.isNotEmpty(), onSave = {
            saver.run(scope, onDone = { nav.popBackStack() }) { Repo.addMembers(id, candidates.filter { it.id in picked }) }
        })
    }) {
        if (candidates.isEmpty()) {
            EmptyState(Icons.Filled.People, str(R.string.no_one_to_add), str(R.string.all_your_friends_are_already_here))
        } else {
            Card(padding = 12.dp) {
                candidates.forEachIndexed { i, f ->
                    SettingRow(
                        f.name,
                        leading = { Avatar(f.name, colorKeyFor(f.id)) },
                        onClick = { picked = if (f.id in picked) picked - f.id else picked + f.id },
                        trailing = { TickBox(f.id in picked) },
                        divider = i < candidates.lastIndex,
                    )
                }
            }
        }
    }
}
