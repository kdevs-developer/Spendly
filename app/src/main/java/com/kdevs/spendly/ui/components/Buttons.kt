package com.kdevs.spendly.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.ui.theme.Bg
import com.kdevs.spendly.ui.theme.Brand
import com.kdevs.spendly.ui.theme.ButtonBrush
import com.kdevs.spendly.ui.theme.Danger
import com.kdevs.spendly.ui.theme.DangerBg
import com.kdevs.spendly.ui.theme.Faint
import com.kdevs.spendly.ui.theme.FieldBorder
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Line
import com.kdevs.spendly.ui.theme.Muted
import com.kdevs.spendly.ui.theme.Primary
import com.kdevs.spendly.ui.theme.PrimaryContainer
import com.kdevs.spendly.ui.theme.Surface

@Composable
fun RoundIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Ink,
    bg: Color = Surface,
) {
    Box(
        Modifier
            .size(40.dp)
            .pressable(CircleShape, scaleTo = 0.92f, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val lift by animateDpAsState(
        if (!enabled) {
            0.dp
        } else if (pressed) {
            1.dp
        } else {
            5.dp
        },
        tween(120),
        label = "lift",
    )
    val glow = Brand.copy(alpha = 0.30f)
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .pressable(shape, enabled = enabled && !busy, veil = false, interactionSource = source, onClick = onClick)
            .shadow(lift, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(ButtonBrush),
        contentAlignment = Alignment.Center,
    ) {
        // The button always stays solid. Disabled is a pale wash on top, never see-through,
        // so the shadow behind it can no longer show as a box.
        if (!enabled) Box(Modifier.matchParentSize().background(Bg.copy(alpha = 0.62f)))
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Text(text, color = if (enabled) Color.White else Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val c = if (danger) Danger else Primary
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .pressable(shape, onClick = onClick)
            .clip(shape)
            .background(if (danger) DangerBg else PrimaryContainer),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = c, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

/** "Continue with Google" button with the Google logo. */
@Composable
fun GoogleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .pressable(shape, enabled = enabled, onClick = onClick)
            .clip(shape)
            .background(Surface)
            .border(1.dp, FieldBorder, shape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** A thin line with a word in the middle, e.g. "or". */
@Composable
fun OrDivider(text: String = "or") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = Line)
        Text(text, color = Faint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(Modifier.weight(1f), color = Line)
    }
}

@Composable
fun Fab(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed) 2.dp else 7.dp, tween(120), label = "fabLift")
    val glow = Brand.copy(alpha = 0.30f)
    Box(
        Modifier
            .size(58.dp)
            .pressable(CircleShape, scaleTo = 0.92f, veil = false, interactionSource = source, onClick = onClick)
            .shadow(lift, CircleShape, ambientColor = glow, spotColor = glow)
            .clip(CircleShape)
            .background(ButtonBrush),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White) }
}
