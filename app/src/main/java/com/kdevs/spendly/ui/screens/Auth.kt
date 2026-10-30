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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Repo
import com.kdevs.spendly.data.Res
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Slide(val icon: ImageVector, val title: String, val body: String, val tint: Color)

private val slides =
    listOf(
        Slide(
            Icons.Filled.AccountBalanceWallet,
            str(R.string.every_rupee_in_one_place),
            str(R.string.track_banks_wallets_cash_and_cards),
            clr(R.color.swatch_blue),
        ),
        Slide(
            Icons.Filled.Receipt,
            str(R.string.add_a_spend_in_seconds),
            str(R.string.big_amount_quick_category_done_templat),
            clr(R.color.swatch_purple),
        ),
        Slide(
            Icons.Filled.PieChart,
            str(R.string.see_where_it_goes),
            str(R.string.reports_by_category_and_trend_show),
            clr(R.color.swatch_teal),
        ),
        Slide(
            Icons.AutoMirrored.Filled.Chat,
            str(R.string.split_with_friends),
            str(R.string.chat_one_to_one_or_in),
            clr(R.color.swatch_orange),
        ),
        Slide(
            Icons.Filled.CloudOff,
            str(R.string.works_without_internet),
            str(R.string.add_things_anywhere_spendly_syncs_when),
            clr(R.color.swatch_green),
        ),
    )

