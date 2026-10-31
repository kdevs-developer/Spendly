package com.kdevs.spendly.ui.screens.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import com.kdevs.spendly.ui.theme.HeroBrush
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Covers the whole app until the person proves it is them. [active] turns true once the splash
 * is gone, and the system prompt then opens by itself. Tap the fingerprint or Unlock to ask again.
 *
 * Look: brand gradient, soft circles in the background, a big fingerprint button with a slow
 * pulse, a shake when it fails, and one white Unlock button at the bottom.
 */
@Composable
fun LockScreen(
    active: Boolean,
    onUnlocked: () -> Unit,
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    var failed by remember { mutableStateOf(false) }
    var waiting by remember { mutableStateOf(false) }
    var shakeTick by remember { mutableIntStateOf(0) }

    fun ask() {
        val activity = ctx.findFragmentActivity()
        // The phone lost its fingerprint / screen lock: do not lock the person out of their own data.
        if (activity == null || !Biometric.available(ctx)) {
            Prefs.updateBiometric(false)
            onUnlocked()
            return
        }
        if (waiting) return
        waiting = true
        failed = false
        Biometric.prompt(activity, str(R.string.unlock_spendly), str(R.string.confirm_it_is_you)) { ok ->
            waiting = false
            if (ok) {
                onUnlocked()
            } else {
                failed = true
                shakeTick++
            }
        }
    }

    LaunchedEffect(active) {
        if (active) {
            delay(200)
            ask()
        }
    }

    // The background is always dark blue, so the status bar icons must be light whatever the theme is.
    DisposableEffect(Unit) {
        val window = ctx.findFragmentActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (before != null) controller.isAppearanceLightStatusBars = before }
    }

    val onBrand = clr(R.color.on_brand)

    // One slow ring that grows and fades, over and over.
    val pulse = rememberInfiniteTransition(label = "lockPulse")
    val ring by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "lockRing",
    )

    // A short left-right shake on the fingerprint when unlocking fails.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeTick) {
        if (shakeTick > 0) {
            repeat(3) {
                shake.animateTo(14f, tween(50))
                shake.animateTo(-14f, tween(50))
            }
            shake.animateTo(0f, tween(50))
        }
    }

    Box(
        Modifier.fillMaxSize().background(HeroBrush)
            // Swallow touches so nothing underneath can be tapped while locked.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // Soft decoration: two large faint circles in the corners.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(onBrand.copy(alpha = 0.07f), radius = size.width * 0.65f, center = Offset(size.width * 0.95f, size.height * 0.02f))
            drawCircle(onBrand.copy(alpha = 0.05f), radius = size.width * 0.55f, center = Offset(size.width * 0.02f, size.height * 0.98f))
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(Modifier.size(200.dp).offset { IntOffset(shake.value.roundToInt(), 0) }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(clr(R.color.lock_ring).copy(alpha = (1f - ring) * 0.30f), radius = r * (0.55f + 0.45f * ring))
                }
                Box(
                    Modifier.size(112.dp)
                        .pressable(CircleShape, scaleTo = 0.94f, veil = false, onClick = { ask() })
                        .clip(CircleShape)
                        .background(onBrand.copy(alpha = 0.16f))
                        .border(1.5.dp, onBrand.copy(alpha = 0.40f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Fingerprint, str(R.string.unlock), tint = onBrand, modifier = Modifier.size(60.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                str(R.string.spendly_is_locked),
                color = onBrand,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                str(R.string.use_your_fingerprint_face_or_screen),
                color = onBrand.copy(alpha = 0.80f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            if (failed) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(onBrand.copy(alpha = 0.14f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = onBrand, modifier = Modifier.size(20.dp))
                    Text(str(R.string.that_did_not_work_tap_unlock), color = onBrand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(14.dp))
            }

            val buttonShape = RoundedCornerShape(18.dp)
            Row(
                Modifier.fillMaxWidth().height(56.dp)
                    .pressable(buttonShape, enabled = !waiting, onClick = { ask() })
                    .clip(buttonShape).background(onBrand),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (waiting) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Brand, strokeWidth = 2.5.dp)
                } else {
                    Icon(Icons.Filled.LockOpen, null, tint = Brand, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(str(R.string.unlock), color = Brand, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                str(R.string.lock_footer),
                color = onBrand.copy(alpha = 0.65f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
