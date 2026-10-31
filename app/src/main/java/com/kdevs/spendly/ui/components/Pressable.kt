package com.kdevs.spendly.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Press feedback for buttons: the button shrinks a little while held and springs back, with a
 * faint dark veil in the button's own shape. There is no ripple, so a rectangle can never show.
 * Put it before shadow()/clip() so the shadow scales with the button.
 */
fun Modifier.pressable(
    shape: Shape,
    enabled: Boolean = true,
    scaleTo: Float = 0.97f,
    veil: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier =
    composed {
        val source = interactionSource ?: remember { MutableInteractionSource() }
        val pressed by source.collectIsPressedAsState()
        val on = pressed && enabled
        val scale by animateFloatAsState(if (on) scaleTo else 1f, spring(dampingRatio = 0.7f, stiffness = 800f), label = "press")
        val shade by animateFloatAsState(if (on && veil) 0.06f else 0f, tween(80), label = "veil")
        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.drawWithContent {
                drawContent()
                if (shade > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), Color.Black.copy(alpha = shade))
            }.clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
    }
