package com.kdevs.spendly.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Biometric
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.findFragmentActivity
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.pressable
import com.kdevs.spendly.ui.theme.Brand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The "S" from the launcher icon, in the same 108 x 108 space.
private const val S_PATH =
    "M66,43 C66,37 60.5,33.5 54,33.5 C46.5,33.5 42,37.5 42,43 C42,49 47,51 54,54 " +
        "C61,57 66,59 66,65 C66,70.5 61.5,74.5 54,74.5 C47,74.5 42,71 42,65"
private val CoinGold get() = clr(R.color.coin_gold)

/**
 * Animated splash. It is also the lock screen: when the app is locked, the biometric prompt opens right on top of it
 * and the splash stays until the person unlocks (there is no separate lock screen).
 *
 * A soft halo fades in, the "S" draws itself in, the gold coin pops in, the wordmark and tagline rise in. Then:
 *  - not locked: the overlay fades into the app;
 *  - locked: the system fingerprint / face / screen-lock prompt opens over the splash. If it is cancelled or fails,
 *    an Unlock button appears at the bottom to try again. After success the overlay fades away.
 *
 * [quick] skips the intro (used when the app locks again after being away), so the prompt opens at once.
 * Background is the same solid brand colour as Theme.Spendly.Starting so the handoff is seamless.
 */
@Composable
fun SplashOverlay(
    locked: Boolean = false,
    quick: Boolean = false,
    onUnlocked: () -> Unit = {},
    onFinished: () -> Unit,
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val halo = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    val coin = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    val sPath = remember { PathParser().parsePathString(S_PATH).toPath() }
    val sLength = remember { PathMeasure().also { it.setPath(sPath, false) }.length }
    val measure = remember { PathMeasure() }

    val lockedNow by rememberUpdatedState(locked)
    val unlockedCb by rememberUpdatedState(onUnlocked)
    val finishedCb by rememberUpdatedState(onFinished)
    var phase by remember { mutableIntStateOf(0) } // 0 intro, 1 waiting for unlock, 2 fading out
    var waiting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun ask() {
        val activity = ctx.findFragmentActivity()
        // The phone lost its fingerprint / screen lock: do not lock the person out of their own data.
        if (activity == null || !Biometric.available(ctx)) {
            Prefs.updateBiometric(false)
            unlockedCb()
            phase = 2
            return
        }
        if (waiting) return
        waiting = true
        failed = false
        Biometric.prompt(activity, str(R.string.unlock_spendly), str(R.string.confirm_it_is_you)) { ok ->
            waiting = false
            if (ok) {
                unlockedCb()
                phase = 2
            } else {
                failed = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (quick) {
            halo.snapTo(1f)
            draw.snapTo(1f)
            coin.snapTo(1f)
            word.snapTo(1f)
            tag.snapTo(1f)
        } else {
            halo.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            draw.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
            coin.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
            coroutineScope {
                launch { word.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
                launch {
                    delay(250)
                    tag.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
                }
            }
        }
        if (lockedNow) {
            phase = 1
            delay(200)
            ask()
        } else {
            if (!quick) delay(450)
            phase = 2
        }
    }

    LaunchedEffect(phase) {
        if (phase == 2) {
            alpha.animateTo(0f, tween(400))
            finishedCb()
        }
    }

    // The background is always the brand colour, so the status bar icons must be light whatever the theme is.
    DisposableEffect(Unit) {
        val window = ctx.findFragmentActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (before != null) controller.isAppearanceLightStatusBars = before }
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(Brand)
            // Swallow touches so nothing underneath can be tapped while the splash (or lock) is showing.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(132.dp)) {
                val s = size.width / 108f
                withTransform({ scale(s, s, Offset.Zero) }) {
                    drawCircle(
                        Color.White.copy(alpha = 0.14f * halo.value),
                        radius = 44f * (0.8f + 0.2f * halo.value),
                        center = Offset(54f, 54f),
                    )
                    val seg = Path()
                    measure.setPath(sPath, false)
                    measure.getSegment(0f, sLength * draw.value, seg, true)
                    drawPath(seg, Color.White, style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    if (coin.value > 0f) drawCircle(CoinGold, radius = 4.6f * coin.value, center = Offset(74.5f, 31.5f))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                str(R.string.spendly),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                modifier =
                    Modifier.graphicsLayer {
                        this.alpha = word.value
                        translationY = (1f - word.value) * 24f
                    },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                str(R.string.know_where_it_goes),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier.graphicsLayer {
                        this.alpha = tag.value
                        translationY = (1f - tag.value) * 16f
                    },
            )
        }

        // Shown only while locked and the system prompt is not on screen (cancelled or failed): a way to try again.
        if (phase == 1 && !waiting) {
            val buttonShape = RoundedCornerShape(18.dp)
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (failed) {
                    Text(
                        str(R.string.that_did_not_work_tap_unlock),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pressable(buttonShape, onClick = { ask() })
                        .clip(buttonShape)
                        .background(Color.White),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Fingerprint, null, tint = Brand, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(str(R.string.unlock), color = Brand, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}
