package com.kdevs.spendly.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.theme.*

/** Top bar with a round back button, used by every full screen. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            else Spacer(Modifier.width(4.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.weight(1f), maxLines = 1)
            actions()
        }
        val body = Modifier.weight(1f).fillMaxWidth().let { if (scroll) it.verticalScroll(rememberScrollState()) else it }
        Column(body.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content()
            Spacer(Modifier.height(24.dp))
        }
        bottomBar()
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Ink, bg: Color = Color.White) {
    Box(
        Modifier.size(40.dp).shadow(1.dp, CircleShape).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
            .background(Surface).padding(padding),
        content = content,
    )
}

@Composable
fun HeroCard(modifier: Modifier = Modifier, brush: Brush = HeroBrush, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = Primary, spotColor = Primary)
            .clip(RoundedCornerShape(28.dp)).background(brush).padding(22.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium, color = Ink)
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = Muted,
        textAlign = if (center) TextAlign.Center else TextAlign.Start, lineHeight = 17.sp)
}

/** A tappable row inside a card: leading icon circle, label, value and chevron. */
@Composable
fun SettingRow(
    label: String,
    value: String = "",
    icon: ImageVector? = null,
    iconColor: Color = Muted,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    divider: Boolean = true,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .heightIn(min = 56.dp).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) leading()
            else if (icon != null) Box(Modifier.size(38.dp).clip(CircleShape).background(iconColor), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f)) {
                if (value.isNotEmpty()) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
                    Text(value, style = MaterialTheme.typography.bodyLarge, color = Ink, fontWeight = FontWeight.Bold)
                } else Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink, fontWeight = FontWeight.Bold)
            }
            if (trailing != null) trailing()
            else if (onClick != null) Icon(Icons.Filled.ChevronRight, null, tint = Faint)
        }
        if (divider) HorizontalDivider(color = Line)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, busy: Boolean = false) {
    val alpha = if (enabled && !busy) 1f else 0.45f
    Box(
        modifier.fillMaxWidth().height(54.dp)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(18.dp), ambientColor = Primary, spotColor = Primary)
            .clip(RoundedCornerShape(18.dp)).background(ButtonBrush, alpha = alpha)
            .clickable(enabled = enabled && !busy, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        else Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val c = if (danger) Danger else Primary
    Box(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(18.dp))
            .background(if (danger) DangerBg else PrimaryContainer).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = c, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

/** The white bar pinned to the bottom of a form with the main button. */
@Composable
fun BottomActionBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun AppTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    error: String? = null,
    leading: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        isError = error != null, supportingText = error?.let { { Text(it) } },
        leadingIcon = leading, shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface, unfocusedContainerColor = Surface,
            unfocusedBorderColor = Color(0xFFD8DFEC),
        ),
    )
}

/** Big centred amount input used on money forms. Value is plain rupee text like "1250.50". */
@Composable
fun AmountField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, color: Color = Ink) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("₹", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = color)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { v -> if (v.count { it == '.' } <= 1 && v.all { it.isDigit() || it == '.' } && v.substringAfter('.', "").length <= 2) onChange(v) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 50.sp, fontWeight = FontWeight.ExtraBold, color = color, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.widthIn(min = 80.dp, max = 260.dp).width(IntrinsicSize.Min),
                decorationBox = { inner -> Box(contentAlignment = Alignment.Center) { if (value.isEmpty()) Text("0", fontSize = 50.sp, fontWeight = FontWeight.ExtraBold, color = Faint); inner() } },
            )
        }
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFE5EAF3)).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val bg by animateColorAsState(if (i == selected) Color.White else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(bg).clickable { onSelect(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (i == selected) Primary else Muted) }
        }
    }
}

@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) PrimaryContainer else Color.White, label = "chip")
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg)
            .let { if (selected) it else it.background(Color.White) }
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (selected) Primary else Muted) }
}

@Composable
fun Avatar(name: String, colorKey: String = "blue", size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(colorSpec(colorKey).brush), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.4f).sp)
    }
}

/** Stable colour for a person, from their id. */
fun colorKeyFor(id: String): String = SolidColors[(id.hashCode() and 0x7fffffff) % SolidColors.size].key

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(PrimaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(34.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Hint(body, center = true)
    }
}

/** Dialogs are only for yes/no confirmations. Everything else is a full screen. */
@Composable
fun ConfirmDialog(
    title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Surface,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = { Text(text, color = Muted) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm, color = if (danger) Danger else Primary, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted, fontWeight = FontWeight.Bold) } },
    )
}

private data class Tab(val label: String, val route: String, val icon: ImageVector)
private val tabs = listOf(
    Tab("Home", Routes.HOME, Icons.Filled.Home),
    Tab("Activity", Routes.ACTIVITY, Icons.Filled.Receipt),
    Tab("Reports", Routes.REPORTS, Icons.Filled.PieChart),
    Tab("Accounts", Routes.ACCOUNTS, Icons.Filled.AccountBalanceWallet),
    Tab("Chat", Routes.CHATS, Icons.AutoMirrored.Filled.Chat),
)

/** The five main tabs. Pass the route of the current tab. */
@Composable
fun BottomTabBar(current: String, nav: NavController) {
    Row(
        Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        tabs.forEach { t ->
            val on = t.route == current
            val bg by animateColorAsState(if (on) PrimaryContainer else Color.Transparent, label = "tab")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable {
                    if (!on) nav.navigate(t.route) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true }
                }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 18.dp, vertical = 4.dp)) {
                    Icon(t.icon, t.label, tint = if (on) Primary else Muted, modifier = Modifier.size(22.dp))
                }
                Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (on) Primary else Muted)
            }
        }
    }
}

/** Layout for a main tab: header, scrolling body, bottom tab bar. */
@Composable
fun TabScreen(
    title: String, current: String, nav: NavController,
    actions: @Composable RowScope.() -> Unit = {},
    fab: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(title, onBack = null, actions = actions, scroll = scroll, bottomBar = { BottomTabBar(current, nav) }, content = content)
        if (fab != null) Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 92.dp)) { fab() }
    }
}

@Composable
fun Fab(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(58.dp).shadow(10.dp, CircleShape, ambientColor = Primary, spotColor = Primary).clip(CircleShape)
            .background(ButtonBrush).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White) }
}

/** Dismisses a screen after an async save and shows a failure message if it throws. */
@Composable
fun rememberSaver(): Saver = remember { Saver() }

class Saver {
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    fun run(scope: kotlinx.coroutines.CoroutineScope, onDone: () -> Unit, block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launchSafe(
            onError = { error = it.message ?: "Something went wrong"; busy = false },
        ) { block(); busy = false; onDone() }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchSafe(onError: (Throwable) -> Unit, block: suspend () -> Unit) {
    this.launch { try { block() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Throwable) { onError(e) } }
}
