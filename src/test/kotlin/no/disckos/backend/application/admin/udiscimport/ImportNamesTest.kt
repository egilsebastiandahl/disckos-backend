package no.disckos.backend.application.admin.udiscimport

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImportNamesTest {

    @Test
    fun `key trims, lower-cases and collapses whitespace`() {
        assertEquals("seb dahl", ImportNames.key("  Seb   DAHL "))
    }

    @Test
    fun `course names match despite filler words and longer UDisc names`() {
        assertEquals(1.0, ImportNames.courseSimilarity("Ekebergsletta Discgolfpark", "Ekeberg Discgolf Park"))
        assertEquals(1.0, ImportNames.courseSimilarity("Holmenkollen Disc Golf Park", "Holmenkollen"))
        assertEquals(1.0, ImportNames.courseSimilarity("Vollen DiscGolfPark", "Vollen Discgolf Park"))
    }

    @Test
    fun `different courses don't match`() {
        assertEquals(0.0, ImportNames.courseSimilarity("Kadettangen Discgolfpark", "Ekeberg Discgolfpark"))
        assertEquals(0.0, ImportNames.courseSimilarity("Discgolfpark", "Ekeberg"), "only filler words")
    }

    @Test
    fun `short tokens need an exact match`() {
        // "Ek" is a prefix of "Ekeberg" but too short to count.
        assertEquals(0.0, ImportNames.courseSimilarity("Ek", "Ekeberg"))
    }

    @Test
    fun `udisc nicknames look like full player names`() {
        assertTrue(ImportNames.looksLikePlayer("Seb D", "Sebastian Dahl"))
        assertTrue(ImportNames.looksLikePlayer("Sebastian", "Sebastian Dahl"))
        assertTrue(ImportNames.looksLikePlayer("sebastian dahl", "Sebastian Dahl"))
    }

    @Test
    fun `unrelated or too-short names don't look like the player`() {
        assertFalse(ImportNames.looksLikePlayer("Ola", "Sebastian Dahl"))
        assertFalse(ImportNames.looksLikePlayer("S", "Sebastian Dahl"))
        assertFalse(ImportNames.looksLikePlayer("Dahl Sebastian", "Sebastian Dahl"), "order matters")
        assertFalse(ImportNames.looksLikePlayer("Sebastian Egil Dahl", "Sebastian Dahl"), "more words than the player")
    }
}