/** Five slides that move on their own every four seconds. Touching Skip, Next or a dot restarts the timer. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(nav: NavController) {
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) } // bumped on any manual action to restart the timer
    val last = slides.lastIndex

    LaunchedEffect(tick) {
        while (pager.currentPage < last) {
            delay(Res.integer(R.integer.welcome_slide_seconds) * 1000L)
            if (pager.currentPage < last) pager.animateScrollToPage(pager.currentPage + 1)
        }
    }

    fun goTo(i: Int) {
        tick++
        scope.launch { pager.animateScrollToPage(i) }
    }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < last) {
                Text(
                    str(R.string.skip),
                    color = Muted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).clickable { goTo(last) }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            } else {
                Spacer(Modifier.height(36.dp))
            }
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
                Text(
                    s.body,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            slides.indices.forEach { i ->
                val on = i == pager.currentPage
                val w by androidx.compose.animation.core.animateDpAsState(if (on) 24.dp else 8.dp, label = "dot")
                Box(
                    Modifier.padding(horizontal = 3.dp).height(8.dp).width(w).clip(CircleShape)
                        .background(if (on) Primary else Faint.copy(alpha = 0.45f)).clickable { goTo(i) },
                )
            }
        }
        if (pager.currentPage < last) {
            PrimaryButton(str(R.string.next), { goTo(pager.currentPage + 1) })
        } else {
            PrimaryButton(str(R.string.get_started), { nav.navigate(Routes.AUTH) })
        }
        Spacer(Modifier.height(14.dp))
        LegalLine(nav)
    }
}

@Composable
private fun LegalLine(nav: NavController) {
    val text =
        buildAnnotatedString {
            append(str(R.string.by_continuing_you_agree_to_the))
            pushStringAnnotation("go", "terms")
            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append(str(R.string.terms)) }
            pop()
            append(str(R.string.and))
            pushStringAnnotation("go", "privacy")
            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append(str(R.string.privacy_policy)) }
            pop()
        }
    ClickableText(
        text,
        modifier = Modifier.fillMaxWidth(),
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
    var mode by remember { mutableIntStateOf(0) } // 0 sign in, 1 create account
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    val saver = rememberSaver()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    fun validate(): String? =
        when {
            mode == 1 && name.isBlank() -> str(R.string.enter_your_name)
            !email.contains("@") || !email.contains(".") -> str(R.string.enter_a_valid_email)
            pass.length < 6 -> str(R.string.password_needs_at_least_6_characters)
            else -> null
        }

    ScreenScaffold(if (mode == 0) str(R.string.welcome_back) else str(R.string.create_account), onBack = { nav.popBackStack() }, busy = saver.busy, busyText = str(R.string.signing_you_in)) {
        Spacer(Modifier.height(4.dp))
        SegmentedControl(listOf(str(R.string.sign_in), str(R.string.create_account)), mode, {
            mode = it
            note = null
            saver.error = null
        })
        if (mode == 1) AppTextField(name, { name = it }, str(R.string.your_name))
        AppTextField(email, { email = it }, str(R.string.email), keyboard = KeyboardType.Email)
        var show by remember { mutableStateOf(false) }
        androidx.compose.material3.OutlinedTextField(
            value = pass, onValueChange = { pass = it }, label = { Text(str(R.string.password)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    str(
                        R.string.show_password,
                    ),
                    Modifier.clip(CircleShape).clickable {
                        show = !show
                    }.padding(6.dp),
                )
            },
            colors =
                androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    unfocusedBorderColor = FieldBorder,
                ),
        )
        (note ?: saver.error)?.let { Text(it, color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        PrimaryButton(if (mode == 0) str(R.string.sign_in) else str(R.string.create_account), {
            val e = validate()
            if (e != null) {
                note = e
                return@PrimaryButton
            }
            note = null
            saver.run(scope, onDone = {}) { if (mode == 0) Repo.signIn(email, pass) else Repo.signUp(name, email, pass) }
        }, busy = saver.busy)
        if (mode == 0) {
            Text(
                str(R.string.forgot_password),
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier.align(Alignment.CenterHorizontally)
                        .clip(CircleShape).clickable {
                            // Clear the old message first, otherwise it would hide the real result of this try.
                            note = null
                            if (!email.contains("@") || !email.contains(".")) {
                                note = str(R.string.enter_your_email_above_first)
                            } else {
                                saver.run(scope, onDone = {
                                    note = str(R.string.if_has_an_account_a_reset, email.trim())
                                }) { Repo.resetPassword(email) }
                            }
                        }.padding(10.dp),
            )
        }
        OrDivider()
        GoogleButton(str(R.string.continue_with_google), {
            note = null
            saver.run(scope, onDone = {}) { Repo.signInWithGoogle(ctx) }
        }, enabled = !saver.busy)
//        SecondaryButton(str(R.string.continue_as_guest), { saver.run(scope, onDone = {}) { Repo.signInGuest() } })
//        Hint(str(R.string.guest_account_hint), center = true)
    }
}

private data class LegalSection(val head: String, val body: String)

private val privacy =
    listOf(
        LegalSection(str(R.string.what_we_collect), str(R.string.your_name_and_email_when_you)),
        LegalSection(str(R.string.where_it_is_stored), str(R.string.in_your_private_area_of_google)),
        LegalSection(str(R.string.chat), str(R.string.messages_and_photos_are_visible_only)),
        LegalSection(str(R.string.friend_code), str(R.string.your_friend_code_lets_people_send)),
        LegalSection(str(R.string.reports), str(R.string.if_you_report_a_person_we)),
        LegalSection(str(R.string.your_choices), str(R.string.you_can_change_your_name_sign)),
        LegalSection(str(R.string.contact), str(R.string.questions_about_privacy_can_be_sent)),
    )

private val terms =
    listOf(
        LegalSection(str(R.string.using_spendly), str(R.string.spendly_helps_you_record_and_understan)),
        LegalSection(str(R.string.your_numbers), str(R.string.balances_are_worked_out_from_what)),
        LegalSection(str(R.string.settling_up), str(R.string.recording_a_settlement_in_a_group)),
        LegalSection(str(R.string.be_kind_in_chat), str(R.string.no_harassment_spam_or_illegal_content)),
        LegalSection(str(R.string.your_account), str(R.string.keep_your_login_safe_you_can)),
        LegalSection(str(R.string.changes), str(R.string.we_may_update_the_app_and)),
    )

/** Privacy Policy and Terms open as normal in-app screens, not a web page. */
@Composable
fun LegalScreen(
    nav: NavController,
    privacy: Boolean,
) {
    val items = if (privacy) com.kdevs.spendly.ui.screens.privacy else terms
    ScreenScaffold(if (privacy) str(R.string.privacy_policy) else str(R.string.terms_of_service), onBack = { nav.popBackStack() }) {
        Hint(str(R.string.last_updated_26_october_2026))
        items.forEachIndexed { i, s ->
            Card {
                Text(str(R.string.text_, i + 1, s.head), style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(6.dp))
                Text(s.body, color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}
