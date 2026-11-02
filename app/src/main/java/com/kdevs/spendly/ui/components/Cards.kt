package com.kdevs.spendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.ui.theme.Faint
import com.kdevs.spendly.ui.theme.HeroBrush
import com.kdevs.spendly.ui.theme.HeroHairline
import com.kdevs.spendly.ui.theme.HeroShadow
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Line
import com.kdevs.spendly.ui.theme.Muted
import com.kdevs.spendly.ui.theme.Primary
import com.kdevs.spendly.ui.theme.PrimaryContainer
import com.kdevs.spendly.ui.theme.SolidColors
import com.kdevs.spendly.ui.theme.Surface
import com.kdevs.spendly.ui.theme.colorSpec

/** A white (or dark) rounded card. Pass onClick to make the whole card tappable; the press stays inside the corners. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(1.dp, Line, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(padding),
        content = content,
    )
}

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    brush: Brush = HeroBrush,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = HeroShadow, spotColor = HeroShadow)
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
            .border(1.dp, HeroHairline, RoundedCornerShape(28.dp))
            .padding(22.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium, color = Ink)
}

@Composable
fun Hint(
    text: String,
    modifier: Modifier = Modifier,
    center: Boolean = false,
) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.labelMedium,
        color = Muted,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        lineHeight = 17.sp,
    )
}

/** A tappable row inside a card: leading icon circle, label, value and chevron. */
@Composable
fun SettingRow(
    label: String,
    value: String = "",
    icon: ImageVector? = null,
    iconColor: Color = Muted,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    divider: Boolean = true,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .heightIn(min = 56.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) {
                leading()
            } else if (icon != null) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(iconColor), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                if (value.isNotEmpty()) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
                    Text(value, style = MaterialTheme.typography.bodyLarge, color = Ink, fontWeight = FontWeight.Bold)
                } else {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(Icons.Filled.ChevronRight, null, tint = Faint)
            }
        }
        if (divider) HorizontalDivider(color = Line)
    }
}

@Composable
fun Avatar(
    name: String,
    colorKey: String = "blue",
    size: Dp = 44.dp,
) {
    Box(Modifier.size(size).clip(CircleShape).background(colorSpec(colorKey).brush), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.4f).sp)
    }
}

/** Stable colour for a person, from their id. */
fun colorKeyFor(id: String): String = SolidColors[(id.hashCode() and 0x7fffffff) % SolidColors.size].key

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(PrimaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(34.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Hint(body, center = true)
    }
}
