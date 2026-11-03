package no.disckos.backend.application.admin.udiscimport

import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** One UDisc scorecard: a single group playing one course/layout at one start time. */
data class UdiscCard(
    /** Stable identifier within one export file: course|layout|raw date. */
    val key: String,
    val courseName: String,
    val layoutName: String,
    val rawDate: String,
    /** Local (Norwegian) start time, or null when the date column could not be read. */
    val startedAt: LocalDateTime?,
    /** Par per hole, null where the export had no par value. Size = number of holes on the card. */
    val pars: List<Int?>,
    val players: List<UdiscPlayerRow>,
) {
    val holeCount: Int get() = pars.size
}

data class UdiscPlayerRow(
    val name: String,
    /** Throws per hole, null where the hole was not played. Same size as [UdiscCard.pars]. */
    val throws: List<Int?>,
)

/**
 * Parses the CSV produced by UDisc's "Export to CSV" (Rounds → ☰ → Export to CSV).
 *
 * UDisc does not document the format and it has changed over time, e.g.
 *   PlayerName,CourseName,LayoutName,Date,Total,+/-,Hole1,Hole2,...
 *   PlayerName,CourseName,LayoutName,StartDate,EndDate,Total,+/-,RoundRating,Hole1,...
 * so columns are looked up by header name, never by position. Each round appears as a
 * "Par" row followed by one row per player, all sharing CourseName/LayoutName/date.
 */
object UdiscCsvParser {

    private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")

    private val PLAYER_HEADERS = setOf("playername", "player", "name")
    private val COURSE_HEADERS = setOf("coursename", "course")
    private val LAYOUT_HEADERS = setOf("layoutname", "layout")
    private val DATE_HEADERS = listOf("startdate", "date", "starttime", "start")
    private val HOLE_HEADER = Regex("""hole(\d+)""")

