package com.kdevs.spendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Primary
import com.kdevs.spendly.ui.theme.Surface

/** Dims the screen, blocks taps and shows a spinner card while something slow runs. */
@Composable
fun LoadingOverlay(message: String? = null) {
    Box(
        Modifier.fillMaxSize().background(clr(R.color.scrim))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.clip(RoundedCornerShape(22.dp)).background(Surface).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(Modifier.size(34.dp), color = Primary, strokeWidth = 3.dp)
            Text(message ?: str(R.string.please_wait), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/** Centred spinner for a list that has not received its first data yet. */
@Composable
fun InlineLoader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(30.dp), color = Primary, strokeWidth = 3.dp)
    }
}
