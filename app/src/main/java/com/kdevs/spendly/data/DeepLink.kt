package com.kdevs.spendly.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A screen another part of the system asks the app to open: the home screen widget ("add expense")
 * and the bill reminder notification. MainActivity reads the extra, the navigation host opens the route.
 */
object DeepLink {
    const val EXTRA = "spendlyRoute"

    val route = MutableStateFlow<String?>(null)
}
