package com.kdevs.spendly.ui.screens.money

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

/** A row inside a form card that opens a picker screen. */
@Composable
fun PickRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Muted,
    onClick: () -> Unit,
    divider: Boolean = true,
) {
    SettingRow(label, value.ifBlank { str(R.string.choose) }, icon = icon, iconColor = tint, onClick = onClick, divider = divider)
}

@Composable
fun PickRowLeading(
    label: String,
    value: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    divider: Boolean = true,
) {
    SettingRow(label, value.ifBlank { str(R.string.choose) }, leading = leading, onClick = onClick, divider = divider)
}

@Composable
fun SavedBar(
    text: String,
    saver: Saver,
    enabled: Boolean = true,
    onSave: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    BottomActionBar {
        saver.error?.let { Text(it, color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        PrimaryButton(text, onSave, enabled = enabled, busy = saver.busy)
        extra()
    }
}

/** Encodes the category picker result as "parentId|subId". */
fun splitPick(v: String): Pair<String, String> = v.substringBefore('|') to v.substringAfter('|', "")

fun iconLabel(key: String) = key.replaceFirstChar { it.uppercase() }
