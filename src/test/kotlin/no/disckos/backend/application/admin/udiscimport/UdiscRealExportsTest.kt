package no.disckos.backend.application.admin.udiscimport

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real "Export to CSV" files from UDisc, kept verbatim in src/test/resources/udisc-exports/.
 * Add new real exports there (and to the list below) whenever UDisc changes its format.
 */
class UdiscRealExportsTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "2026-09-04-krokhol.csv",
            "2026-09-25-holmenkollen.csv",
            "2026-10-12-muselunden.csv",
            "2026-10-31-muselunden.csv",
        ],
    )
    fun `parsed scores add up to UDisc's own Total column`(file: String) {
        val csv = requireNotNull(javaClass.getResource("/udisc-exports/$file")) { "missing fixture $file" }.readText()
        val card = UdiscCsvParser.parse(csv).single()

        // Read UDisc's Total column straight from the file, independently of the parser.
        // (These fixtures contain no quoted fields, so a plain split is enough.)
        val lines = csv.lines().filter { it.isNotBlank() }
        val header = lines.first().split(",")
        val nameCol = header.indexOf("PlayerName")
        val totalCol = header.indexOf("Total")
        val totals = lines.drop(1).associate { line -> line.split(",").let { it[nameCol] to it[totalCol].toInt() } }

        assertEquals(LocalDate.parse(file.take(10)), card.startedAt?.toLocalDate(), "round date")
        assertEquals(totals.getValue("Par"), card.pars.sumOf { it ?: 0 }, "course par")
        assertEquals(totals.keys - "Par", card.players.map { it.name }.toSet(), "players")
        card.players.forEach { p ->
            assertEquals(totals.getValue(p.name), p.throws.sumOf { it ?: 0 }, "total for ${p.name}")
            assertTrue(p.throws.none { it == null }, "${p.name} played every hole")
        }
    }

    // ---- Full history ("Rounds → ☰ → Export to CSV", 2026-11-09) ---------------------

    @Test
    fun `a full-history export splits into one card per round and every total adds up`() {
        val csv = fixture(FULL_HISTORY)
        val cards = UdiscCsvParser.parse(csv)

        // Read UDisc's Total column straight from the file. The only quoted text in this
        // file is "Brink" inside a layout name, with no commas, so a plain split is enough.
        val lines = csv.lines().filter { it.isNotBlank() }
        val header = lines.first().split(",")
        val (nameCol, courseCol, dateCol, totalCol) = listOf("PlayerName", "CourseName", "StartDate", "Total").map(header::indexOf)
        val totals = lines.drop(1).associate { line ->
            line.split(",").let { Triple(it[nameCol].trim(), it[courseCol], it[dateCol]) to it[totalCol].toInt() }
        }

        assertEquals(118, cards.size)
        assertEquals(331, cards.sumOf { it.players.size }, "every player row lands on a card")
        assertEquals(16, cards.map { it.courseName }.distinct().size)
        cards.forEach { card ->
            assertEquals(totals.getValue(Triple("Par", card.courseName, card.rawDate)), card.pars.sumOf { it ?: 0 }, "par of ${card.key}")
            assertTrue(card.pars.none { it == null }, "${card.key} has a par for every hole")
            card.players.forEach { p ->
                assertEquals(totals.getValue(Triple(p.name, card.courseName, card.rawDate)), p.throws.sumOf { it ?: 0 }, "${p.name} on ${card.key}")
            }
        }
    }

    @Test
    fun `a full-history export has 5 doubles rows and 26 unfinished rounds, none of them importable`() {
        val players = UdiscCsvParser.parse(fixture(FULL_HISTORY)).flatMap { it.players }

        val doubles = players.filter { ImportNames.isDoubles(it.name) }
        assertEquals(
            listOf("Sebastian + Eirik Roe", "Sebastian + TrippeL", "Soflinge  + Paolus", "LMG Lasse  + Lars Beinhard", "Sebastian + TrippeL"),
            doubles.map { it.name },
        )
        val (complete, unfinished) = players.filterNot { ImportNames.isDoubles(it.name) }.partition { p -> p.throws.none { it == null } }
        assertEquals(26, unfinished.size)
        assertEquals(300, complete.size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-09-04-krokhol.csv", "2026-09-25-holmenkollen.csv", "2026-10-12-muselunden.csv", "2026-10-31-muselunden.csv"])
    fun `a round gets the same card key in the full export as in its own export, so it isn't imported twice`(file: String) {
        val single = UdiscCsvParser.parse(fixture(file)).single()
        val inFull = UdiscCsvParser.parse(fixture(FULL_HISTORY)).single { it.key == single.key }

        assertEquals(single.players.map { it.name }, inFull.players.map { it.name })
        assertEquals(single.players.map { it.throws }, inFull.players.map { it.throws })
    }

    private fun fixture(file: String) =
        requireNotNull(javaClass.getResource("/udisc-exports/$file")) { "missing fixture $file" }.readText()

    companion object {
        const val FULL_HISTORY = "2026-11-09-full-history.csv"
    }
}
