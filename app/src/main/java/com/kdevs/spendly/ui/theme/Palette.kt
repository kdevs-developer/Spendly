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
        IconGroup(str(R.string.banking), listOf("bank", "savings", "salary", "business", "office", "credit", "verified", "insurance")),
        IconGroup(str(R.string.wallets_and_payments), listOf("wallet", "upi", "phone", "paytm", "nfc", "atm", "exchange", "quote")),
        IconGroup(str(R.string.cash), listOf("cash", "coins", "safe", "money", "cashplus", "paid")),
        IconGroup(str(R.string.cards), listOf("card", "debit", "gift", "bonus", "offer")),
        IconGroup("Investments", listOf("invest", "chart", "pie", "bar", "interest", "jewel", "star", "trophy")),
        IconGroup(str(R.string.other), listOf("home", "travel", "car", "goal", "heart", "other")),
    )
val CategoryIconGroups =
    listOf(
        IconGroup(
            "Food & drink",
            listOf(
                "food", "coffee", "grocery", "drink", "fastfood", "pizza", "cake", "icecream", "bakery", "ramen",
                "lunch", "dinner", "breakfast", "tea", "liquor", "wine", "juice", "seafood", "kitchen",
            ),
        ),
        IconGroup(
            str(R.string.travel),
            listOf(
                "transport", "fuel", "flight", "hotel", "car", "bike", "scooter", "train", "metro", "taxi",
                "boat", "ev", "parking", "beach", "trip", "delivery", "repair",
            ),
        ),
        IconGroup(
            "Home & bills",
            listOf(
                "home", "bills", "rent", "wifi", "power", "water", "bulb", "sofa", "bed", "plumbing", "repairs",
                "cleaning", "laundry", "tv", "router", "build", "garden", "cottage", "security", "key", "fire",
            ),
        ),
        IconGroup(
            str(R.string.shopping),
            listOf(
                "shopping", "gift", "clothes", "beauty", "cart", "mall", "store", "jewel", "watch", "haircut",
                "face", "sale", "offer", "laptop", "headphones", "camera", "toys", "style",
            ),
        ),
        IconGroup(
            "Health & sport",
            listOf(
                "health", "fitness", "hospital", "medicine", "vaccine", "heart", "mind", "yoga", "soccer",
                "cricket", "basketball", "tennis", "swim", "run",
            ),
        ),
        IconGroup("Family & learning", listOf("education", "kids", "pets", "book", "library", "science", "code", "baby")),
        IconGroup(
            "Fun & lifestyle",
            listOf("movie", "music", "theater", "party", "casino", "park", "ticket", "mic", "livetv", "subs", "podcast", "games", "palette"),
        ),
        IconGroup(
            "Money & work",
            listOf(
                "salary", "work", "office", "invest", "tax", "goal", "bonus", "trophy", "interest", "chart", "pie",
                "receipt", "insurance", "charity", "exchange", "atm", "money", "cashplus", "factory", "farm", "engineer", "other",
            ),
        ),
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
        // food
        "fastfood" -> Icons.Filled.Fastfood
        "pizza" -> Icons.Filled.LocalPizza
        "cake" -> Icons.Filled.Cake
        "icecream" -> Icons.Filled.Icecream
        "bakery" -> Icons.Filled.BakeryDining
        "ramen" -> Icons.Filled.RamenDining
        "lunch" -> Icons.Filled.LunchDining
        "dinner" -> Icons.Filled.DinnerDining
        "breakfast" -> Icons.Filled.BreakfastDining
        "tea" -> Icons.Filled.EmojiFoodBeverage
        "liquor" -> Icons.Filled.Liquor
        "wine" -> Icons.Filled.WineBar
        "juice" -> Icons.Filled.LocalDrink
        "seafood" -> Icons.Filled.SetMeal
        "kitchen" -> Icons.Filled.Kitchen
        // travel
        "car" -> Icons.Filled.DirectionsCar
        "bike" -> Icons.Filled.PedalBike
        "scooter" -> Icons.Filled.TwoWheeler
        "train" -> Icons.Filled.Train
        "metro" -> Icons.Filled.Subway
        "taxi" -> Icons.Filled.LocalTaxi
        "boat" -> Icons.Filled.DirectionsBoat
        "ev" -> Icons.Filled.EvStation
        "parking" -> Icons.Filled.LocalParking
        "beach" -> Icons.Filled.BeachAccess
        "trip" -> Icons.Filled.Hiking
        "delivery" -> Icons.Filled.LocalShipping
        "repair" -> Icons.Filled.CarRepair
        // home and bills
        "power" -> Icons.Filled.Bolt
        "water" -> Icons.Filled.WaterDrop
        "bulb" -> Icons.Filled.Lightbulb
        "sofa" -> Icons.Filled.Weekend
        "bed" -> Icons.Filled.Bed
        "plumbing" -> Icons.Filled.Plumbing
        "repairs" -> Icons.Filled.Handyman
        "cleaning" -> Icons.Filled.CleaningServices
        "laundry" -> Icons.Filled.LocalLaundryService
        "tv" -> Icons.Filled.Tv
        "router" -> Icons.Filled.Router
        "build" -> Icons.Filled.Construction
        "garden" -> Icons.Filled.Yard
        "cottage" -> Icons.Filled.Cottage
        "security" -> Icons.Filled.Security
        "key" -> Icons.Filled.Key
        "fire" -> Icons.Filled.LocalFireDepartment
        // shopping
        "cart" -> Icons.Filled.ShoppingCart
        "mall" -> Icons.Filled.LocalMall
        "store" -> Icons.Filled.Store
        "jewel" -> Icons.Filled.Diamond
        "watch" -> Icons.Filled.Watch
        "haircut" -> Icons.Filled.ContentCut
        "face" -> Icons.Filled.Face
        "sale" -> Icons.Filled.Sell
        "offer" -> Icons.Filled.LocalOffer
        "laptop" -> Icons.Filled.Laptop
        "headphones" -> Icons.Filled.Headphones
        "camera" -> Icons.Filled.PhotoCamera
        "toys" -> Icons.Filled.Toys
        "style" -> Icons.Filled.Style
        // health and sport
        "hospital" -> Icons.Filled.LocalHospital
        "medicine" -> Icons.Filled.Medication
        "vaccine" -> Icons.Filled.Vaccines
        "heart" -> Icons.Filled.Favorite
        "mind" -> Icons.Filled.Psychology
        "yoga" -> Icons.Filled.SelfImprovement
        "soccer" -> Icons.Filled.SportsSoccer
        "cricket" -> Icons.Filled.SportsCricket
        "basketball" -> Icons.Filled.SportsBasketball
        "tennis" -> Icons.Filled.SportsTennis
        "swim" -> Icons.Filled.Pool
        "run" -> Icons.Filled.DirectionsRun
        // family and learning
        "book" -> Icons.Filled.MenuBook
        "library" -> Icons.Filled.LocalLibrary
        "science" -> Icons.Filled.Science
        "code" -> Icons.Filled.Code
        "baby" -> Icons.Filled.ChildFriendly
        // fun
        "music" -> Icons.Filled.MusicNote
        "theater" -> Icons.Filled.TheaterComedy
        "party" -> Icons.Filled.Celebration
        "casino" -> Icons.Filled.Casino
        "park" -> Icons.Filled.Attractions
        "ticket" -> Icons.Filled.ConfirmationNumber
        "mic" -> Icons.Filled.Mic
        "livetv" -> Icons.Filled.LiveTv
        "subs" -> Icons.Filled.Subscriptions
        "podcast" -> Icons.Filled.Podcasts
        "games" -> Icons.Filled.SportsEsports
        "palette" -> Icons.Filled.Palette
        // money and work
        "work" -> Icons.Filled.BusinessCenter
        "office" -> Icons.Filled.Business
        "bonus" -> Icons.Filled.Redeem
        "trophy" -> Icons.Filled.EmojiEvents
        "interest" -> Icons.Filled.Percent
        "chart" -> Icons.Filled.ShowChart
        "pie" -> Icons.Filled.PieChart
        "bar" -> Icons.Filled.BarChart
        "receipt" -> Icons.Filled.Receipt
        "insurance" -> Icons.Filled.Shield
        "charity" -> Icons.Filled.VolunteerActivism
        "exchange" -> Icons.Filled.CurrencyExchange
        "atm" -> Icons.Filled.LocalAtm
        "money" -> Icons.Filled.Money
        "cashplus" -> Icons.Filled.AttachMoney
        "paid" -> Icons.Filled.Paid
        "quote" -> Icons.Filled.RequestQuote
        "credit" -> Icons.Filled.CreditScore
        "verified" -> Icons.Filled.VerifiedUser
        "nfc" -> Icons.Filled.Nfc
        "star" -> Icons.Filled.Star
        "factory" -> Icons.Filled.Factory
        "farm" -> Icons.Filled.Agriculture
        "engineer" -> Icons.Filled.Engineering
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