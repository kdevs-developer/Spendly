package com.kdevs.spendly.data

import com.kdevs.spendly.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val zone: ZoneId get() = ZoneId.systemDefault()

fun formatPaise(
    paise: Long,
    withSign: Boolean = false,
): String {
    val neg = paise < 0
    val a = abs(paise)
    val s = (a / 100).toString()
    val grouped =
        if (s.length <= 3) {
            s
        } else {
            val head = s.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$head,${s.takeLast(3)}"
        }
    val frac = if (a % 100 == 0L) "" else "." + (a % 100).toString().padStart(2, '0')
    val sign =
        when {
            neg -> "-"
            withSign && paise > 0 -> "+"
            else -> ""
        }
    return "$sign₹$grouped$frac"
}

/** "1,250.50" -> 125050. Returns null for blank or invalid input. */
fun parseRupees(text: String): Long? {
    val t = text.replace(",", "").trim()
    if (t.isEmpty()) return null
    return runCatching {
        BigDecimal(t).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }.getOrNull()
}

fun paiseToField(paise: Long): String = if (paise == 0L) "" else BigDecimal(paise).movePointLeft(2).stripTrailingZeros().toPlainString()

fun toLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

fun LocalDate.startMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()

fun todayMillis(): Long = LocalDate.now().startMillis()

/** Pickers return midnight of the chosen day; this puts the current clock time on it. */
fun withNowTime(dayMillis: Long): Long =
    toLocalDate(dayMillis).atTime(java.time.LocalTime.now()).atZone(zone).toInstant().toEpochMilli()

/** Keeps the saved time if the day didn't change, otherwise stamps the current time on the new day. */
fun stampTime(
    dayMillis: Long,
    existingMillis: Long?,
): Long =
    if (existingMillis != null && toLocalDate(existingMillis) == toLocalDate(dayMillis)) existingMillis else withNowTime(dayMillis)

/** False for older transactions that were saved at midnight with no time. */
fun hasTime(millis: Long): Boolean = millis != toLocalDate(millis).startMillis()

/** Right now, as a full moment (day and clock time). */
fun nowMillis(): Long = withNowTime(todayMillis())

/** Moves a moment to another day and keeps its clock time. A midnight source stays at midnight (no time). */
fun withTimeOf(
    dayMillis: Long,
    timeSource: Long,
): Long =
    toLocalDate(dayMillis).atTime(Instant.ofEpochMilli(timeSource).atZone(zone).toLocalTime()).atZone(zone).toInstant().toEpochMilli()

/** Minutes since midnight (0 to 1439) of a moment. */
fun minutesOfDay(millis: Long): Int = Instant.ofEpochMilli(millis).atZone(zone).let { it.hour * 60 + it.minute }

/** The given day at a chosen clock time, in minutes since midnight. */
fun withMinutes(
    dayMillis: Long,
    minutes: Int,
): Long = toLocalDate(dayMillis).atTime(minutes / 60, minutes % 60).atZone(zone).toInstant().toEpochMilli()

/** Recurring entries are saved at midnight; give them a steady 9:00 AM so they show a time. */
fun atNineIfNoTime(millis: Long): Long = if (hasTime(millis)) millis else withMinutes(millis, 9 * 60)

fun monthStart(d: LocalDate = LocalDate.now()): Long = d.withDayOfMonth(1).startMillis()

fun nextMonthStart(d: LocalDate = LocalDate.now()): Long = d.withDayOfMonth(1).plusMonths(1).startMillis()

fun dayLabel(millis: Long): String {
    val d = toLocalDate(millis)
    val today = LocalDate.now()
    return when (d) {
        today -> str(R.string.today)
        today.minusDays(1) -> str(R.string.yesterday)
        else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
}

fun fullDate(millis: Long): String = toLocalDate(millis).format(DateTimeFormatter.ofPattern("d MMM yyyy"))

fun timeOf(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("h:mm a"))

fun addPeriod(
    millis: Long,
    every: String,
): Long {
    val d = toLocalDate(millis)
    return when (every) {
        "daily" -> d.plusDays(1)
        "weekly" -> d.plusWeeks(1)
        "yearly" -> d.plusYears(1)
        else -> d.plusMonths(1)
    }.startMillis()
}

// ---------- Split maths. Everything in whole paise; the parts always add up to the total. ----------

object SplitMath {
    /** Equal split. The leftover paise go one each to the first members. */
    fun equal(
        total: Long,
        ids: List<String>,
    ): Map<String, Long> {
        if (ids.isEmpty()) return emptyMap()
        val base = total / ids.size
        var extra = (total % ids.size).toInt()
        return ids.associateWith { base + if (extra-- > 0) 1 else 0 }
    }

    /** Percent is held in basis points (100% = 10000). Rounds down, rest goes to the first member. */
    fun percent(
        total: Long,
        bps: Map<String, Int>,
    ): Map<String, Long> {
        val out = bps.mapValues { (_, b) -> total * b / 10000 }.toMutableMap()
        val diff = total - out.values.sum()
        bps.keys.firstOrNull()?.let { out[it] = out.getValue(it) + diff }
        return out
    }

    fun shares(
        total: Long,
        weights: Map<String, Int>,
    ): Map<String, Long> {
        val sum = weights.values.sum()
        if (sum <= 0) return weights.mapValues { 0L }
        val out = weights.mapValues { (_, w) -> total * w / sum }.toMutableMap()
        val diff = total - out.values.sum()
        weights.keys.firstOrNull()?.let { out[it] = out.getValue(it) + diff }
        return out
    }

    /** Net balance per person. Positive = is owed money, negative = owes money. */
    fun balances(entries: List<com.kdevs.spendly.data.model.SplitEntry>): Map<String, Long> {
        val b = mutableMapOf<String, Long>()
        for (e in entries) {
            if (e.kind == "settlement") {
                b[e.paidBy] = (b[e.paidBy] ?: 0) + e.totalPaise
                b[e.settleTo] = (b[e.settleTo] ?: 0) - e.totalPaise
            } else {
                b[e.paidBy] = (b[e.paidBy] ?: 0) + e.totalPaise
                e.owed.forEach { (uid, p) -> b[uid] = (b[uid] ?: 0) - p }
            }
        }
        return b
    }

    data class Transfer(val from: String, val to: String, val paise: Long)

    /** Fewest payments that clear every balance. */
    fun simplify(balances: Map<String, Long>): List<Transfer> {
        val debtors = balances.filter { it.value < 0 }.map { it.key to -it.value }.toMutableList()
        val creditors = balances.filter { it.value > 0 }.map { it.key to it.value }.toMutableList()
        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }
        val out = mutableListOf<Transfer>()
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val pay = minOf(debtors[i].second, creditors[j].second)
            if (pay > 0) out += Transfer(debtors[i].first, creditors[j].first, pay)
            debtors[i] = debtors[i].first to debtors[i].second - pay
            creditors[j] = creditors[j].first to creditors[j].second - pay
            if (debtors[i].second == 0L) i++
            if (j < creditors.size && creditors[j].second == 0L) j++
        }
        return out
    }
}