    // "2026-10-31 1411+0100" (what UDisc writes today), "2026-10-31 14:11", "2026-10-31T13:11:00Z", ...
    private val ISO_LIKE = Regex(
        """^(\d{4})-(\d{1,2})-(\d{1,2})(?:[ T](\d{1,2}):?(\d{2})(?::\d{2}(?:\.\d+)?)?\s*(Z|[+-]\d{2}:?\d{2})?)?""",
    )
    private val DOTTED = Regex("""^(\d{1,2})\.(\d{1,2})\.(\d{4})(?:,?\s+(\d{1,2})[:.](\d{2}))?""")
    private val US = Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})(?:,?\s+(\d{1,2}):(\d{2})(?::\d{2})?\s*([AaPp][Mm])?)?""")

    fun parse(csv: String): List<UdiscCard> {
        val text = csv.removePrefix("﻿")
        val rows = readRows(text, detectDelimiter(text)).filter { row -> row.any { it.isNotBlank() } }
        require(rows.isNotEmpty()) { "Filen er tom." }

        val header = rows.first().map(::normalizeHeader)
        val playerCol = header.indexOfFirst { it in PLAYER_HEADERS }
        val courseCol = header.indexOfFirst { it in COURSE_HEADERS }
        val layoutCol = header.indexOfFirst { it in LAYOUT_HEADERS }
        val dateCol = DATE_HEADERS.map { header.indexOf(it) }.firstOrNull { it >= 0 } ?: -1
        val holeCols = header.withIndex()
            .mapNotNull { (i, h) -> HOLE_HEADER.matchEntire(h)?.let { it.groupValues[1].toInt() to i } }
            .sortedBy { it.first }
            .map { it.second }

        require(playerCol >= 0 && courseCol >= 0 && dateCol >= 0 && holeCols.isNotEmpty()) {
            "Fant ikke kolonnene UDisc bruker (PlayerName, CourseName, Date/StartDate, Hole1 …). " +
                "Er dette en CSV-eksport av runder fra UDisc?"
        }

        val groups = LinkedHashMap<String, CardBuilder>()
        for (row in rows.drop(1)) {
            val name = row.cell(playerCol)
            if (name.isEmpty()) continue
            val course = row.cell(courseCol)
            val layout = row.cell(layoutCol)
            val date = row.cell(dateCol)
            val key = "$course|$layout|$date"
            val group = groups.getOrPut(key) { CardBuilder(key, course, layout, date) }
            val values = holeCols.map { parseThrows(row.cell(it)) }
            if (name.equals("par", ignoreCase = true)) group.pars = values else group.players += name to values
        }

        return groups.values.mapNotNull { it.build() }
    }

    private class CardBuilder(val key: String, val course: String, val layout: String, val rawDate: String) {
        var pars: List<Int?>? = null
        val players = mutableListOf<Pair<String, List<Int?>>>()

        fun build(): UdiscCard? {
            if (players.isEmpty()) return null
            // The export has as many HoleN columns as the longest course in the file, so a
            // 9-hole round still has empty Hole10..Hole18 cells. The par row is the best
            // signal for how many holes this card really had.
            val lastPar = pars?.indexOfLast { it != null } ?: -1
            val holeCount = if (lastPar >= 0) {
                lastPar + 1
            } else {
                players.maxOf { (_, values) -> values.indexOfLast { it != null } } + 1
            }
            if (holeCount <= 0) return null
            return UdiscCard(
                key = key,
                courseName = course,
                layoutName = layout,
                rawDate = rawDate,
                startedAt = parseDate(rawDate),
                pars = (pars ?: emptyList()).padOrTrim(holeCount),
                players = players.map { (name, values) -> UdiscPlayerRow(name, values.padOrTrim(holeCount)) },
            )
        }
    }

    internal fun parseDate(raw: String): LocalDateTime? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        return try {
            ISO_LIKE.find(s)?.let { m ->
                val local = localDateTime(m.int(1), m.int(2), m.int(3), m.intOrZero(4), m.intOrZero(5))
                // With an offset, convert to Norwegian time so a phone set to another
                // time zone doesn't move the round to another date.
                val offset = m.groups[6]?.value?.let(ZoneOffset::of)
                if (offset == null) local else local.atOffset(offset).atZoneSameInstant(OSLO).toLocalDateTime()
            } ?: DOTTED.find(s)?.let { m ->
                localDateTime(m.int(3), m.int(2), m.int(1), m.intOrZero(4), m.intOrZero(5))
            } ?: US.find(s)?.let { m ->
                var hour = m.intOrZero(4)
                val ampm = m.groups[6]?.value?.lowercase()
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0
                localDateTime(m.int(3), m.int(1), m.int(2), hour, m.intOrZero(5))
            }
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun localDateTime(y: Int, mo: Int, d: Int, h: Int, mi: Int) = LocalDateTime.of(y, mo, d, h, mi)

    private fun MatchResult.int(group: Int) = groupValues[group].toInt()
    private fun MatchResult.intOrZero(group: Int) = groups[group]?.value?.toIntOrNull() ?: 0

    private fun parseThrows(cell: String): Int? {
        val n = cell.toIntOrNull() ?: cell.replace(',', '.').toDoubleOrNull()?.toInt() ?: return null
        return n.takeIf { it > 0 }
    }

    private fun normalizeHeader(h: String) = h.trim().lowercase().filter { it.isLetterOrDigit() }

    private fun List<String>.cell(index: Int) = if (index in indices) this[index].trim() else ""

    private fun List<Int?>.padOrTrim(size: Int): List<Int?> = List(size) { i -> getOrNull(i) }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        return listOf(',', ';', '\t').maxBy { d -> firstLine.count { it == d } }
    }

    /** Minimal RFC 4180 reader: quoted fields, "" escapes, CRLF/LF line endings, newlines inside quotes. */
    private fun readRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        row.add(field.toString())
                        field.setLength(0)
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row.add(field.toString())
                        field.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
