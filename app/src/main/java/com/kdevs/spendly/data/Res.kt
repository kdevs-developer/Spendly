package com.kdevs.spendly.data

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

/**
 * One way to read res/values from anywhere: composables, lambdas, the repository and top-level lists.
 * SpendlyApp calls [init] before anything else, so every value is ready by the time a screen draws.
 * Words live in strings.xml and colours in colors.xml, so changing either never touches Kotlin.
 */
object Res {
    private lateinit var ctx: Context

    fun init(c: Context) {
        ctx = c.applicationContext
    }

    fun string(
        @StringRes id: Int,
        vararg args: Any?,
    ): String = if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)

    fun colorInt(
        @ColorRes id: Int,
    ): Int = ContextCompat.getColor(ctx, id)

    fun integer(
        @IntegerRes id: Int,
    ): Int = ctx.resources.getInteger(id)
}

/** Text from strings.xml. Pass values for %1$s style placeholders. */
fun str(
    @StringRes id: Int,
    vararg args: Any?,
): String = Res.string(id, *args)

/** Colour from colors.xml. */
fun clr(
    @ColorRes id: Int,
): Color = Color(Res.colorInt(id))
