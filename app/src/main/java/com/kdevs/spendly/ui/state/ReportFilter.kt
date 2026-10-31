package com.kdevs.spendly.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Period and accounts chosen on the Report filter screen. period: month | last30 | year | custom */
object ReportFilter {
    var period by mutableStateOf("month")
    var customFrom by mutableStateOf(0L)
    var customTo by mutableStateOf(0L)
    var accountIds by mutableStateOf(setOf<String>())
    var kind by mutableStateOf("expense")
}
