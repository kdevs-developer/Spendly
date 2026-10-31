package com.kdevs.spendly.ui.screens.money

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.theme.IconCircle
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Muted
import androidx.core.content.edit

/**
 * Remembers, per transaction type ("expense" / "income"), the last account used and the
 * most recent category + sub-category pairs. Stored on this phone only.
 */
object RecentPicks {
    private const val PREFS = "spendly_recent_picks"
    private const val MAX = 8

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lastAccount(
        c: Context,
        kind: String,
    ): String = prefs(c).getString("acc_$kind", "") ?: ""

    /** Most recent first. Each pair is (categoryId, subcategoryId); subcategoryId may be empty. */
    fun categories(
        c: Context,
        kind: String,
    ): List<Pair<String, String>> =
        (prefs(c).getString("cat_$kind", "") ?: "")
            .split(",")
            .filter { it.contains("|") }
            .map {
                val parts = it.split("|", limit = 2)
                parts[0] to parts[1]
            }

    fun remember(
        c: Context,
        kind: String,
        accountId: String,
        catId: String,
        subId: String,
    ) {
        val list = categories(c, kind).toMutableList()
        if (catId.isNotBlank()) {
            list.remove(catId to subId)
            list.add(0, catId to subId)
        }
        prefs(c)
            .edit {
                putString("acc_$kind", accountId)
                    .putString(
                        "cat_$kind",
                        list.take(MAX).joinToString(",") { "${it.first}|${it.second}" })
            }
    }
}

class QuickItem(
    val catId: String,
    val subId: String,
    val label: String,
    val icon: String,
    val colorKey: String,
)

@Composable
fun QuickCategories(
    items: List<QuickItem>,
    selectedCat: String,
    selectedSub: String,
    onPick: (QuickItem) -> Unit,
    onMore: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items.forEach { q ->
            val selected = q.catId == selectedCat && q.subId == selectedSub
            Column(
                Modifier
                    .width(62.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPick(q) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    IconCircle(q.icon, q.colorKey, 48.dp)
                }
                Text(
                    q.label,
                    fontSize = 12.sp,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(
            Modifier
                .width(62.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onMore),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .padding(4.dp)
                    .border(0.5.dp, Muted.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MoreHoriz, str(R.string.more), tint = Muted)
            }
            Text(str(R.string.more), fontSize = 12.sp, color = Ink, maxLines = 1)
        }
    }
}