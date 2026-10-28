package com.kdevs.spendly.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.ui.theme.Primary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RupeeParts = listOf(
    "M42,40H66",
    "M42,49H66",
    "M42,40H52a9,9 0 0 1 0,18H42L60,74",
)

/**
 * Animated splash: the coin ring draws in, the rupee mark draws in, the coin fills white,
 * the wordmark and tagline rise in, then the whole overlay fades into the app.
 * Background is the same solid brand blue as Theme.Spendly.Starting so the handoff is seamless.
 */
@Composable
fun SplashOverlay(onFinished: () -> Unit) {
    val ring = remember { Animatable(0f) }
    val rupee = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    val parts = remember { RupeeParts.map { PathParser().parsePathString(it).toPath() } }
    val lengths = remember { parts.map { p -> PathMeasure().also { it.setPath(p, false) }.length } }
    val measure = remember { PathMeasure() }

    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        rupee.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        fill.animateTo(1f, tween(350))
        coroutineScope {
            launch { word.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
            launch { delay(250); tag.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        }
        delay(450)
        alpha.animateTo(0f, tween(400))
        onFinished()
    }

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(Primary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(132.dp)) {
            val s = size.width / 108f
            val rupeeColor = lerp(Color.White, Primary, fill.value)
            withTransform({ scale(s, s, Offset.Zero) }) {
                drawCircle(Color.White.copy(alpha = fill.value), radius = 30f, center = Offset(54f, 54f))
                drawArc(
                    color = Color.White, startAngle = -90f, sweepAngle = 360f * ring.value, useCenter = false,
                    topLeft = Offset(24f, 24f), size = Size(60f, 60f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
                var remain = lengths.sum() * rupee.value
                parts.forEachIndexed { i, p ->
                    val d = remain.coerceIn(0f, lengths[i])
                    if (d > 0f) {
                        val seg = Path()
                        measure.setPath(p, false)
                        measure.getSegment(0f, d, seg, true)
                        drawPath(seg, rupeeColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    remain -= lengths[i]
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Spendly", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp,
            modifier = Modifier.graphicsLayer { this.alpha = word.value; translationY = (1f - word.value) * 24f },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Know where it goes", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.graphicsLayer { this.alpha = tag.value; translationY = (1f - tag.value) * 16f },
        )
    }
}
