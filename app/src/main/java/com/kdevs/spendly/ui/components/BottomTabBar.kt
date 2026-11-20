package com.kdevs.spendly.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.theme.Danger
import com.kdevs.spendly.ui.theme.GlassBar
import com.kdevs.spendly.ui.theme.GlassBorder
import com.kdevs.spendly.ui.theme.HeroShadow
import com.kdevs.spendly.ui.theme.Muted
import com.kdevs.spendly.ui.theme.Primary
import com.kdevs.spendly.ui.theme.PrimaryContainer

private data class Tab(
    val label: String,
    val route: String,
    val icon: ImageVector,
)

private val tabs =
    listOf(
        Tab(str(R.string.home), Routes.HOME, Icons.Filled.Home),
        Tab(str(R.string.activity), Routes.ACTIVITY, Icons.Filled.Receipt),
        Tab(str(R.string.reports), Routes.REPORTS, Icons.Filled.PieChart),
        Tab(str(R.string.accounts), Routes.ACCOUNTS, Icons.Filled.AccountBalanceWallet),
        Tab(str(R.string.chat), Routes.CHATS, Icons.AutoMirrored.Filled.Chat),
    )

/**
 * Switches main tab. Always lands on the tab itself with only Home underneath, so a transaction edit/detail opened
 * from a tab is closed instead of being saved and restored (that made Home look dead until Back was pressed).
 */
fun NavController.navigateToTab(route: String) {
    if (route == Routes.HOME) {
        if (!popBackStack(Routes.HOME, false)) {
            navigate(Routes.HOME) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
        return
    }
    navigate(route) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}

/** The five main tabs. Pass the route of the current tab. */
@Composable
fun BottomTabBar(
    current: String,
    nav: NavController,
) {
    val unread by com.kdevs.spendly.data.Inbox.unreadTotal
        .collectAsState()
    // Floating glass pill: lifted off the screen edges instead of a full-width bar.
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(32.dp), ambientColor = HeroShadow, spotColor = HeroShadow)
            .background(GlassBar, RoundedCornerShape(32.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        tabs.forEach { t ->
            val on = t.route == current
            val bg by animateColorAsState(if (on) PrimaryContainer else Color.Transparent, tween(200), label = "tab")
            Column(
                Modifier
                    .weight(1f)
                    .pressable(RoundedCornerShape(16.dp), scaleTo = 0.92f, veil = false) {
                        if (!on) nav.navigateToTab(t.route)
                    }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 18.dp, vertical = 4.dp)) {
                    Icon(t.icon, t.label, tint = if (on) Primary else Muted, modifier = Modifier.size(22.dp))
                    if (t.route == Routes.CHATS && unread > 0) {
                        Box(
                            Modifier
                                .align(
                                    Alignment.TopEnd,
                                ).offset(x = 10.dp, y = (-4).dp)
                                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                .clip(CircleShape)
                                .background(Danger)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (unread >
                                    99
                                ) {
                                    "99+"
                                } else {
                                    "$unread"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
                Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (on) Primary else Muted)
            }
        }
    }
}
