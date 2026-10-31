package com.kdevs.spendly.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kdevs.spendly.R
import com.kdevs.spendly.data.clr
import com.kdevs.spendly.data.str

/** Solid colours and gradients. A colour is stored as a key string; "g_" keys are gradients. */
data class ColorSpec(val key: String, val label: String, val colors: List<Color>) {
    val isGradient get() = colors.size > 1
    val solid get() = colors.first()
    val brush: Brush get() = if (isGradient) Brush.linearGradient(colors) else Brush.linearGradient(listOf(colors[0], colors[0]))
}

val SolidColors =
    listOf(
        ColorSpec("blue", str(R.string.blue), listOf(clr(R.color.swatch_blue))),
        ColorSpec("purple", str(R.string.purple), listOf(clr(R.color.swatch_purple))),
        ColorSpec("green", str(R.string.green), listOf(clr(R.color.swatch_green))),
        ColorSpec("orange", str(R.string.orange), listOf(clr(R.color.swatch_orange))),
        ColorSpec("pink", str(R.string.pink), listOf(clr(R.color.swatch_pink))),
        ColorSpec("teal", str(R.string.teal), listOf(clr(R.color.swatch_teal))),
        ColorSpec("red", str(R.string.red), listOf(clr(R.color.swatch_red))),
        ColorSpec("slate", str(R.string.slate), listOf(clr(R.color.swatch_slate))),
        // More solids. Defined here so no extra XML is needed.
        ColorSpec("indigo", str(R.string.palette_indigo_name), listOf(clr(R.color.palette_indigo))),
        ColorSpec("violet", str(R.string.palette_violet_name), listOf(clr(R.color.palette_violet))),
        ColorSpec("fuchsia", str(R.string.palette_fuchsia_name), listOf(clr(R.color.palette_fuchsia))),
        ColorSpec("rose", str(R.string.palette_rose_name), listOf(clr(R.color.palette_rose))),
        ColorSpec("coral", str(R.string.palette_coral_name), listOf(clr(R.color.palette_coral))),
        ColorSpec("amber", str(R.string.palette_amber_name), listOf(clr(R.color.palette_amber))),
        ColorSpec("gold", str(R.string.palette_gold_name), listOf(clr(R.color.palette_gold))),
        ColorSpec("lime", str(R.string.palette_lime_name), listOf(clr(R.color.palette_lime))),
        ColorSpec("emerald", str(R.string.palette_emerald_name), listOf(clr(R.color.palette_emerald))),
        ColorSpec("mint", str(R.string.palette_mint_name), listOf(clr(R.color.palette_mint))),
        ColorSpec("cyan", str(R.string.palette_cyan_name), listOf(clr(R.color.palette_cyan))),
        ColorSpec("sky", str(R.string.palette_sky_name), listOf(clr(R.color.palette_sky))),
        ColorSpec("navy", str(R.string.palette_navy_name), listOf(clr(R.color.palette_navy))),
        ColorSpec("maroon", str(R.string.palette_maroon_name), listOf(clr(R.color.palette_maroon))),
        ColorSpec("brown", str(R.string.palette_brown_name), listOf(clr(R.color.palette_brown))),
        ColorSpec("olive", str(R.string.palette_olive_name), listOf(clr(R.color.palette_olive))),
        ColorSpec("graphite", str(R.string.palette_graphite_name), listOf(clr(R.color.palette_graphite))),
        ColorSpec("steel", str(R.string.palette_steel_name), listOf(clr(R.color.palette_steel))),
    )

val GradientColors =
    listOf(
        ColorSpec("g_sunset", str(R.string.sunset), listOf(clr(R.color.swatch_deep_orange), clr(R.color.swatch_pink))),
        ColorSpec("g_ocean", str(R.string.ocean), listOf(clr(R.color.swatch_sky), clr(R.color.swatch_teal))),
        ColorSpec("g_aurora", str(R.string.aurora), listOf(clr(R.color.swatch_blue), clr(R.color.swatch_purple))),
        ColorSpec("g_forest", str(R.string.forest), listOf(clr(R.color.swatch_green), clr(R.color.swatch_teal))),
        ColorSpec("g_berry", str(R.string.palette_g_berry_name), listOf(clr(R.color.palette_g_berry_start), clr(R.color.palette_g_berry_end))),
        ColorSpec("g_lagoon", str(R.string.palette_g_lagoon_name), listOf(clr(R.color.palette_g_lagoon_start), clr(R.color.palette_g_lagoon_end))),
        ColorSpec("g_mango", str(R.string.palette_g_mango_name), listOf(clr(R.color.palette_g_mango_start), clr(R.color.palette_g_mango_end))),
        ColorSpec("g_mint", str(R.string.palette_g_mint_name), listOf(clr(R.color.palette_g_mint_start), clr(R.color.palette_g_mint_end))),
        ColorSpec("g_dusk", str(R.string.palette_g_dusk_name), listOf(clr(R.color.palette_g_dusk_start), clr(R.color.palette_g_dusk_end))),
        ColorSpec("g_lime", str(R.string.palette_g_lime_name), listOf(clr(R.color.palette_g_lime_start), clr(R.color.palette_g_lime_end))),
        ColorSpec("g_flame", str(R.string.palette_g_flame_name), listOf(clr(R.color.palette_g_flame_start), clr(R.color.palette_g_flame_end))),
        ColorSpec("g_midnight", str(R.string.palette_g_midnight_name), listOf(clr(R.color.palette_g_midnight_start), clr(R.color.palette_g_midnight_end))),
        ColorSpec("g_peach", str(R.string.palette_g_peach_name), listOf(clr(R.color.palette_g_peach_start), clr(R.color.palette_g_peach_end))),
        ColorSpec("g_steel", str(R.string.palette_g_steel_name), listOf(clr(R.color.palette_g_steel_start), clr(R.color.palette_g_steel_end))),
        ColorSpec("g_royal", str(R.string.palette_g_royal_name), listOf(clr(R.color.palette_g_royal_start), clr(R.color.palette_g_royal_end))),
        ColorSpec("g_cherry", str(R.string.palette_g_cherry_name), listOf(clr(R.color.palette_g_cherry_start), clr(R.color.palette_g_cherry_end))),
    )

val AllColors = SolidColors + GradientColors

fun colorSpec(key: String): ColorSpec = AllColors.firstOrNull { it.key == key } ?: SolidColors[0]
