package com.kdevs.spendly.data

import com.kdevs.spendly.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Full backup (one JSON file) and CSV import. Both work on the phone's own data only.
 *
 * Restore writes every record under its original id, so restoring the same file twice changes nothing and
 * nothing already in the app is deleted. CSV import gives each row a steady id made from its contents, so
 * importing the same file again does not add the rows a second time.
 */
object Backup {
    const val VERSION = 1

    // ------------------------------------------------------------------ Backup

    fun toJson(): String {
        val root = JSONObject()
        root.put("app", "spendly")
        root.put("version", VERSION)
        root.put("savedAt", System.currentTimeMillis())
        root.put(
            "accounts",
            arr(Repo.accounts.value) {
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("type", it.type).put("icon", it.icon)
                    .put("colorKey", it.colorKey).put("openingPaise", it.openingPaise)
                    .put("archived", it.archived).put("createdAt", it.createdAt)
            },
        )
        root.put(
            "categories",
            arr(Repo.categories.value) {
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("icon", it.icon).put("colorKey", it.colorKey)
                    .put("parentId", it.parentId).put("kind", it.kind)
            },
        )
        root.put(
            "txns",
            arr(Repo.txns.value) {
                JSONObject()
                    .put("id", it.id).put("kind", it.kind).put("amountPaise", it.amountPaise)
                    .put("accountId", it.accountId).put("toAccountId", it.toAccountId)
                    .put("categoryId", it.categoryId).put("subcategoryId", it.subcategoryId)
                    .put("note", it.note).put("dateMillis", it.dateMillis).put("recurringId", it.recurringId)
            },
        )
        root.put(
            "budgets",
            arr(Repo.budgets.value) {
                JSONObject().put("id", it.id).put("categoryId", it.categoryId).put("limitPaise", it.limitPaise)
            },
        )
        root.put(
            "goals",
            arr(Repo.goals.value) {
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("icon", it.icon).put("colorKey", it.colorKey)
                    .put("targetPaise", it.targetPaise).put("savedPaise", it.savedPaise).put("deadlineMillis", it.deadlineMillis)
            },
        )
        root.put(
            "recurring",
            arr(Repo.recurring.value) {
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("kind", it.kind).put("amountPaise", it.amountPaise)
                    .put("accountId", it.accountId).put("categoryId", it.categoryId).put("subcategoryId", it.subcategoryId)
                    .put("every", it.every).put("interval", it.interval).put("nextMillis", it.nextMillis)
                    .put("active", it.active).put("anchorDay", it.anchorDay)
            },
        )
        root.put(
            "bills",
            arr(Repo.bills.value) {
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("amountPaise", it.amountPaise)
                    .put("accountId", it.accountId).put("categoryId", it.categoryId).put("subcategoryId", it.subcategoryId)
                    .put("dueMillis", it.dueMillis).put("every", it.every).put("anchorDay", it.anchorDay)
                    .put("remindDays", it.remindDays).put("paid", it.paid).put("paidMillis", it.paidMillis)
            },
        )
        return root.toString(2)
    }

    private fun <T> arr(
        list: List<T>,
        f: (T) -> JSONObject,
    ): JSONArray = JSONArray().also { a -> list.forEach { a.put(f(it)) } }

