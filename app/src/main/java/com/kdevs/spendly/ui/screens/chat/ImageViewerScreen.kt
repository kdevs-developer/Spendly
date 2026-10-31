package com.kdevs.spendly.ui.screens.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.theme.*

@Composable
fun ImageViewerScreen(
    nav: NavController,
    chatId: String,
    msgId: String,
) {
    val msgs by remember(chatId) { Repo.messages(chatId) }.collectAsState(initial = emptyList())
    val images = msgs.filter { it.type == "image" && !it.deleted }
    var index by remember(images.size) { mutableIntStateOf(images.indexOfFirst { it.id == msgId }.coerceAtLeast(0)) }
    val cur = images.getOrNull(index)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(clr(R.color.viewer_bg)).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                Icons.Filled.Close,
                str(R.string.close),
                { nav.popBackStack() },
                tint = Color.White,
                bg = Color.White.copy(alpha = 0.12f),
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cur?.senderName.orEmpty(), color = Color.White, fontWeight = FontWeight.ExtraBold)
                if (cur != null) {
                    Text(
                        str(R.string.of, timeOf(cur.millis()), index + 1, images.size),
                        color = clr(R.color.viewer_text),
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.size(40.dp))
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(clr(R.color.viewer_surface)),
            contentAlignment = Alignment.Center,
        ) {
            if (cur != null) AsyncImage(cur.imageUrl, str(R.string.photo), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        if (images.size > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                images.take(7).forEachIndexed { i, m ->
                    AsyncImage(
                        m.imageUrl,
                        null,
                        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).clickable {
                            index = i
                        }.background(
                            if (i == index) Color.White else Color.Transparent,
                        ).padding(if (i == index) 2.dp else 0.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        alpha = if (i == index) 1f else 0.6f,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            ViewerAction(str(R.string.share), Icons.Filled.Share, Color.White) {
                cur?.let {
                    val i =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, it.imageUrl)
                        }
                    ctx.startActivity(Intent.createChooser(i, str(R.string.share_photo)))
                }
            }
            ViewerAction(str(R.string.open), Icons.Filled.OpenInBrowser, Color.White) {
                cur?.let { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it.imageUrl))) }
            }
            ViewerAction(str(R.string.hide), Icons.Filled.VisibilityOff, clr(R.color.viewer_danger)) {
                cur?.let {
                    scope.launchIO { Repo.deleteForMe(it) }
                    nav.popBackStack()
                }
            }
        }
    }
}

@Composable
private fun ViewerAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = tint)
        }
        Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
