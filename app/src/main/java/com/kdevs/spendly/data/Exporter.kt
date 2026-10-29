package com.kdevs.spendly.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.kdevs.spendly.data.model.Txn
import com.kdevs.spendly.ui.screens.Lookup
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds the files for Export. No extra libraries: the Excel file (.xlsx) is written as the plain XML it
 * is made of, and the PDF comes from Android's own PdfDocument.
 */
object Exporter {
    private class Row(
        val dateMillis: Long,
        val type: String,
        val category: String,
        val sub: String,
        val account: String,
        val toAccount: String,
        val paise: Long,
        val note: String,
    )

    private fun rows(
        list: List<Txn>,
        look: Lookup,
    ): List<Row> =
        list.sortedByDescending { it.dateMillis }.map { t ->
            val transfer = t.kind == "transfer"
            Row(
                t.dateMillis,
                t.kind.replaceFirstChar { it.uppercase() },
                if (transfer) "Transfer" else if (t.categoryId.isBlank()) "Uncategorised" else look.categoryName(t.categoryId),
                if (t.subcategoryId.isBlank()) "" else look.categoryName(t.subcategoryId),
                look.accountName(t.accountId),
                if (transfer) look.accountName(t.toAccountId) else "",
                t.amountPaise,
                t.note,
            )
        }

    private fun rupees(paise: Long) = paise / 100.0

    // ------------------------------------------------------------------ Excel

    private fun esc(s: String): String =
        buildString {
            for (ch in s) {
                when {
                    ch == '&' -> append("&amp;")
                    ch == '<' -> append("&lt;")
                    ch == '>' -> append("&gt;")
                    ch == '"' -> append("&quot;")
                    ch.code < 32 && ch != '\n' && ch != '\t' -> Unit // control characters are not allowed in XML
                    else -> append(ch)
                }
            }
        }

    private fun colName(i: Int) = ('A' + i).toString()

    private fun text(
        ref: String,
        v: String,
        style: Int = 0,
    ) = """<c r="$ref" t="inlineStr" s="$style"><is><t xml:space="preserve">${esc(v)}</t></is></c>"""

    private fun num(
        ref: String,
        v: Double,
        style: Int,
    ) = """<c r="$ref" s="$style"><v>$v</v></c>"""

    private fun sheet(
        widths: List<Int>,
        freezeHeader: Boolean,
        body: String,
    ): String =
        buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            if (freezeHeader) {
                append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
            }
            append("<cols>")
            widths.forEachIndexed { i, w -> append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            append("</cols><sheetData>")
            append(body)
            append("</sheetData></worksheet>")
        }

