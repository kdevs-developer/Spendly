package com.kdevs.spendly.data

import com.kdevs.spendly.data.model.Recurring
import com.kdevs.spendly.data.model.Txn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class InsightsTest {
    private fun day(
        y: Int,
        m: Int,
        d: Int,
    ) = LocalDate.of(y, m, d).startMillis()

    private fun exp(
        id: String,
        paise: Long,
        date: Long,
        cat: String = "food",
        note: String = "",
        kind: String = "expense",
    ) = Txn(id = id, kind = kind, amountPaise = paise, categoryId = cat, dateMillis = date, note = note, accountId = "acc")

    // ---------- Month over month ----------

    @Test
    fun comparesThisMonthWithTheSameDaysOfLastMonth() {
        val today = LocalDate.of(2026, 11, 10)
        val txns =
            listOf(
                exp("1", 30_000, day(2026, 11, 3)), // this month
                exp("2", 10_000, day(2026, 10, 4)), // last month, inside the same days
                exp("3", 99_000, day(2026, 10, 20)), // last month, after day 10: must not count
                exp("4", 5_000, day(2026, 11, 5), cat = "fuel"),
                exp("5", 7_000, day(2026, 11, 6), kind = "income"), // income never counts
            )
        val changes = Insights.categoryChanges(txns, today)
        val food = changes.first { it.categoryId == "food" }
        assertEquals(30_000L, food.thisMonth)
        assertEquals(10_000L, food.lastMonth)
        assertEquals(200, food.percent)
        val fuel = changes.first { it.categoryId == "fuel" }
        assertEquals(0L, fuel.lastMonth)
        assertNull(fuel.percent)
        assertEquals(food.categoryId, changes.first().categoryId) // biggest change first
    }

    @Test
    fun totalSpentCountsOnlyExpensesInTheRange() {
        val txns =
            listOf(exp("1", 100, day(2026, 11, 1)), exp("2", 200, day(2026, 11, 2)), exp("3", 400, day(2026, 11, 2), kind = "income"))
        assertEquals(300L, Insights.totalSpent(txns, day(2026, 11, 1), day(2026, 11, 3)))
        assertEquals(100L, Insights.totalSpent(txns, day(2026, 11, 1), day(2026, 11, 2)))
    }

    // ---------- Top notes ----------

    @Test
    fun topNotesGroupsByNoteIgnoringCaseAndBlanks() {
        val txns =
            listOf(
                exp("1", 10_000, day(2026, 11, 1), note = "Swiggy"),
                exp("2", 15_000, day(2026, 11, 2), note = "swiggy "),
                exp("3", 50_000, day(2026, 11, 2), note = "Rent"),
                exp("4", 99_900, day(2026, 11, 2), note = ""),
            )
        val top = Insights.topNotes(txns, day(2026, 11, 1), day(2026, 11, 30))
        assertEquals(2, top.size)
        assertEquals("Rent", top[0].note)
        assertEquals(25_000L, top[1].paise)
        assertEquals(2, top[1].count)
    }

    // ---------- Unusual spend ----------

    private val now = day(2026, 11, 10) + 12 * 60 * 60 * 1000L

    private fun history(n: Int = 4) = (1..n).map { exp("h$it", 20_000, now - it * 86_400_000L) }

    @Test
    fun flagsAnExpenseThatIsFarAboveTheCategoryAverage() {
        val big = exp("new", 150_000, now)
        val u = Insights.unusual(history(), big, now)
        assertNotNull(u)
        assertEquals(20_000L, u!!.usualPaise)
    }

    @Test
    fun doesNotFlagANormalExpense() {
        assertNull(Insights.unusual(history(), exp("new", 50_000, now), now))
    }

    @Test
    fun needsFourEarlierExpensesBeforeJudging() {
        assertNull(Insights.unusual(history(3), exp("new", 150_000, now), now))
    }

    @Test
    fun ignoresOtherCategoriesIncomeAndOldHistory() {
        val mixed =
            history(3) + exp("old", 20_000, now - 200 * 86_400_000L) + exp("other", 20_000, now - 86_400_000L, cat = "fuel")
        assertNull(Insights.unusual(mixed, exp("new", 150_000, now), now))
    }

    @Test
    fun aSmallAbsoluteJumpIsNotUnusual() {
        // 3x of a tiny average is still under Rs 500 above it.
        val tiny = (1..4).map { exp("t$it", 1_000, now - it * 86_400_000L) }
        assertNull(Insights.unusual(tiny, exp("new", 5_000, now), now))
    }

    @Test
    fun recentUnusualListsOnlyRecentFlaggedExpenses() {
        val big = exp("big", 150_000, now - 86_400_000L)
        val list = Insights.recentUnusual(history() + big, 14, now)
        assertEquals(1, list.size)
        assertEquals("big", list[0].first.id)
    }

    // ---------- Quick-add suggestions ----------

    @Test
    fun suggestsTheUsualDetailsForANoteYouUsedBefore() {
        val txns =
            listOf(
                exp("1", 2_000, day(2026, 10, 1), note = "Tea"),
                exp("2", 2_000, day(2026, 10, 2), note = "Tea"),
                exp("3", 2_500, day(2026, 10, 3), note = "tea", cat = "snacks"), // most recent
                exp("4", 9_000, day(2026, 10, 3), note = "Tea stall"),
                exp("5", 7_000, day(2026, 10, 4), note = "Tea", kind = "income"),
            )
        val s = Insights.noteSuggestions(txns, "te", "expense")
        assertEquals(2, s.size)
        assertEquals("tea", s[0].note) // the most used note first, shown as it was last written
        assertEquals(2_500L, s[0].amountPaise)
        assertEquals("snacks", s[0].categoryId)
        assertEquals(3, s[0].times)
        assertEquals("Tea stall", s[1].note)
    }

    @Test
    fun noSuggestionsForShortTextOrAnUnusedNote() {
        val txns = listOf(exp("1", 2_000, day(2026, 10, 1), note = "Tea"))
        assertEquals(emptyList<Insights.Suggestion>(), Insights.noteSuggestions(txns, "t", "expense"))
        assertEquals(emptyList<Insights.Suggestion>(), Insights.noteSuggestions(txns, "coffee", "expense"))
    }

    @Test
    fun doesNotSuggestWhatIsAlreadyTypedInFullForASingleUse() {
        val txns = listOf(exp("1", 2_000, day(2026, 10, 1), note = "Tea"))
        assertEquals(emptyList<Insights.Suggestion>(), Insights.noteSuggestions(txns, "tea", "expense"))
    }

    // ---------- Subscriptions ----------

    private fun rec(
        every: String,
        paise: Long,
        interval: Int = 1,
    ) = Recurring(every = every, amountPaise = paise, interval = interval)

    @Test
    fun monthlyCostAveragesWeeklyAndYearlyItems() {
        assertEquals(1_000L, Insights.monthlyCost(rec("monthly", 1_000)))
        assertEquals(5_200L, Insights.monthlyCost(rec("weekly", 1_200)))
        assertEquals(1_000L, Insights.monthlyCost(rec("yearly", 12_000)))
        assertEquals(500L, Insights.monthlyCost(rec("monthly", 1_000, interval = 2)))
    }

    @Test
    fun yearlyCostIsExact() {
        assertEquals(62_400L, Insights.yearlyCost(rec("weekly", 1_200)))
        assertEquals(12_000L, Insights.yearlyCost(rec("monthly", 1_000)))
        assertEquals(12_000L, Insights.yearlyCost(rec("yearly", 12_000)))
        assertEquals(6_000L, Insights.yearlyCost(rec("monthly", 1_000, interval = 2)))
    }
}
