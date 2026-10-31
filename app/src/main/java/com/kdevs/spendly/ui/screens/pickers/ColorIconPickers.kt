package com.kdevs.spendly.ui.screens.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.screens.money.iconLabel
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*

// ------------------------------------------------------------------ Colour, icon, account, date pickers

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerScreen(
    nav: NavController,
    current: String,
    locked: String,
) {
    var chosen by remember { mutableStateOf(current.ifBlank { "blue" }) }

    @Composable fun Swatch(c: ColorSpec) {
        val on = c.key == chosen
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(c.brush)
                .let {
                    if (on) it.border(3.dp, Ink, CircleShape) else it
                }.clickable { chosen = c.key },
            contentAlignment = Alignment.Center,
        ) { if (on) Icon(Icons.Filled.Check, str(R.string.selected_2), tint = Color.White) }
    }
    ScreenScaffold(str(R.string.choose_colour), { nav.popBackStack() }, bottomBar = {
        BottomActionBar {
            PrimaryButton(str(R.string.use_2, colorSpec(chosen).label), {
                nav.setPicked("pickedColor", chosen)
                nav.popBackStack()
            })
        }
    }) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colorSpec(chosen).brush),
            contentAlignment = Alignment.Center,
        ) {
            Text(colorSpec(chosen).label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
        SectionTitle(str(R.string.solid))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SolidColors.forEach { Swatch(it) }
        }
        SectionTitle(str(R.string.gradients))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GradientColors.forEach { Swatch(it) }
        }
        Hint(str(R.string.the_same_colours_and_gradients_are))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerScreen(
    nav: NavController,
    group: String,
    current: String,
) {
    val groups = if (group == "account") AccountIconGroups else CategoryIconGroups
    var chosen by remember { mutableStateOf(current.ifBlank { "other" }) }
    ScreenScaffold(if (group == "account") str(R.string.choose_account_icon) else str(R.string.choose_category_icon), {
        nav.popBackStack()
    }, bottomBar = {
        BottomActionBar {
            PrimaryButton(str(R.string.use_this_icon), {
                nav.setPicked("pickedIcon", chosen)
                nav.popBackStack()
            })
        }
    }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IconCircle(chosen, "blue", 84.dp) }
        groups.forEach { g ->
            SectionTitle(g.title)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                g.keys.forEach { k ->
                    val on = k == chosen
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (on) PrimaryContainer else Surface)
                            .let { if (on) it.border(2.dp, Primary, RoundedCornerShape(20.dp)) else it }
                            .clickable { chosen = k },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(iconFor(k), iconLabel(k), tint = if (on) Primary else Muted, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}
