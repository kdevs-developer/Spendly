package com.kdevs.spendly.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.nav.Routes
import com.kdevs.spendly.ui.theme.Bg
import com.kdevs.spendly.ui.theme.Brand
import com.kdevs.spendly.ui.theme.ButtonBrush
import com.kdevs.spendly.ui.theme.Danger
import com.kdevs.spendly.ui.theme.DangerBg
import com.kdevs.spendly.ui.theme.Faint
import com.kdevs.spendly.ui.theme.FieldBorder
import com.kdevs.spendly.ui.theme.HeroBrush
import com.kdevs.spendly.ui.theme.Ink
import com.kdevs.spendly.ui.theme.Line
import com.kdevs.spendly.ui.theme.Muted
import com.kdevs.spendly.ui.theme.Primary
import com.kdevs.spendly.ui.theme.PrimaryContainer
import com.kdevs.spendly.ui.theme.SolidColors
import com.kdevs.spendly.ui.theme.Surface
import com.kdevs.spendly.ui.theme.Track
import com.kdevs.spendly.ui.theme.colorSpec
import kotlinx.coroutines.launch

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
    content: @Composable ColumnScope.() -> Unit,
) {
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
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.weight(1f), maxLines = 1)
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
}

/**
 * Press feedback for buttons: the button shrinks a little while held and springs back, with a
 * faint dark veil in the button's own shape. There is no ripple, so a rectangle can never show.
 * Put it before shadow()/clip() so the shadow scales with the button.
 */
fun Modifier.pressable(
    shape: Shape,
    enabled: Boolean = true,
    scaleTo: Float = 0.97f,
    veil: Boolean = true,
    onClick: () -> Unit,
): Modifier =
    composed {
        val source = remember { MutableInteractionSource() }
        val pressed by source.collectIsPressedAsState()
        val on = pressed && enabled
        val scale by animateFloatAsState(if (on) scaleTo else 1f, spring(dampingRatio = 0.7f, stiffness = 800f), label = "press")
        val shade by animateFloatAsState(if (on && veil) 0.10f else 0f, tween(80), label = "veil")
        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawWithContent {
                drawContent()
                if (shade > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), Color.Black.copy(alpha = shade))
            }
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
    }

@Composable
fun RoundIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Ink,
    bg: Color = Surface,
) {
    Box(
        Modifier.size(40.dp).pressable(CircleShape, scaleTo = 0.92f, onClick = onClick)
            .shadow(1.dp, CircleShape).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(20.dp)) }
}

/** A white (or dark) rounded card. Pass onClick to make the whole card tappable; the press stays inside the corners. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().shadow(1.dp, shape).clip(shape).background(Surface)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(padding),
        content = content,
    )
}

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    brush: Brush = HeroBrush,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = Brand, spotColor = Brand)
            .clip(RoundedCornerShape(28.dp)).background(brush).padding(22.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium, color = Ink)
}

@Composable
fun Hint(
    text: String,
    modifier: Modifier = Modifier,
    center: Boolean = false,
) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.labelMedium,
        color = Muted,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        lineHeight = 17.sp,
    )
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
            if (leading != null) {
                leading()
            } else if (icon != null) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(iconColor), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                if (value.isNotEmpty()) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
                    Text(value, style = MaterialTheme.typography.bodyLarge, color = Ink, fontWeight = FontWeight.Bold)
                } else {
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(Icons.Filled.ChevronRight, null, tint = Faint)
            }
        }
        if (divider) HorizontalDivider(color = Line)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    val lift by animateDpAsState(if (enabled) 8.dp else 0.dp, label = "lift")
    Box(
        modifier.fillMaxWidth().height(54.dp)
            .pressable(shape, enabled = enabled && !busy, onClick = onClick)
            .shadow(lift, shape, ambientColor = Brand, spotColor = Brand)
            .clip(shape).background(ButtonBrush),
        contentAlignment = Alignment.Center,
    ) {
        // The button always stays solid. Disabled is a pale wash on top, never see-through,
        // so the shadow behind it can no longer show as a box.
        if (!enabled) Box(Modifier.matchParentSize().background(Bg.copy(alpha = 0.62f)))
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Text(text, color = if (enabled) Color.White else Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val c = if (danger) Danger else Primary
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.fillMaxWidth().height(50.dp).pressable(shape, onClick = onClick)
            .clip(shape).background(if (danger) DangerBg else PrimaryContainer),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = c, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

/** "Continue with Google" button with the Google logo. */
@Composable
fun GoogleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().height(52.dp).pressable(shape, enabled = enabled, onClick = onClick)
            .clip(shape).background(Surface).border(1.dp, FieldBorder, shape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** A thin line with a word in the middle, e.g. "or". */
@Composable
fun OrDivider(text: String = "or") {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = Line)
        Text(text, color = Faint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(Modifier.weight(1f), color = Line)
    }
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
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                unfocusedBorderColor = FieldBorder,
            ),
    )
}

/**
 * Big centred amount input. Value is plain rupee text like "1250.50".
 * The field is exactly as wide as its text (measured, not guessed), the size steps down for long
 * amounts, and the row has a fixed height, so nothing is clipped and nothing jumps while typing.
 */
