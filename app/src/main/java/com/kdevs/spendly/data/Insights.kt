package com.kdevs.spendly.data

import com.kdevs.spendly.data.model.Recurring
import com.kdevs.spendly.data.model.Txn
import java.time.LocalDate

/**
 * Plain calculations behind Insights, Reports, quick-add suggestions and subscriptions.
 * Nothing here touches the screen or Firebase, so it can be tested on its own.
 */
object Insights {
    // ---------- Month over month ----------

    data class CategoryChange(
        val categoryId: String,
        val thisMonth: Long,
        val lastMonth: Long,
    ) {
        val delta: Long get() = thisMonth - lastMonth

        /** Whole percent change against last month; null when last month had nothing to compare with. */
        val percent: Int? get() = if (lastMonth <= 0) null else ((thisMonth - lastMonth) * 100 / lastMonth).toInt()
    }

    private fun spentByCategory(
        txns: List<Txn>,
        fromMillis: Long,
        toMillis: Long,
    ): Map<String, Long> {
        val out = HashMap<String, Long>()
        for (t in txns) {
            if (t.kind != "expense" || t.dateMillis < fromMillis || t.dateMillis >= toMillis) continue
            out[t.categoryId] = (out[t.categoryId] ?: 0L) + t.amountPaise
        }
        return out
    }

    fun totalSpent(
        txns: List<Txn>,
        fromMillis: Long,
        toMillis: Long,
    ): Long = spentByCategory(txns, fromMillis, toMillis).values.sum()

    /**
     * Spending per category so far this month against the same days of last month, so the 10th of this month
     * is not compared with all of last month. Largest change first.
     */
    fun categoryChanges(
        txns: List<Txn>,
        today: LocalDate = LocalDate.now(),
    ): List<CategoryChange> {
        val thisStart = today.withDayOfMonth(1)
        val lastStart = thisStart.minusMonths(1)
        val cut = minOf(today.dayOfMonth, lastStart.lengthOfMonth())
        val lastEnd = lastStart.plusDays(cut.toLong())
        val now = spentByCategory(txns, thisStart.startMillis(), today.plusDays(1).startMillis())
        val before = spentByCategory(txns, lastStart.startMillis(), lastEnd.startMillis())
        return (now.keys + before.keys)
            .map { CategoryChange(it, now[it] ?: 0L, before[it] ?: 0L) }
            .sortedByDescending { kotlin.math.abs(it.delta) }
    }

    // ---------- Top notes ----------

    data class NoteTotal(
        val note: String,
        val count: Int,
        val paise: Long,
    )

    /** The notes you spent the most on in a period (a note is usually a shop or a person). Blank notes are skipped. */
    fun topNotes(
        txns: List<Txn>,
        fromMillis: Long,
        toMillis: Long,
        limit: Int = 8,
    ): List<NoteTotal> {
        val sums = HashMap<String, Long>()
        val counts = HashMap<String, Int>()
        val shown = HashMap<String, String>()
        for (t in txns) {
            if (t.kind != "expense" || t.dateMillis < fromMillis || t.dateMillis >= toMillis) continue
            val key = t.note.trim().lowercase()
            if (key.isEmpty()) continue
            sums[key] = (sums[key] ?: 0L) + t.amountPaise
            counts[key] = (counts[key] ?: 0) + 1
            shown.putIfAbsent(key, t.note.trim())
        }
        return sums.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { NoteTotal(shown.getValue(it.key), counts.getValue(it.key), it.value) }
    }

    // ---------- Unusual spend ----------

    /** An expense far above what you usually spend in its category. */
    data class Unusual(
        val categoryId: String,
        val amountPaise: Long,
        val usualPaise: Long,
    )

    private const val DAY = 24L * 60 * 60 * 1000

    /**
     * Is [t] at least three times the average of your earlier expenses in the same category over the last 90 days
     * (and at least Rs 500 above it)? Needs four earlier expenses to have a fair average.
     */
    fun unusual(
        history: List<Txn>,
        t: Txn,
        nowMillis: Long = System.currentTimeMillis(),
    ): Unusual? {
        if (t.kind != "expense" || t.categoryId.isBlank()) return null
        val earlier =
            history.filter {
                it.kind == "expense" && it.id != t.id && it.categoryId == t.categoryId &&
                    it.dateMillis >= nowMillis - 90 * DAY && it.dateMillis <= nowMillis
            }
        if (earlier.size < 4) return null
        val avg = earlier.sumOf { it.amountPaise } / earlier.size
        return if (t.amountPaise >= avg * 3 && t.amountPaise - avg >= 50_000) Unusual(t.categoryId, t.amountPaise, avg) else null
    }

    /** Expenses from the last [days] days that count as unusual, newest first. */
    fun recentUnusual(
        txns: List<Txn>,
        days: Int = 14,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<Pair<Txn, Unusual>> =
        txns
            .filter { it.kind == "expense" && it.dateMillis >= nowMillis - days * DAY && it.dateMillis <= nowMillis }
            .mapNotNull { t -> unusual(txns, t, nowMillis)?.let { t to it } }
            .sortedByDescending { it.first.dateMillis }

    // ---------- Quick-add suggestions ----------

    data class Suggestion(
        val note: String,
        val amountPaise: Long,
        val accountId: String,
        val categoryId: String,
        val subcategoryId: String,
        val times: Int,
    )

    /**
     * Notes you used before that contain what is being typed. The amount, account and category are the ones from the most
     * recent time that note was used. Most used first.
     */
    fun noteSuggestions(
        txns: List<Txn>,
        typed: String,
        kind: String,
        limit: Int = 3,
    ): List<Suggestion> {
        val q = typed.trim().lowercase()
        if (q.length < 2) return emptyList()
        val groups = HashMap<String, MutableList<Txn>>()
        for (t in txns) {
            if (t.kind != kind) continue
            val key = t.note.trim().lowercase()
            if (key.isEmpty() || !key.contains(q)) continue
            groups.getOrPut(key) { mutableListOf() }.add(t)
        }
        return groups.values
            .map { list ->
                val last = list.maxByOrNull { it.dateMillis }!!
                Suggestion(last.note.trim(), last.amountPaise, last.accountId, last.categoryId, last.subcategoryId, list.size)
            }.filter { !(it.note.equals(typed.trim(), ignoreCase = true) && it.times <= 1) }
            .sortedWith(compareByDescending<Suggestion> { it.times }.thenBy { it.note.length })
            .take(limit)
    }

    // ---------- Subscriptions ----------

    /** What an active recurring item costs in one month (weekly x 52/12, yearly / 12), counting its repeat interval. */
    fun monthlyCost(r: Recurring): Long {
        val n = r.interval.coerceAtLeast(1)
        return when (r.every) {
            "daily" -> r.amountPaise * 365 / 12 / n
            "weekly" -> r.amountPaise * 52 / 12 / n
            "yearly" -> r.amountPaise / 12 / n
            else -> r.amountPaise / n
        }
    }

    /** The exact cost over a year (not twelve rounded months). */
    fun yearlyCost(r: Recurring): Long {
        val n = r.interval.coerceAtLeast(1)
        return when (r.every) {
            "daily" -> r.amountPaise * 365 / n
            "weekly" -> r.amountPaise * 52 / n
            "yearly" -> r.amountPaise / n
            else -> r.amountPaise * 12 / n
        }
    }
}
