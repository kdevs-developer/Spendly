package com.kdevs.spendly.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.ui.theme.*

/** Money text that never wraps awkwardly. */
@Composable
fun Money(
    paise: Long,
    color: Color = Ink,
    size: Int = 15,
    withSign: Boolean = false,
) {
    Text(formatPaise(paise, withSign), color = color, fontWeight = FontWeight.ExtraBold, fontSize = size.sp)
}

@Composable
fun LabeledBar(
    label: String,
    value: Long,
    max: Long,
    color: Color,
    right: String,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
            Text(right, fontWeight = FontWeight.Bold, color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Track),
        ) {
            val f = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
