package com.kdevs.spendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.theme.Danger
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Muted
import com.kdevs.spendly.ui.theme.Primary
import com.kdevs.spendly.ui.theme.Success

private class TypeSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
)

/**
 * Multi-select for transaction kinds: three equal-width tiles (Expense, Income, Transfer).
 *
 * - Fixed width tiles, so selecting one never makes the row jump or wrap.
 * - Each kind has its own icon and colour (red / green / blue), so it reads without colour too.
 * - [selected] holds the keys "expense", "income" and "transfer". The caller owns the rules:
 *   Filter allows none (none = all), Export keeps at least one (guard inside [onToggle]).
 * - [emptyMeansAll] adds a status text next to the label: str(R.string.all_types_included) or the picked names.
 *
 * Put it inside a Card:  Card { TypeTiles(kinds, { kinds = kinds.flip(it) }, emptyMeansAll = true) }
 */
@Composable
fun TypeTiles(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = str(R.string.transaction_types),
    emptyMeansAll: Boolean = false,
    enabled: Boolean = true,
) {
    val specs =
        listOf(
            TypeSpec("expense", str(R.string.expense), Icons.Filled.CallMade, Danger),
            TypeSpec("income", str(R.string.income), Icons.Filled.CallReceived, Success),
            TypeSpec("transfer", str(R.string.transfer), Icons.Filled.SwapHoriz, Primary),
        )
    // TODO: move "All types included" to strings.xml.
    val status =
        if (!emptyMeansAll) {
            null
        } else if (selected.isEmpty()) {
            "All types included"
        } else {
            specs.filter { it.key in selected }.joinToString(", ") { it.label }
        }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, fontSize = 12.sp)
            if (status != null) Text(status, color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            specs.forEach { s ->
                TypeTile(s, s.key in selected, enabled, Modifier.weight(1f)) { onToggle(s.key) }
            }
        }
    }
}

@Composable
private fun TypeTile(
    spec: TypeSpec,
    on: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (on) spec.color.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier
            .clip(shape)
            .background(if (on) spec.color.copy(alpha = 0.12f) else Color.Transparent)
            .border(if (on) 1.dp else 0.5.dp, borderColor, shape)
            .toggleable(value = on, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() }),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.surface else Muted.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(spec.icon, contentDescription = null, tint = if (on) spec.color else Muted, modifier = Modifier.size(20.dp))
            }
            Text(
                spec.label,
                color = if (on) spec.color else Ink,
                fontSize = 13.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
        }
        if (on) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = spec.color,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(14.dp),
            )
        }
    }
}