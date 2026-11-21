package com.kdevs.spendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.theme.Bg
import com.kdevs.spendly.ui.theme.GlassBar
import com.kdevs.spendly.ui.theme.GlassBorder
import com.kdevs.spendly.ui.theme.GlowA
import com.kdevs.spendly.ui.theme.GlowB
import com.kdevs.spendly.ui.theme.HeroShadow
import com.kdevs.spendly.ui.theme.Ink

/** Soft glass backdrop: the tinted page colour with two soft glows, so the translucent cards have something to sit over. */
fun Modifier.glassBackdrop(): Modifier =
    composed {
        val base = Bg
        val a = GlowA
        val b = GlowB
        this.background(base).drawWithCache {
            val r = size.width * 0.75f
            val brushA =
                Brush.radialGradient(
                    listOf(a, Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.1f),
                    radius = r,
                )
            val brushB =
                Brush.radialGradient(
                    listOf(b, Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.75f),
                    radius = r,
                )
            onDrawBehind {
                drawRect(brushA)
                drawRect(brushB)
            }
        }
    }

/** Top bar with a round back button, used by every full screen. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    scroll: Boolean = true,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    busy: Boolean = false,
    busyText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // One scroll state drives both the header and the fade below it. It is only ever read inside
    // draw / graphics-layer lambdas, so scrolling repaints a few layers and never recomposes.
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val collapsePx = with(density) { 72.dp.toPx() }
    val fadePx = with(density) { 22.dp.toPx() }
    Box(Modifier.fillMaxSize()) {
        Column(modifier.fillMaxSize().glassBackdrop().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, str(R.string.back), onBack)
                } else {
                    Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    modifier =
                        Modifier.weight(1f).graphicsLayer {
                            // Title eases down to 86% as you scroll: a collapsing header with no relayout.
                            val p = (scrollState.value / collapsePx).coerceIn(0f, 1f)
                            val k = 1f - 0.14f * p
                            scaleX = k
                            scaleY = k
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                actions()
            }
            // Pinned part (like the amount): it never scrolls, so typing cannot push it out of view.
            if (header != null) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = header,
                )
            }
            val body =
                Modifier.weight(1f).fillMaxWidth().let {
                    if (scroll) {
                        it
                            // Items dissolve into the header and into the bottom bar instead of being cut off by a
                            // hard edge: soft masks at both ends of the viewport. Each one grows with the distance
                            // that is actually left to scroll, so nothing is faded when you are at the very top or bottom.
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                val top = scrollState.value.toFloat().coerceAtMost(fadePx)
                                if (top > 0f) {
                                    drawRect(
                                        Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = top),
                                        size = Size(size.width, top),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                                val max = scrollState.maxValue
                                val bottom = if (max == Int.MAX_VALUE) 0f else (max - scrollState.value).toFloat().coerceIn(0f, fadePx)
                                if (bottom > 0f) {
                                    drawRect(
                                        Brush.verticalGradient(
                                            0f to Color.Black,
                                            1f to Color.Transparent,
                                            startY = size.height - bottom,
                                            endY = size.height,
                                        ),
                                        topLeft = Offset(0f, size.height - bottom),
                                        size = Size(size.width, bottom),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                            }.verticalScroll(scrollState)
                    } else {
                        it
                    }
                }
            Column(body.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
                Spacer(Modifier.height(24.dp))
            }
            bottomBar()
        }
        if (busy) LoadingOverlay(busyText)
    }
}

/** The white bar pinned to the bottom of a form with the main button. */
@Composable
fun BottomActionBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), ambientColor = HeroShadow, spotColor = HeroShadow)
            .background(GlassBar, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Layout for a main tab: header, scrolling body, bottom tab bar. */
@Composable
fun TabScreen(
    title: String,
    current: String,
    nav: NavController,
    actions: @Composable RowScope.() -> Unit = {},
    fab: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            title,
            onBack = null,
            actions = actions,
            scroll = scroll,
            bottomBar = { BottomTabBar(current, nav) },
            content = content,
        )
        if (fab != null) Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 112.dp)) { fab() }
    }
}
