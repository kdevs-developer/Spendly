package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Slide(val icon: ImageVector, val title: String, val body: String, val tint: Color)

private val slides = listOf(
    Slide(Icons.Filled.AccountBalanceWallet, "Every rupee, in one place", "Track banks, wallets, cash and cards together and see what you really have.", Color(0xFF1D4ED8)),
    Slide(Icons.Filled.Receipt, "Add a spend in seconds", "Big amount, quick category, done. Templates and repeats handle the usual ones.", Color(0xFF7E22CE)),
    Slide(Icons.Filled.PieChart, "See where it goes", "Reports by category and trend show what to cut and what to keep.", Color(0xFF0F766E)),
    Slide(Icons.AutoMirrored.Filled.Chat, "Split with friends", "Chat one to one or in groups and split bills without the awkward maths.", Color(0xFFC2410C)),
    Slide(Icons.Filled.CloudOff, "Works without internet", "Add things anywhere. Spendly syncs when you are back online.", Color(0xFF15803D)),
)

/** Five slides that move on their own every four seconds. Touching Skip, Next or a dot restarts the timer. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(nav: NavController) {
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }   // bumped on any manual action to restart the timer
    val last = slides.lastIndex

    LaunchedEffect(tick) {
        while (pager.currentPage < last) {
            delay(4000)
            if (pager.currentPage < last) pager.animateScrollToPage(pager.currentPage + 1)
        }
    }
    fun goTo(i: Int) { tick++; scope.launch { pager.animateScrollToPage(i) } }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < last) Text("Skip", color = Muted, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable { goTo(last) }.padding(horizontal = 14.dp, vertical = 8.dp))
            else Spacer(Modifier.height(36.dp))
        }
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val s = slides[i]
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(150.dp).clip(CircleShape).background(s.tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(104.dp).clip(CircleShape).background(s.tint), contentAlignment = Alignment.Center) {
                        Icon(s.icon, null, tint = Color.White, modifier = Modifier.size(52.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(s.title, style = MaterialTheme.typography.headlineLarge, color = Ink, textAlign = TextAlign.Center, lineHeight = 38.sp)
                Spacer(Modifier.height(12.dp))
                Text(s.body, color = Muted, textAlign = TextAlign.Center, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            slides.indices.forEach { i ->
                val on = i == pager.currentPage
                val w by androidx.compose.animation.core.animateDpAsState(if (on) 24.dp else 8.dp, label = "dot")
                Box(Modifier.padding(horizontal = 3.dp).height(8.dp).width(w).clip(CircleShape)
                    .background(if (on) Primary else Color(0xFFCBD5E1)).clickable { goTo(i) })
            }
        }
        if (pager.currentPage < last) PrimaryButton("Next", { goTo(pager.currentPage + 1) })
        else PrimaryButton("Get started", { nav.navigate(Routes.AUTH) })
        Spacer(Modifier.height(14.dp))
        LegalLine(nav)
    }
}

@Composable
private fun LegalLine(nav: NavController) {
    val text = buildAnnotatedString {
        append("By continuing you agree to the ")
        pushStringAnnotation("go", "terms"); withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append("Terms") }; pop()
        append(" and ")
        pushStringAnnotation("go", "privacy"); withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append("Privacy Policy") }; pop()
    }
    ClickableText(
        text, modifier = Modifier.fillMaxWidth(),
        style = androidx.compose.ui.text.TextStyle(color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp),
        onClick = { off ->
            text.getStringAnnotations("go", off, off).firstOrNull()?.let {
                nav.navigate(if (it.item == "terms") Routes.TERMS else Routes.PRIVACY)
            }
        },
    )
}

@Composable
fun AuthScreen(nav: NavController) {
    var mode by remember { mutableIntStateOf(0) }   // 0 sign in, 1 create account
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()

    fun validate(): String? = when {
        mode == 1 && name.isBlank() -> "Enter your name."
        !email.contains("@") || !email.contains(".") -> "Enter a valid email."
        pass.length < 6 -> "Password needs at least 6 characters."
        else -> null
    }

    ScreenScaffold(if (mode == 0) "Welcome back" else "Create account", onBack = { nav.popBackStack() }) {
        Spacer(Modifier.height(4.dp))
        SegmentedControl(listOf("Sign in", "Create account"), mode, { mode = it; note = null; saver.error = null })
        if (mode == 1) AppTextField(name, { name = it }, "Your name")
        AppTextField(email, { email = it }, "Email", keyboard = KeyboardType.Email)
        var show by remember { mutableStateOf(false) }
        androidx.compose.material3.OutlinedTextField(
            value = pass, onValueChange = { pass = it }, label = { Text("Password") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Show password", Modifier.clickable { show = !show }) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedContainerColor = Surface, unfocusedContainerColor = Surface, unfocusedBorderColor = Color(0xFFD8DFEC)),
        )
        (note ?: saver.error)?.let { Text(it, color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        PrimaryButton(if (mode == 0) "Sign in" else "Create account", {
            val e = validate()
            if (e != null) { note = e; return@PrimaryButton }
            note = null
            saver.run(scope, onDone = {}) { if (mode == 0) Repo.signIn(email, pass) else Repo.signUp(name, email, pass) }
        }, busy = saver.busy)
        if (mode == 0) {
            Text("Forgot password?", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally)
                .clip(CircleShape).clickable {
                    if (!email.contains("@")) note = "Enter your email above first."
                    else saver.run(scope, onDone = { note = "Reset link sent to $email" }) { Repo.resetPassword(email) }
                }.padding(10.dp))
        }
        HorizontalDivider(color = Line)
        SecondaryButton("Continue as guest", { saver.run(scope, onDone = {}) { Repo.signInGuest() } })
        Hint("A guest account stays on this phone's login only. Create an account later to keep your data safe.", center = true)
    }
}

private data class LegalSection(val head: String, val body: String)

private val privacy = listOf(
    LegalSection("What we collect", "Your name and email when you sign up, and the accounts, transactions, budgets, goals and chats you add. If you use a guest account, only what you add."),
    LegalSection("Where it is stored", "In your private area of Google Firebase (Firestore, Storage and Authentication). Only you can read your money data. Spendly keeps a copy on your phone so it works offline."),
    LegalSection("Chat", "Messages and photos are visible only to the people in that chat. Chat is protected by access rules on the server, not by end-to-end encryption, so do not share secrets such as card numbers or passwords."),
    LegalSection("Friend code", "Your friend code lets people send you a request. They see your display name, nothing else. You can block anyone from Chat settings."),
    LegalSection("Reports", "If you report a person, we keep your reason and the chat reference so the problem can be reviewed."),
    LegalSection("Your choices", "You can change your name, sign out, or delete everything from Settings. Deleting removes your data and your login for good."),
    LegalSection("Contact", "Questions about privacy can be sent to the developer, KDevs, through the Spendly page on the store."),
)

private val terms = listOf(
    LegalSection("Using Spendly", "Spendly helps you record and understand your spending. It is a personal tool, not a bank, and it never moves real money."),
    LegalSection("Your numbers", "Balances are worked out from what you enter. Check them against your bank. We are not responsible for decisions made from them."),
    LegalSection("Settling up", "Recording a settlement in a group only notes that a payment happened. Pay people yourself, outside the app."),
    LegalSection("Be kind in chat", "No harassment, spam, or illegal content. We may remove content or accounts that break these rules."),
    LegalSection("Your account", "Keep your login safe. You can delete your account at any time from Settings."),
    LegalSection("Changes", "We may update the app and these terms. Major changes will be shown in the app."),
)

/** Privacy Policy and Terms open as normal in-app screens, not a web page. */
@Composable
fun LegalScreen(nav: NavController, privacy: Boolean) {
    val items = if (privacy) com.kdevs.spendly.ui.screens.privacy else terms
    ScreenScaffold(if (privacy) "Privacy Policy" else "Terms of Service", onBack = { nav.popBackStack() }) {
        Hint("Last updated 26 October 2026")
        items.forEachIndexed { i, s ->
            Card {
                Text("${i + 1}. ${s.head}", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(s.body, color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}
