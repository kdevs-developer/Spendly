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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
