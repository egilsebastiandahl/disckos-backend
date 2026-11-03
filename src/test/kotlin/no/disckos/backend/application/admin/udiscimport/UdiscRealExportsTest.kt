package no.disckos.backend.application.admin.udiscimport

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
}