    /** Puts a backup file's records back. Returns how many records were written. Throws if the file is not a Spendly backup. */
    suspend fun restore(json: String): Int {
        val root = runCatching { JSONObject(json) }.getOrNull()
        require(root != null && root.optString("app") == "spendly") { "This is not a Spendly backup file." }
        var n = 0

        suspend fun <T : Any> load(
            key: String,
            collection: String,
            build: (JSONObject) -> Pair<String, T>,
        ) {
            val a = root.optJSONArray(key) ?: return
            val items = (0 until a.length()).map { build(a.getJSONObject(it)) }
            Repo.putMany(collection, items)
            n += items.size
        }
        // Accounts and categories first, so everything that points at them lands in a finished picture.
        load("accounts", "accounts") {
            it.getString("id") to
                Account(
                    name = it.optString("name"), type = it.optString("type", "bank"), icon = it.optString("icon", "bank"),
                    colorKey = it.optString("colorKey", "blue"), openingPaise = it.optLong("openingPaise"),
                    archived = it.optBoolean("archived"), createdAt = it.optLong("createdAt"),
                )
        }
        load("categories", "categories") {
            it.getString("id") to
                Category(
                    name = it.optString("name"), icon = it.optString("icon", "other"), colorKey = it.optString("colorKey", "slate"),
                    parentId = it.optString("parentId"), kind = it.optString("kind", "expense"),
                )
        }
        load("txns", "txns") {
            it.getString("id") to
                Txn(
                    kind = it.optString("kind", "expense"), amountPaise = it.optLong("amountPaise"),
                    accountId = it.optString("accountId"), toAccountId = it.optString("toAccountId"),
                    categoryId = it.optString("categoryId"), subcategoryId = it.optString("subcategoryId"),
                    note = it.optString("note"), dateMillis = it.optLong("dateMillis"), recurringId = it.optString("recurringId"),
                )
        }
        load("budgets", "budgets") {
            it.getString("id") to Budget(categoryId = it.optString("categoryId"), limitPaise = it.optLong("limitPaise"))
        }
        load("goals", "goals") {
            it.getString("id") to
                Goal(
                    name = it.optString("name"), icon = it.optString("icon", "goal"), colorKey = it.optString("colorKey", "green"),
                    targetPaise = it.optLong("targetPaise"), savedPaise = it.optLong("savedPaise"),
                    deadlineMillis = it.optLong("deadlineMillis"),
                )
        }
        load("recurring", "recurring") {
            it.getString("id") to
                Recurring(
                    name = it.optString("name"), kind = it.optString("kind", "expense"), amountPaise = it.optLong("amountPaise"),
                    accountId = it.optString("accountId"), categoryId = it.optString("categoryId"),
                    subcategoryId = it.optString("subcategoryId"), every = it.optString("every", "monthly"),
                    interval = it.optInt("interval", 1), nextMillis = it.optLong("nextMillis"),
                    active = it.optBoolean("active", true), anchorDay = it.optInt("anchorDay"),
                )
        }
        load("bills", "bills") {
            it.getString("id") to
                Bill(
                    name = it.optString("name"), amountPaise = it.optLong("amountPaise"), accountId = it.optString("accountId"),
                    categoryId = it.optString("categoryId"), subcategoryId = it.optString("subcategoryId"),
                    dueMillis = it.optLong("dueMillis"), every = it.optString("every", "none"), anchorDay = it.optInt("anchorDay"),
                    remindDays = it.optInt("remindDays", 1), paid = it.optBoolean("paid"), paidMillis = it.optLong("paidMillis"),
                )
        }
        return n
    }

    // ------------------------------------------------------------------ CSV import

    class CsvResult(
        val imported: Int,
        val skipped: Int,
        val newAccounts: Int,
        val newCategories: Int,
    )

