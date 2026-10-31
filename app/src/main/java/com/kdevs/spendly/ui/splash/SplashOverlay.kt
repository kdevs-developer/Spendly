package com.kdevs.spendly.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.str
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
 * Animated splash: a soft halo fades in, the "S" draws itself in, the gold coin pops in,
 * the wordmark and tagline rise in, then the whole overlay fades into the app.
 * Background is the same solid brand blue as Theme.Spendly.Starting so the handoff is seamless.
 */
@Composable
fun SplashOverlay(onFinished: () -> Unit) {
    val halo = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    val coin = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    val sPath = remember { PathParser().parsePathString(S_PATH).toPath() }
    val sLength = remember { PathMeasure().also { it.setPath(sPath, false) }.length }
    val measure = remember { PathMeasure() }

    LaunchedEffect(Unit) {
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
        delay(450)
        alpha.animateTo(0f, tween(400))
        onFinished()
    }

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(Brand),
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
}
