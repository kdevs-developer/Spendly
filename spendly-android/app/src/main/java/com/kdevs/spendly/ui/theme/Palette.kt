package com.kdevs.spendly.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

/** Solid colours and gradients. A colour is stored as a key string; "g_" keys are gradients. */
data class ColorSpec(val key: String, val label: String, val colors: List<Color>) {
    val isGradient get() = colors.size > 1
    val solid get() = colors.first()
    val brush: Brush get() = if (isGradient) Brush.linearGradient(colors) else Brush.linearGradient(listOf(colors[0], colors[0]))
}

val SolidColors = listOf(
    ColorSpec("blue", "Blue", listOf(Color(0xFF1D4ED8))),
    ColorSpec("purple", "Purple", listOf(Color(0xFF7E22CE))),
    ColorSpec("green", "Green", listOf(Color(0xFF15803D))),
    ColorSpec("orange", "Orange", listOf(Color(0xFFC2410C))),
    ColorSpec("pink", "Pink", listOf(Color(0xFFBE185D))),
    ColorSpec("teal", "Teal", listOf(Color(0xFF0F766E))),
    ColorSpec("red", "Red", listOf(Color(0xFFC62828))),
    ColorSpec("slate", "Slate", listOf(Color(0xFF475569))),
)
val GradientColors = listOf(
    ColorSpec("g_sunset", "Sunset", listOf(Color(0xFFEA580C), Color(0xFFBE185D))),
    ColorSpec("g_ocean", "Ocean", listOf(Color(0xFF0369A1), Color(0xFF0F766E))),
    ColorSpec("g_aurora", "Aurora", listOf(Color(0xFF1D4ED8), Color(0xFF7E22CE))),
    ColorSpec("g_forest", "Forest", listOf(Color(0xFF15803D), Color(0xFF0F766E))),
)
val AllColors = SolidColors + GradientColors
fun colorSpec(key: String): ColorSpec = AllColors.firstOrNull { it.key == key } ?: SolidColors[0]

/** Account types each have their own default colour and icon. */
data class AccountTypeInfo(val key: String, val label: String, val blurb: String, val colorKey: String, val icon: String)
val AccountTypes = listOf(
    AccountTypeInfo("bank", "Bank account", "Savings or current account", "blue", "bank"),
    AccountTypeInfo("wallet", "Wallet", "UPI and digital wallets", "purple", "wallet"),
    AccountTypeInfo("cash", "Cash", "Money in hand", "green", "cash"),
    AccountTypeInfo("card", "Credit card", "Cards you pay off later", "orange", "card"),
)

class IconGroup(val title: String, val keys: List<String>)

val AccountIconGroups = listOf(
    IconGroup("Banking", listOf("bank", "savings", "salary", "business")),
    IconGroup("Wallets and payments", listOf("wallet", "upi", "phone", "paytm")),
    IconGroup("Cash", listOf("cash", "coins", "safe")),
    IconGroup("Cards", listOf("card", "debit", "gift")),
    IconGroup("Other", listOf("invest", "home", "travel", "other")),
)
val CategoryIconGroups = listOf(
    IconGroup("Food", listOf("food", "coffee", "grocery", "drink")),
    IconGroup("Travel", listOf("transport", "fuel", "flight", "hotel")),
    IconGroup("Home", listOf("home", "bills", "rent", "wifi")),
    IconGroup("Shopping", listOf("shopping", "gift", "clothes", "beauty")),
    IconGroup("Life", listOf("health", "fitness", "education", "kids", "pets")),
    IconGroup("Money", listOf("salary", "invest", "tax", "other", "movie", "goal")),
)

fun iconFor(key: String): ImageVector = when (key) {
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
fun IconCircle(icon: String, colorKey: String, size: Dp = 44.dp, tint: Color = Color.White) {
    Box(
        Modifier.size(size).clip(CircleShape).background(colorSpec(colorKey).brush),
        contentAlignment = Alignment.Center,
    ) { Icon(iconFor(icon), null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}
