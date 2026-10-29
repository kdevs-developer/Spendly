package com.kdevs.spendly.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.data.Inbox
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Muted
import com.kdevs.spendly.ui.theme.Surface
import kotlinx.coroutines.delay

/** A strip that slides in from the top when a message arrives in a chat you are not looking at. Tap to open it. */
@Composable
fun MessageBanner(nav: NavController) {
    val banner by Inbox.banner.collectAsState()
    var shown by remember { mutableStateOf<Inbox.Banner?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(banner?.key) {
        val b = banner ?: return@LaunchedEffect
        shown = b
        visible = true
        delay(4000)
        visible = false
        delay(300)
        if (Inbox.banner.value?.key == b.key) Inbox.banner.value = null
    }

    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            val b = shown
            if (b != null) {
                val shape = RoundedCornerShape(20.dp)
                Row(
                    Modifier.fillMaxWidth().shadow(8.dp, shape).clip(shape).background(Surface).clickable {
                        visible = false
                        Inbox.banner.value = null
                        nav.navigate(Routes.chat(b.chatId)) { launchSingleTop = true }
                    }.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(b.title, colorKeyFor(b.chatId), 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(b.title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(b.body, color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}