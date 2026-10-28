package com.kdevs.spendly.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    )
val GradientColors =
    listOf(
        ColorSpec("g_sunset", str(R.string.sunset), listOf(clr(R.color.swatch_deep_orange), clr(R.color.swatch_pink))),
        ColorSpec("g_ocean", str(R.string.ocean), listOf(clr(R.color.swatch_sky), clr(R.color.swatch_teal))),
        ColorSpec("g_aurora", str(R.string.aurora), listOf(clr(R.color.swatch_blue), clr(R.color.swatch_purple))),
        ColorSpec("g_forest", str(R.string.forest), listOf(clr(R.color.swatch_green), clr(R.color.swatch_teal))),
    )
val AllColors = SolidColors + GradientColors

fun colorSpec(key: String): ColorSpec = AllColors.firstOrNull { it.key == key } ?: SolidColors[0]

/** Account types each have their own default colour and icon. */
data class AccountTypeInfo(val key: String, val label: String, val blurb: String, val colorKey: String, val icon: String)

val AccountTypes =
    listOf(
        AccountTypeInfo("bank", str(R.string.bank_account), str(R.string.savings_or_current_account), "blue", "bank"),
        AccountTypeInfo("wallet", str(R.string.wallet), str(R.string.upi_and_digital_wallets), "purple", "wallet"),
        AccountTypeInfo("cash", str(R.string.cash), str(R.string.money_in_hand), "green", "cash"),
        AccountTypeInfo("card", str(R.string.credit_card), str(R.string.cards_you_pay_off_later), "orange", "card"),
    )

class IconGroup(val title: String, val keys: List<String>)

val AccountIconGroups =
    listOf(
        IconGroup(str(R.string.banking), listOf("bank", "savings", "salary", "business")),
        IconGroup(str(R.string.wallets_and_payments), listOf("wallet", "upi", "phone", "paytm")),
        IconGroup(str(R.string.cash), listOf("cash", "coins", "safe")),
        IconGroup(str(R.string.cards), listOf("card", "debit", "gift")),
        IconGroup(str(R.string.other), listOf("invest", "home", "travel", "other")),
    )
val CategoryIconGroups =
    listOf(
        IconGroup(str(R.string.food), listOf("food", "coffee", "grocery", "drink")),
        IconGroup(str(R.string.travel), listOf("transport", "fuel", "flight", "hotel")),
        IconGroup(str(R.string.home), listOf("home", "bills", "rent", "wifi")),
        IconGroup(str(R.string.shopping), listOf("shopping", "gift", "clothes", "beauty")),
        IconGroup(str(R.string.life), listOf("health", "fitness", "education", "kids", "pets")),
        IconGroup(str(R.string.money), listOf("salary", "invest", "tax", "other", "movie", "goal")),
    )

fun iconFor(key: String): ImageVector =
    when (key) {
        "bank" -> Icons.Filled.AccountBalance
        "savings" -> Icons.Filled.Savings
        "salary" -> Icons.Filled.Work
        "business" -> Icons.Filled.Storefront
        "wallet" -> Icons.Filled.AccountBalanceWallet
        "upi" -> Icons.Filled.QrCode2
        "phone" -> Icons.Filled.PhoneAndroid
        "paytm" -> Icons.Filled.Contactless
        "cash" -> Icons.Filled.Payments
        "coins" -> Icons.Filled.MonetizationOn
        "safe" -> Icons.Filled.Lock
        "card" -> Icons.Filled.CreditCard
        "debit" -> Icons.Filled.Payment
        "gift" -> Icons.Filled.CardGiftcard
        "invest" -> Icons.Filled.TrendingUp
        "home" -> Icons.Filled.Home
        "travel" -> Icons.Filled.Luggage
        "food" -> Icons.Filled.Restaurant
        "coffee" -> Icons.Filled.LocalCafe
        "grocery" -> Icons.Filled.ShoppingBasket
        "drink" -> Icons.Filled.LocalBar
        "transport" -> Icons.Filled.DirectionsBus
        "fuel" -> Icons.Filled.LocalGasStation
        "flight" -> Icons.Filled.Flight
        "hotel" -> Icons.Filled.Hotel
        "bills" -> Icons.Filled.ReceiptLong
        "rent" -> Icons.Filled.Apartment
        "wifi" -> Icons.Filled.Wifi
        "shopping" -> Icons.Filled.ShoppingBag
        "clothes" -> Icons.Filled.Checkroom
        "beauty" -> Icons.Filled.Spa
        "health" -> Icons.Filled.MedicalServices
        "fitness" -> Icons.Filled.FitnessCenter
        "education" -> Icons.Filled.School
        "kids" -> Icons.Filled.ChildCare
        "pets" -> Icons.Filled.Pets
        "tax" -> Icons.Filled.AccountBalance
        "movie" -> Icons.Filled.Movie
        "goal" -> Icons.Filled.Flag
        else -> Icons.Filled.Category
    }

@Composable
fun IconCircle(
    icon: String,
    colorKey: String,
    size: Dp = 44.dp,
    tint: Color = Color.White,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(colorSpec(colorKey).brush),
        contentAlignment = Alignment.Center,
    ) { Icon(iconFor(icon), null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}
