package com.kdevs.spendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.theme.Bg
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Surface

/** Top bar with a round back button, used by every full screen. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    scroll: Boolean = true,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    busy: Boolean = false,
    busyText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, str(R.string.back), onBack)
                } else {
                    Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                actions()
            }
            // Pinned part (like the amount): it never scrolls, so typing cannot push it out of view.
            if (header != null) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = header,
                )
            }
            val body = Modifier.weight(1f).fillMaxWidth().let { if (scroll) it.verticalScroll(rememberScrollState()) else it }
            Column(body.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
                Spacer(Modifier.height(24.dp))
            }
            bottomBar()
        }
        if (busy) LoadingOverlay(busyText)
    }
}

/** The white bar pinned to the bottom of a form with the main button. */
@Composable
fun BottomActionBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Layout for a main tab: header, scrolling body, bottom tab bar. */
@Composable
fun TabScreen(
    title: String,
    current: String,
    nav: NavController,
    actions: @Composable RowScope.() -> Unit = {},
    fab: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            title,
            onBack = null,
            actions = actions,
            scroll = scroll,
            bottomBar = { BottomTabBar(current, nav) },
            content = content,
        )
        if (fab != null) Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 92.dp)) { fab() }
    }
}
