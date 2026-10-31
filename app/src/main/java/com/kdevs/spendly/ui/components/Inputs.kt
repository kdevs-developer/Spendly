package com.kdevs.spendly.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.ui.theme.Faint
import com.kdevs.spendly.ui.theme.FieldBorder
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Muted
import com.kdevs.spendly.ui.theme.Primary
import com.kdevs.spendly.ui.theme.PrimaryContainer
import com.kdevs.spendly.ui.theme.Surface
import com.kdevs.spendly.ui.theme.Track

@Composable
fun AppTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    error: String? = null,
    leading: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        isError = error != null, supportingText = error?.let { { Text(it) } },
        leadingIcon = leading, shape = RoundedCornerShape(16.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                unfocusedBorderColor = FieldBorder,
            ),
    )
}

/**
 * Big centred amount input. Value is plain rupee text like "1250.50".
 * The field is exactly as wide as its text (measured, not guessed), the size steps down for long
 * amounts, and the row has a fixed height, so nothing is clipped and nothing jumps while typing.
 */
@Composable
fun AmountField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Ink,
) {
    val digits = value.length
    val size =
        when {
            digits > 11 -> 32.sp
            digits > 9 -> 38.sp
            digits > 7 -> 44.sp
            else -> 50.sp
        }
    val style =
        androidx.compose.ui.text.TextStyle(
            fontSize = size,
            lineHeight = (size.value * 1.2f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            textAlign = TextAlign.Start,
        )
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val textWidth =
        with(density) { measurer.measure(androidx.compose.ui.text.AnnotatedString(value.ifEmpty { "0" }), style).size.width.toDp() }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    Row(
        modifier.fillMaxWidth().height(76.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.requestFocus() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "₹",
            fontSize = (size.value * 0.6f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(end = 6.dp),
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = {
                    v ->
                if (v.count { it == '.' } <= 1 && v.all { it.isDigit() || it == '.' } && v.substringAfter('.', "").length <= 2) onChange(v)
            },
            textStyle = style,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(color),
            modifier = Modifier.width((textWidth + 4.dp).coerceIn(28.dp, 250.dp)).focusRequester(focus),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("0", style = style.copy(color = Faint))
                    inner()
                }
            },
        )
    }
}

/** Two or three options with a white pill that slides to the chosen one. */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = RoundedCornerShape(16.dp)
    val inner = RoundedCornerShape(12.dp)
    BoxWithConstraints(modifier.fillMaxWidth().clip(outer).background(Track).padding(4.dp)) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, spring(dampingRatio = 0.85f, stiffness = 380f), label = "segX")
        Box(
            Modifier.offset { IntOffset(x.roundToPx(), 0) }.width(segment).height(38.dp)
                .shadow(2.dp, inner).background(Surface, inner),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val c by animateColorAsState(if (i == selected) Primary else Muted, tween(180), label = "segText")
                Box(
                    Modifier.weight(1f).height(38.dp).clip(inner)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c) }
            }
        }
    }
}

@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val pill = RoundedCornerShape(999.dp)
    val bg by animateColorAsState(if (selected) PrimaryContainer else Surface, tween(160), label = "chip")
    val fg by animateColorAsState(if (selected) Primary else Muted, tween(160), label = "chipText")
    Box(
        Modifier.pressable(pill, scaleTo = 0.95f, veil = false, onClick = onClick)
            .clip(pill).background(bg).padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = fg) }
}