@Composable
fun AmountField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Ink,
) {
    val digits = value.length
    val size =
        when {
            digits > 11 -> 32.sp
            digits > 9 -> 38.sp
            digits > 7 -> 44.sp
            else -> 50.sp
        }
    val style =
        androidx.compose.ui.text.TextStyle(
            fontSize = size,
            lineHeight = (size.value * 1.2f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            textAlign = TextAlign.Start,
        )
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val textWidth =
        with(density) { measurer.measure(androidx.compose.ui.text.AnnotatedString(value.ifEmpty { "0" }), style).size.width.toDp() }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    Row(
        modifier.fillMaxWidth().height(76.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.requestFocus() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "₹",
            fontSize = (size.value * 0.6f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(end = 6.dp),
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = {
                    v ->
                if (v.count { it == '.' } <= 1 && v.all { it.isDigit() || it == '.' } && v.substringAfter('.', "").length <= 2) onChange(v)
            },
            textStyle = style,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(color),
            modifier = Modifier.width((textWidth + 4.dp).coerceIn(28.dp, 250.dp)).focusRequester(focus),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("0", style = style.copy(color = Faint))
                    inner()
                }
            },
        )
    }
}

/** Two or three options with a white pill that slides to the chosen one. */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = RoundedCornerShape(16.dp)
    val inner = RoundedCornerShape(12.dp)
    BoxWithConstraints(modifier.fillMaxWidth().clip(outer).background(Track).padding(4.dp)) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, spring(dampingRatio = 0.85f, stiffness = 380f), label = "segX")
        Box(
            Modifier.offset { IntOffset(x.roundToPx(), 0) }.width(segment).height(38.dp)
                .shadow(2.dp, inner).background(Surface, inner),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val c by animateColorAsState(if (i == selected) Primary else Muted, tween(180), label = "segText")
                Box(
                    Modifier.weight(1f).height(38.dp).clip(inner)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c) }
            }
        }
    }
}

@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val pill = RoundedCornerShape(999.dp)
    val bg by animateColorAsState(if (selected) PrimaryContainer else Surface, tween(160), label = "chip")
    val fg by animateColorAsState(if (selected) Primary else Muted, tween(160), label = "chipText")
    Box(
        Modifier.pressable(pill, scaleTo = 0.95f, veil = false, onClick = onClick)
            .clip(pill).background(bg).padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = fg) }
}

@Composable
fun Avatar(
    name: String,
    colorKey: String = "blue",
    size: Dp = 44.dp,
) {
    Box(Modifier.size(size).clip(CircleShape).background(colorSpec(colorKey).brush), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.4f).sp)
    }
}

/** Stable colour for a person, from their id. */
fun colorKeyFor(id: String): String = SolidColors[(id.hashCode() and 0x7fffffff) % SolidColors.size].key

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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

private data class Tab(val label: String, val route: String, val icon: ImageVector)

private val tabs =
    listOf(
        Tab(str(R.string.home), Routes.HOME, Icons.Filled.Home),
        Tab(str(R.string.activity), Routes.ACTIVITY, Icons.Filled.Receipt),
        Tab(str(R.string.reports), Routes.REPORTS, Icons.Filled.PieChart),
        Tab(str(R.string.accounts), Routes.ACCOUNTS, Icons.Filled.AccountBalanceWallet),
        Tab(str(R.string.chat), Routes.CHATS, Icons.AutoMirrored.Filled.Chat),
    )

/** The five main tabs. Pass the route of the current tab. */
@Composable
fun BottomTabBar(
    current: String,
    nav: NavController,
) {
    val unread by com.kdevs.spendly.data.Inbox.unreadTotal.collectAsState()
    Row(
        Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        tabs.forEach { t ->
            val on = t.route == current
            val bg by animateColorAsState(if (on) PrimaryContainer else Color.Transparent, tween(200), label = "tab")
            Column(
                Modifier.weight(1f).pressable(RoundedCornerShape(16.dp), scaleTo = 0.92f, veil = false) {
                    if (!on) {
                        nav.navigate(t.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 18.dp, vertical = 4.dp)) {
                    Icon(t.icon, t.label, tint = if (on) Primary else Muted, modifier = Modifier.size(22.dp))
                    if (t.route == Routes.CHATS && unread > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-4).dp).defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                .clip(CircleShape).background(Danger).padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (unread > 99) "99+" else "$unread", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (on) Primary else Muted)
            }
        }
    }
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

@Composable
fun Fab(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(58.dp).pressable(CircleShape, scaleTo = 0.92f, onClick = onClick)
            .shadow(10.dp, CircleShape, ambientColor = Brand, spotColor = Brand).clip(CircleShape).background(ButtonBrush),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Color.White) }
}

/** Dismisses a screen after an async save and shows a failure message if it throws. */
@Composable
fun rememberSaver(): Saver = remember { Saver() }

class Saver {
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun run(
        scope: kotlinx.coroutines.CoroutineScope,
        onDone: () -> Unit,
        block: suspend () -> Unit,
    ) {
        if (busy) return
        busy = true
        error = null
        scope.launchSafe(
            onError = {
                error = it.message ?: str(R.string.something_went_wrong_2)
                busy = false
            },
        ) {
            block()
            busy = false
            onDone()
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchSafe(
    onError: (Throwable) -> Unit,
    block: suspend () -> Unit,
) {
    this.launch {
        try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }
}