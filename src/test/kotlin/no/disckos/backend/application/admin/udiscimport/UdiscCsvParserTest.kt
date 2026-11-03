package no.disckos.backend.application.admin.udiscimport

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UdiscCsvParserTest {

    // Newer layout (StartDate/EndDate/RoundRating). Two rounds; the 9-hole one still has an
    // empty Hole10 cell because the file is as wide as its longest course.
    private val newerExport = """
        PlayerName,CourseName,LayoutName,StartDate,EndDate,Total,+/-,RoundRating,Hole1,Hole2,Hole3,Hole4,Hole5,Hole6,Hole7,Hole8,Hole9,Hole10
        Par,Ekebergsletta Discgolfpark,Main,2026-10-28 1804,2026-10-28 1950,27,,,3,3,3,3,3,3,3,3,3,
        Sebastian Dahl,Ekebergsletta Discgolfpark,Main,2026-10-28 1804,2026-10-28 1950,29,+2,,3,4,3,3,2,4,3,4,3,
        Ola,Ekebergsletta Discgolfpark,Main,2026-10-28 1804,2026-10-28 1950,24,,,4,4,,3,3,4,3,3,0,
        Par,"Holmenkollen, Oslo",Long,2026-10-20 1200,2026-10-20 1400,31,,,3,3,3,3,3,3,3,3,3,4
        Ola,"Holmenkollen, Oslo",Long,2026-10-20 1200,2026-10-20 1400,33,+2,,3,3,3,3,3,3,3,3,4,5
    """.trimIndent()

    @Test
    fun `groups rows into one card per round`() {
        val cards = UdiscCsvParser.parse(newerExport)

        assertEquals(2, cards.size)
        val ekeberg = cards[0]
        assertEquals("Ekebergsletta Discgolfpark", ekeberg.courseName)
        assertEquals("Main", ekeberg.layoutName)
        assertEquals(LocalDateTime.of(2026, 10, 28, 18, 4), ekeberg.startedAt)
        assertEquals(listOf("Sebastian Dahl", "Ola"), ekeberg.players.map { it.name })
    }

    @Test
    fun `uses the par row to cut off empty trailing hole columns`() {
        val (ekeberg, holmenkollen) = UdiscCsvParser.parse(newerExport)

        assertEquals(9, ekeberg.holeCount)
        assertEquals(List(9) { 3 }, ekeberg.pars)
        assertEquals(10, holmenkollen.holeCount)
        assertEquals(4, holmenkollen.pars.last())
    }

    @Test
    fun `unplayed holes become null`() {
        val ola = UdiscCsvParser.parse(newerExport)[0].players[1]

        assertNull(ola.throws[2], "empty cell")
        assertNull(ola.throws[8], "zero")
        assertEquals(listOf(4, 4, null, 3, 3, 4, 3, 3, null), ola.throws)
    }

    @Test
    fun `handles quoted fields containing commas`() {
        assertEquals("Holmenkollen, Oslo", UdiscCsvParser.parse(newerExport)[1].courseName)
    }

    @Test
    fun `reads the older layout with a single Date column`() {
        val csv = """
            PlayerName,CourseName,LayoutName,Date,Total,+/-,Hole1,Hole2,Hole3
            Par,Vollen,Main,2021-07-03 1340,9,,3,3,3
            Kari,Vollen,Main,2021-07-03 1340,10,+1,3,4,3
        """.trimIndent()

        val card = UdiscCsvParser.parse(csv).single()

        assertEquals(LocalDateTime.of(2021, 7, 3, 13, 40), card.startedAt)
        assertEquals(listOf(3, 4, 3), card.players.single().throws)
    }

    @Test
    fun `tolerates BOM, semicolons, CRLF and spaced hole headers`() {
        val csv = "﻿PlayerName;CourseName;LayoutName;Date;Hole 1;Hole 2\r\n" +
            "Par;Vollen;Main;2021-07-03 13:40;3;4\r\n" +
            "Kari;Vollen;Main;2021-07-03 13:40;2;4\r\n"

        val card = UdiscCsvParser.parse(csv).single()

        assertEquals(listOf(3, 4), card.pars)
        assertEquals(listOf(2, 4), card.players.single().throws)
    }

    @Test
    fun `without a par row the hole count comes from the scores`() {
        val csv = """
            PlayerName,CourseName,LayoutName,Date,Hole1,Hole2,Hole3,Hole4
            Kari,Vollen,Main,2021-07-03 1340,3,4,3,
        """.trimIndent()

        val card = UdiscCsvParser.parse(csv).single()

        assertEquals(3, card.holeCount)
        assertEquals(listOf(null, null, null), card.pars)
    }

    @Test
    fun `rejects files that are not UDisc exports`() {
        val ex = assertThrows<IllegalArgumentException> { UdiscCsvParser.parse("name,email\nOla,ola@example.com") }
        assertTrue(ex.message!!.contains("UDisc"))
    }

    @Test
    fun `parses the date formats UDisc has used`() {
        val expected = LocalDateTime.of(2026, 10, 28, 18, 4)
        assertEquals(expected, UdiscCsvParser.parseDate("2026-10-28 1804"))
        assertEquals(expected, UdiscCsvParser.parseDate("2026-10-28 18:04"))
        assertEquals(expected, UdiscCsvParser.parseDate("2026-10-28T18:04:00"))
        assertEquals(expected, UdiscCsvParser.parseDate("28.10.2026 18:04"))
        assertEquals(expected, UdiscCsvParser.parseDate("10/28/2026 6:04 PM"))
        // UTC instant → Norwegian local time (CET, UTC+1, in late October).
        assertEquals(expected, UdiscCsvParser.parseDate("2026-10-28T17:04:00Z"))
        assertEquals(LocalDateTime.of(2026, 10, 28, 0, 0), UdiscCsvParser.parseDate("2026-10-28"))
        assertNull(UdiscCsvParser.parseDate("i går"))
    }
}
