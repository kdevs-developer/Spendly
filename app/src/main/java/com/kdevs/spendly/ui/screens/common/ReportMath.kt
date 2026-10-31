package com.kdevs.spendly.ui.screens.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.kdevs.spendly.R
import com.kdevs.spendly.data.*
import com.kdevs.spendly.data.model.*
import com.kdevs.spendly.data.str
import com.kdevs.spendly.ui.state.ReportFilter
import com.kdevs.spendly.ui.theme.*
import java.time.LocalDate

fun spent(list: List<Txn>) = list.filter { it.kind == "expense" }.sumOf { it.amountPaise }

fun earned(list: List<Txn>) = list.filter { it.kind == "income" }.sumOf { it.amountPaise }

fun inRange(
    list: List<Txn>,
    from: Long,
    to: Long,
) = list.filter { it.dateMillis in from until to }

/** [from, to) in millis for the period chosen on the Report filter screen. */
fun reportRange(): Pair<Long, Long> {
    val today = LocalDate.now()
    return when (ReportFilter.period) {
        "last30" -> today.minusDays(29).startMillis() to today.plusDays(1).startMillis()
        "year" -> today.withDayOfYear(1).startMillis() to today.withDayOfYear(1).plusYears(1).startMillis()
        "custom" -> ReportFilter.customFrom to (ReportFilter.customTo + 86_400_000L)
        else -> monthStart() to nextMonthStart()
    }
}

fun periodLabel(): String =
    when (ReportFilter.period) {
        "last30" -> str(R.string.last_30_days)
        "year" -> str(R.string.this_year)
        "custom" -> str(R.string.to, fullDate(ReportFilter.customFrom), fullDate(ReportFilter.customTo))
        else -> str(R.string.this_month)
    }
