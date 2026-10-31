package com.kdevs.spendly.data

import java.time.LocalDate
import kotlin.math.min

/**
 * Next occurrence after [from], stepping by [interval] units of [every].
 * Call this wherever you currently advance Recurring.nextMillis by one unit,
 * then convert the result back to millis with your existing helper.
 *
 * Monthly keeps the original day (anchorDay), clamped to the month length,
 * so a rule anchored on the 31st lands on Feb 28/29, then Mar 31 again.
 */
fun nextOccurrence(
    from: LocalDate,
    every: String,
    interval: Int,
    anchorDay: Int,
): LocalDate {
    val n = interval.coerceAtLeast(1).toLong()
    return when (every) {
        "daily" -> from.plusDays(n)
        "weekly" -> from.plusWeeks(n)
        "yearly" -> from.plusYears(n)
        else -> {
            val m = from.withDayOfMonth(1).plusMonths(n)
            m.withDayOfMonth(min(anchorDay, m.lengthOfMonth()))
        }
    }
}
