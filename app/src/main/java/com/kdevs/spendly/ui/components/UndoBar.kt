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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Undo
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The bar at the bottom that says what just happened and offers Undo. Hides itself after a few seconds. */
@Composable
fun UndoBar() {
    val item by Undo.current.collectAsState()
    var shown by remember { mutableStateOf<Undo.Item?>(null) }
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(item?.key) {
        val it = item ?: return@LaunchedEffect
        shown = it
        visible = true
        delay(if (it.restore != null) 6000 else 5000)
        visible = false
        delay(300)
        if (Undo.current.value?.key == it.key) Undo.current.value = null
    }

    Box(
        Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 84.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            val s = shown
            if (s != null) {
                val shape = RoundedCornerShape(18.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, shape)
                        .clip(shape)
                        .background(Ink)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(s.message, Modifier.weight(1f), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    val restore = s.restore
                    if (restore != null) {
                        Text(
                            str(R.string.undo),
                            color = Color(0xFF8FB8FF),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        visible = false
                                        Undo.current.value = null
                                        scope.launch { runCatching { restore() } }
                                    }.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