    /** Excel file with two sheets: Transactions (one row each) and Summary (totals and spending by category). */
    fun xlsx(
        list: List<Txn>,
        look: Lookup,
        periodLabel: String,
    ): ByteArray {
        val data = rows(list, look)
        val headers = listOf("Date", "Type", "Category", "Sub-category", "Account", "To account", "Amount (INR)", "Note")

        val tx = StringBuilder()
        tx.append("""<row r="1">""")
        headers.forEachIndexed { i, h -> tx.append(text("${colName(i)}1", h, 1)) }
        tx.append("</row>")
        data.forEachIndexed { idx, r ->
            val n = idx + 2
            tx.append("""<row r="$n">""")
            tx.append(num("A$n", toLocalDate(r.dateMillis).toEpochDay() + 25569.0, 3)) // Excel counts days from 1899-12-30
            tx.append(text("B$n", r.type))
            tx.append(text("C$n", r.category))
            tx.append(text("D$n", r.sub))
            tx.append(text("E$n", r.account))
            tx.append(text("F$n", r.toAccount))
            tx.append(num("G$n", rupees(r.paise), 2))
            tx.append(text("H$n", r.note))
            tx.append("</row>")
        }

        val income = list.filter { it.kind == "income" }.sumOf { it.amountPaise }
        val expense = list.filter { it.kind == "expense" }.sumOf { it.amountPaise }
        val byCat =
            list.filter { it.kind == "expense" }
                .groupBy { if (it.categoryId.isBlank()) "Uncategorised" else look.categoryName(it.categoryId) }
                .map { (k, v) -> k to v.sumOf { it.amountPaise } }
                .sortedByDescending { it.second }

        val sm = StringBuilder()
        var r = 1
        fun line(
            a: String,
            b: Double? = null,
            style: Int = 0,
            c: Double? = null,
        ) {
            sm.append("""<row r="$r">""")
            sm.append(text("A$r", a, if (style == 1) 1 else if (style == 4) 4 else 0))
            if (b != null) sm.append(num("B$r", b, if (style == 4) 5 else 2))
            if (c != null) sm.append(num("C$r", c, 6))
            sm.append("</row>")
            r++
        }
        sm.append("""<row r="1">${text("A1", "Spendly export", 4)}</row>""")
        r = 2
        sm.append("""<row r="2">${text("A2", "Period")}${text("B2", periodLabel)}</row>""")
        sm.append("""<row r="3">${text("A3", "Transactions")}${num("B3", list.size.toDouble(), 0)}</row>""")
        r = 4
        line("Total income", rupees(income))
        line("Total expenses", rupees(expense))
        line("Net (income - expenses)", rupees(income - expense), 4)
        r++
        sm.append("""<row r="$r">${text("A$r", "Expenses by category", 1)}${text("B$r", "Amount (INR)", 1)}${text("C$r", "Share", 1)}</row>""")
        r++
        byCat.forEach { (k, v) -> line(k, rupees(v), 0, if (expense > 0) v.toDouble() / expense else 0.0) }

        val styles =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1"><numFmt numFmtId="164" formatCode="dd\-mmm\-yyyy"/></numFmts>
<fonts count="3"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF1D4ED8"/><bgColor indexed="64"/></patternFill></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="7">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
<xf numFmtId="4" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="4" fontId="2" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>
<xf numFmtId="10" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            fun put(
                name: String,
                content: String,
            ) {
                z.putNextEntry(ZipEntry(name))
                z.write(content.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            put(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""",
            )
            put(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            )
            put(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Transactions" sheetId="1" r:id="rId1"/><sheet name="Summary" sheetId="2" r:id="rId2"/></sheets></workbook>""",
            )
            put(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""",
            )
            put("xl/styles.xml", styles)
            put("xl/worksheets/sheet1.xml", sheet(listOf(13, 11, 20, 20, 18, 18, 15, 40), true, tx.toString()))
            put("xl/worksheets/sheet2.xml", sheet(listOf(30, 20, 10), false, sm.toString()))
        }
        return out.toByteArray()
    }

    // ------------------------------------------------------------------ PDF

    /** A4 PDF: title, totals, then a table that continues over as many pages as needed. */
    fun pdf(
        list: List<Txn>,
        look: Lookup,
        periodLabel: String,
    ): ByteArray {
        val data = rows(list, look)
        val income = list.filter { it.kind == "income" }.sumOf { it.amountPaise }
        val expense = list.filter { it.kind == "expense" }.sumOf { it.amountPaise }

        val w = 595
        val h = 842
        val m = 36f
        val right = w - m
        val colDate = m
        val colCat = m + 66
        val colAcc = m + 196
        val colNote = m + 290
        val rowH = 20f

        fun paint(
            size: Float,
            bold: Boolean = false,
            color: Int = Color.parseColor("#111827"),
        ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val body = paint(9f)
        val bodyRight = paint(9f, true).apply { textAlign = Paint.Align.RIGHT }
        val head = paint(9f, true, Color.WHITE)
        val headRight = paint(9f, true, Color.WHITE).apply { textAlign = Paint.Align.RIGHT }
        val muted = paint(9f, false, Color.parseColor("#6B7280"))
        val green = Color.parseColor("#15803D")
        val red = Color.parseColor("#B91C1C")
        val fill = Paint().apply { color = Color.parseColor("#1D4ED8") }
        val zebra = Paint().apply { color = Color.parseColor("#F3F4F6") }

        fun fit(
            s: String,
            p: TextPaint,
            width: Float,
        ) = TextUtils.ellipsize(s, p, width, TextUtils.TruncateAt.END).toString()

        val doc = PdfDocument()
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var c: Canvas? = null
        var y = 0f

        fun footer() {
            val cv = c ?: return
            cv.drawText("Spendly  |  page $pageNo", m, h - 24f, muted)
        }

        fun tableHeader() {
            val cv = c ?: return
            cv.drawRect(m, y, right, y + rowH, fill)
            cv.drawText("Date", colDate + 4, y + 14, head)
            cv.drawText("Category", colCat, y + 14, head)
            cv.drawText("Account", colAcc, y + 14, head)
            cv.drawText("Note", colNote, y + 14, head)
            cv.drawText("Amount", right - 4, y + 14, headRight)
            y += rowH
        }

        fun newPage() {
            page?.let {
                footer()
                doc.finishPage(it)
            }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
            page = p
            c = p.canvas
            y = m
            if (pageNo == 1) {
                val cv = p.canvas
                cv.drawText("Spendly transactions", m, y + 18, paint(20f, true))
                y += 34
                cv.drawText(periodLabel, m, y, paint(11f, false, Color.parseColor("#374151")))
                y += 14
                cv.drawText("Created ${fullDate(todayMillis())}  |  ${list.size} transactions", m, y, muted)
                y += 22
                val third = (right - m) / 3
                listOf(
                    Triple("Income", formatPaise(income), green),
                    Triple("Expenses", formatPaise(expense), red),
                    Triple("Net", formatPaise(income - expense), Color.parseColor("#111827")),
                ).forEachIndexed { i, (label, value, col) ->
                    val x = m + third * i
                    cv.drawText(label, x, y, muted)
                    cv.drawText(value, x, y + 18, paint(15f, true, col))
                }
                y += 40
            }
            tableHeader()
        }

        newPage()
        data.forEachIndexed { i, r ->
            if (y + rowH > h - 44f) newPage()
            val cv = c!!
            if (i % 2 == 0) cv.drawRect(m, y, right, y + rowH, zebra)
            cv.drawText(fullDate(r.dateMillis), colDate + 4, y + 14, body)
            val cat = if (r.sub.isBlank()) r.category else "${r.category} / ${r.sub}"
            cv.drawText(fit(cat, body, colAcc - colCat - 6), colCat, y + 14, body)
            val acc = if (r.toAccount.isBlank()) r.account else "${r.account} > ${r.toAccount}"
            cv.drawText(fit(acc, body, colNote - colAcc - 6), colAcc, y + 14, body)
            cv.drawText(fit(r.note, body, right - 90 - colNote), colNote, y + 14, body)
            val (txt, col) =
                when (r.type) {
                    "Income" -> "+" + formatPaise(r.paise) to green
                    "Expense" -> "-" + formatPaise(r.paise) to red
                    else -> formatPaise(r.paise) to Color.parseColor("#6B7280")
                }
            bodyRight.color = col
            cv.drawText(txt, right - 4, y + 14, bodyRight)
            y += rowH
        }
        if (data.isEmpty()) c?.drawText("No transactions in this period.", m, y + 16, muted)
        page?.let {
            footer()
            doc.finishPage(it)
        }
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    fun fileName(
        ext: String,
        today: LocalDate = LocalDate.now(),
    ) = "Spendly_transactions_$today.$ext"
}