    /** Splits CSV text into rows of cells. Handles quotes, commas and line breaks inside quotes, and a leading BOM. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        val src = text.removePrefix("\uFEFF")
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                quoted && c == '"' && i + 1 < src.length && src[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> {
                    row.add(cell.toString())
                    cell.clear()
                }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < src.length && src[i + 1] == '\n') i++
                    row.add(cell.toString())
                    cell.clear()
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) rows.add(row)
        return rows
    }

    private val dateOnly =
        listOf("yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "d MMM yyyy", "dd MMM yyyy", "d MMMM yyyy")
            .map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }
    private val dateTime =
        listOf("yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm", "dd/MM/yyyy HH:mm", "d/M/yyyy HH:mm", "dd-MM-yyyy HH:mm")
            .map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }

    /** Reads the dates people usually have in a sheet. Returns null when none fits. */
    fun parseDate(raw: String): Long? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        for (f in dateTime) {
            runCatching { return LocalDateTime.parse(t, f).atZone(zone).toInstant().toEpochMilli() }
        }
        for (f in dateOnly) {
            runCatching { return LocalDate.parse(t, f).startMillis() }
        }
        return null
    }

    private fun sha(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)

    /**
     * Columns (by heading, any order, any capital letters): Date, Amount (required), Type, Category, Sub-category, Account,
     * To account, Note. Type is Expense, Income or Transfer (blank means Expense). Accounts and categories that do not exist
     * yet are created.
     */
    suspend fun importCsv(text: String): CsvResult {
        val rows = parseCsv(text)
        require(rows.size >= 2) { "The file has no rows." }
        val head = rows[0].map { it.trim().lowercase().replace("_", " ").replace("-", " ") }

        fun col(vararg names: String): Int = head.indexOfFirst { it in names }
        val cDate = col("date")
        val cAmount = col("amount")
        require(cDate >= 0 && cAmount >= 0) { "The first row must have Date and Amount headings." }
        val cType = col("type", "kind")
        val cCat = col("category")
        val cSub = col("sub category", "subcategory")
        val cAcc = col("account", "from", "from account")
        val cTo = col("to account", "to")
        val cNote = col("note", "notes", "description")

        fun cell(
            r: List<String>,
            i: Int,
        ) = if (i in r.indices) r[i].trim() else ""

        val accountIds = HashMap<String, String>()
        Repo.accounts.value.forEach { accountIds[it.name.lowercase()] = it.id }
        val catIds = HashMap<String, String>() // "kind|parent|name" -> id
        Repo.categories.value.forEach { catIds["${it.kind}|${it.parentId}|${it.name.lowercase()}"] = it.id }
        var newAccounts = 0
        var newCategories = 0
        val now = System.currentTimeMillis()

        suspend fun accountId(name: String): String {
            if (name.isBlank()) return Repo.accounts.value.firstOrNull { !it.archived }?.id.orEmpty()
            val key = name.lowercase()
            accountIds[key]?.let { return it }
            val id = Repo.put("accounts", "", Account(name = name, createdAt = now))
            accountIds[key] = id
            newAccounts++
            return id
        }

        suspend fun categoryId(
            kind: String,
            parent: String,
            name: String,
        ): String {
            if (name.isBlank()) return ""
            val key = "$kind|$parent|${name.lowercase()}"
            catIds[key]?.let { return it }
            val id = Repo.put("categories", "", Category(name = name, parentId = parent, kind = kind))
            catIds[key] = id
            newCategories++
            return id
        }

        val out = mutableListOf<Pair<String, Any>>()
        val seen = HashMap<String, Int>()
        var skipped = 0
        for (r in rows.drop(1)) {
            val millis = parseDate(cell(r, cDate))
            val paise = parseRupees(cell(r, cAmount).replace("₹", "").replace("Rs.", "").replace("Rs", ""))?.let { kotlin.math.abs(it) }
            if (millis == null || paise == null || paise == 0L) {
                skipped++
                continue
            }
            val kind =
                when (cell(r, cType).lowercase()) {
                    "income" -> "income"
                    "transfer" -> "transfer"
                    else -> "expense"
                }
            val acc = accountId(cell(r, cAcc))
            if (acc.isBlank()) {
                skipped++
                continue
            }
            val toAcc = if (kind == "transfer") accountId(cell(r, cTo)) else ""
            val catKind = if (kind == "income") "income" else "expense"
            val cat = if (kind == "transfer") "" else categoryId(catKind, "", cell(r, cCat).takeUnless { it.equals("Uncategorised", true) }.orEmpty())
            val sub = if (cat.isBlank()) "" else categoryId(catKind, cat, cell(r, cSub))
            val note = cell(r, cNote)
            val sig = "$millis|$kind|$paise|$acc|$toAcc|$cat|$sub|$note"
            val nth = (seen[sig] ?: 0).also { seen[sig] = it + 1 }
            out.add(
                "imp_${sha("$sig|$nth")}" to
                    Txn(
                        kind = kind, amountPaise = paise, accountId = acc, toAccountId = toAcc,
                        categoryId = cat, subcategoryId = sub, note = note, dateMillis = millis,
                    ),
            )
        }
        Repo.putMany("txns", out)
        return CsvResult(out.size, skipped, newAccounts, newCategories)
    }
}
