package com.kdevs.spendly.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.theme.Danger
import com.kdevs.spendly.ui.theme.Muted
import com.kdevs.spendly.ui.theme.Primary
import com.kdevs.spendly.ui.theme.Surface

/** Dialogs are only for yes/no confirmations. Everything else is a full screen. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Surface,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = { Text(text, color = Muted) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirm, color = if (danger) Danger else Primary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str(R.string.cancel), color = Muted, fontWeight = FontWeight.Bold) } },
    )
}
