package com.kdevs.spendly.data

import com.kdevs.spendly.data.model.SplitEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MoneyMathTest {
    // ---------- Showing and reading money ----------

    @Test
    fun formatsWithIndianGrouping() {
        assertEquals("₹0", formatPaise(0))
        assertEquals("₹999", formatPaise(99_900))
        assertEquals("₹1,250.50", formatPaise(125_050))
        assertEquals("₹1,23,456.78", formatPaise(12_345_678))
        assertEquals("-₹50", formatPaise(-5_000))
        assertEquals("+₹50", formatPaise(5_000, withSign = true))
    }

    @Test
    fun parsesRupeesIntoPaise() {
        assertEquals(125_050L, parseRupees("1,250.50"))
        assertEquals(100L, parseRupees("1"))
        assertEquals(1L, parseRupees("0.005")) // rounds half up
        assertNull(parseRupees(""))
        assertNull(parseRupees("abc"))
    }

    @Test
    fun fieldTextRoundTrips() {
        assertEquals("1250.5", paiseToField(125_050))
        assertEquals("", paiseToField(0))
    }

    // ---------- Split maths: the parts must always add up to the total ----------

    @Test
    fun equalSplitGivesLeftoverPaiseToTheFirstMembers() {
        val r = SplitMath.equal(10_000, listOf("a", "b", "c"))
        assertEquals(10_000L, r.values.sum())
        assertEquals(3_334L, r["a"])
        assertEquals(3_333L, r["b"])
        assertEquals(3_333L, r["c"])
    }

    @Test
    fun equalSplitOfNobodyIsEmpty() {
        assertEquals(emptyMap<String, Long>(), SplitMath.equal(10_000, emptyList()))
    }

    @Test
    fun percentSplitAddsUpToTheTotal() {
        val r = SplitMath.percent(10_001, linkedMapOf("a" to 3_333, "b" to 3_333, "c" to 3_334))
        assertEquals(10_001L, r.values.sum())
    }

    @Test
    fun sharesSplitAddsUpToTheTotal() {
        val r = SplitMath.shares(10_000, linkedMapOf("a" to 1, "b" to 2, "c" to 4))
        assertEquals(10_000L, r.values.sum())
        assertEquals(0L, SplitMath.shares(10_000, mapOf("a" to 0)).values.sum())
    }

    private fun expense(
        paidBy: String,
        total: Long,
        owed: Map<String, Long>,
    ) = SplitEntry(kind = "expense", totalPaise = total, paidBy = paidBy, owed = owed)

    @Test
    fun balancesShowWhoIsOwedAndWhoOwes() {
        val b = SplitMath.balances(listOf(expense("a", 30_000, mapOf("a" to 10_000, "b" to 10_000, "c" to 10_000))))
        assertEquals(20_000L, b["a"])
        assertEquals(-10_000L, b["b"])
        assertEquals(-10_000L, b["c"])
        assertEquals(0L, b.values.sum())
    }

    @Test
    fun settlementClearsADebt() {
        val entries =
            listOf(
                expense("a", 20_000, mapOf("a" to 10_000, "b" to 10_000)),
                SplitEntry(kind = "settlement", totalPaise = 10_000, paidBy = "b", settleTo = "a"),
            )
        val b = SplitMath.balances(entries)
        assertEquals(0L, b["a"])
        assertEquals(0L, b["b"])
        assertEquals(emptyList<SplitMath.Transfer>(), SplitMath.simplify(b))
    }

    @Test
    fun simplifyUsesFewPaymentsAndMovesExactlyWhatIsOwed() {
        val b = mapOf("a" to 20_000L, "b" to -10_000L, "c" to -10_000L)
        val plan = SplitMath.simplify(b)
        assertEquals(2, plan.size)
        assertEquals(20_000L, plan.filter { it.to == "a" }.sumOf { it.paise })
        // Applying the plan leaves everyone at zero.
        val after = b.toMutableMap()
        plan.forEach {
            after[it.from] = after.getValue(it.from) + it.paise
            after[it.to] = after.getValue(it.to) - it.paise
        }
        assertEquals(true, after.values.all { it == 0L })
    }

    // ---------- Repeating dates ----------

    @Test
    fun monthlyOnThe31stClampsThenReturns() {
        val feb = nextOccurrence(LocalDate.of(2026, 1, 31), "monthly", 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), feb)
        val mar = nextOccurrence(feb, "monthly", 1, 31)
        assertEquals(LocalDate.of(2026, 3, 31), mar)
    }

    @Test
    fun monthlyEveryTwoMonths() {
        assertEquals(LocalDate.of(2026, 3, 15), nextOccurrence(LocalDate.of(2026, 1, 15), "monthly", 2, 15))
    }

    @Test
    fun weeklyDailyAndYearly() {
        assertEquals(LocalDate.of(2026, 1, 15), nextOccurrence(LocalDate.of(2026, 1, 1), "weekly", 2, 1))
        assertEquals(LocalDate.of(2026, 1, 4), nextOccurrence(LocalDate.of(2026, 1, 1), "daily", 3, 1))
        assertEquals(LocalDate.of(2027, 3, 10), nextOccurrence(LocalDate.of(2026, 3, 10), "yearly", 1, 10))
    }

    @Test
    fun yearlyOnLeapDayLandsOnFeb28() {
        assertEquals(LocalDate.of(2025, 2, 28), nextOccurrence(LocalDate.of(2024, 2, 29), "yearly", 1, 29))
    }

    @Test
    fun zeroOrNegativeIntervalCountsAsOne() {
        assertEquals(LocalDate.of(2026, 1, 2), nextOccurrence(LocalDate.of(2026, 1, 1), "daily", 0, 1))
    }

    @Test
    fun addPeriodKeepsTheAnchorDay() {
        val jan31 = LocalDate.of(2026, 1, 31).startMillis()
        val feb = addPeriod(jan31, "monthly", anchorDay = 31)
        assertEquals(LocalDate.of(2026, 2, 28), toLocalDate(feb))
        assertEquals(LocalDate.of(2026, 3, 31), toLocalDate(addPeriod(feb, "monthly", anchorDay = 31)))
    }

    // ---------- CSV and dates for import ----------

    @Test
    fun csvHandlesQuotesCommasAndLineBreaks() {
        val rows =
            Backup.parseCsv(
                "\uFEFFDate,Amount,Note\r\n2026-01-05,100,\"Tea, biscuits\"\n2026-01-06,\"1,200\",\"He said \"\"hi\"\"\nsecond line\"\n",
            )
        assertEquals(3, rows.size)
        assertEquals(listOf("Date", "Amount", "Note"), rows[0])
        assertEquals("Tea, biscuits", rows[1][2])
        assertEquals("1,200", rows[2][1])
        assertEquals("He said \"hi\"\nsecond line", rows[2][2])
    }

    @Test
    fun csvSkipsBlankLines() {
        assertEquals(2, Backup.parseCsv("a,b\n\n1,2\n\n").size)
    }

    @Test
    fun readsTheCommonDateStyles() {
        val expected = LocalDate.of(2026, 3, 7).startMillis()
        assertEquals(expected, Backup.parseDate("2026-03-07"))
        assertEquals(expected, Backup.parseDate("07/03/2026"))
        assertEquals(expected, Backup.parseDate("7 Mar 2026"))
        assertNull(Backup.parseDate("not a date"))
        assertNull(Backup.parseDate(""))
    }
}
