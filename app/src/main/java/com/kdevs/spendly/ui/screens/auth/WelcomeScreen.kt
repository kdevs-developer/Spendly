package com.kdevs.spendly.ui.screens.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Res
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class Slide(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val tint: Color,
)

private val slides =
    listOf(
        Slide(
            Icons.Filled.AccountBalanceWallet,
            str(R.string.slide_all_money_title),
            str(R.string.slide_all_money_body),
            clr(R.color.swatch_blue),
        ),
        Slide(Icons.Filled.Bolt, str(R.string.slide_quick_add_title), str(R.string.slide_quick_add_body), clr(R.color.swatch_purple)),
        Slide(Icons.Filled.PieChart, str(R.string.slide_reports_title), str(R.string.slide_reports_body), clr(R.color.swatch_teal)),
        Slide(Icons.Filled.Savings, str(R.string.slide_budgets_title), str(R.string.slide_budgets_body), clr(R.color.swatch_green)),
        Slide(Icons.AutoMirrored.Filled.Chat, str(R.string.slide_split_title), str(R.string.slide_split_body), clr(R.color.swatch_orange)),
    )

/**
 * Five slides that move on their own every four seconds. Touching Skip, Next or a dot restarts the timer.
 * Each slide fades and scales as it swipes, the icon drifts a little faster than the text (parallax),
 * and while a slide is on screen its icon floats and its halo pulses.
 */
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
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
            // 0 when this slide is centred, -1 / +1 when it is a full page away.
            val shift = (pager.currentPage - i) + pager.currentPageOffsetFraction
            val away = shift.absoluteValue.coerceIn(0f, 1f)
            val alive = rememberInfiniteTransition(label = "alive")
            val float by alive.animateFloat(
                -1f,
                1f,
                infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "float",
            )
            val pulse by alive.animateFloat(
                1f,
                1.14f,
                infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "pulse",
            )
            Column(
                Modifier.fillMaxSize().graphicsLayer {
                    alpha = 1f - away * 0.85f
                    val k = 1f - away * 0.12f
                    scaleX = k
                    scaleY = k
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                            translationX = shift * size.width * 0.5f // parallax: drifts against the swipe
                        }.clip(CircleShape)
                        .background(s.tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(104.dp)
                            .graphicsLayer {
                                translationY = float * 8.dp.toPx()
                                rotationZ = shift * -25f
                            }.clip(CircleShape)
                            .background(s.tint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(s.icon, null, tint = Color.White, modifier = Modifier.size(52.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    s.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp,
                    modifier = Modifier.graphicsLayer { translationX = shift * size.width * 0.25f },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    s.body,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp).graphicsLayer { translationX = shift * size.width * 0.12f },
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
                val w by androidx.compose.animation.core
                    .animateDpAsState(if (on) 24.dp else 8.dp, label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .width(w)
                        .clip(CircleShape)
                        .background(if (on) Primary else Faint.copy(alpha = 0.45f))
                        .clickable { goTo(i) },
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
        style =
            androidx.compose.ui.text
                .TextStyle(color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp),
        onClick = { off ->
            text.getStringAnnotations("go", off, off).firstOrNull()?.let {
                nav.navigate(if (it.item == "terms") Routes.TERMS else Routes.PRIVACY)
            }
        },
    )
}
