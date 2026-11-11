package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.udiscimport.ImportNames
import no.disckos.backend.application.admin.udiscimport.UdiscCard
import no.disckos.backend.application.admin.udiscimport.UdiscCsvParser
import no.disckos.backend.application.admin.udiscimport.UdiscPlayerRow
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemberImportPlannerTest {

    private val me = UUID.randomUUID()
    private val kari = UUID.randomUUID()
    private val ola = UUID.randomUUID()
    private val per = UUID.randomUUID()
    private val holmenkollen = UUID.randomUUID()
    private val ekeberg = UUID.randomUUID()
    private val start = LocalDateTime.of(2026, 10, 4, 14, 0)
    private val par3 = List(9) { 3 }

    private fun row(name: String, vararg throws: Int?) = UdiscPlayerRow(name, throws.toList().ifEmpty { List(9) { 3 } })
    private fun finished(name: String, strokes: Int = 3) = UdiscPlayerRow(name, List(9) { strokes })

    private fun card(
        vararg players: UdiscPlayerRow,
        course: String = "Holmenkollen DiscGolfpark",
        at: LocalDateTime? = start,
        pars: List<Int?> = par3,
        key: String = "$course|Main|$at",
    ) = UdiscCard(key, course, "Main", at.toString(), at, pars, players.toList())

    private fun ctx(
        players: Map<UUID, String> = mapOf(me to "Sebastian", kari to "Kari Hansen", ola to "Ola", per to "Per Olsen"),
        playerAliases: Map<String, UUID> = mapOf("seb d" to me, "kari" to kari),
        memberUdiscNames: Map<String, UUID> = mapOf("pelle" to per),
        events: Map<LocalDate, List<UUID?>> = emptyMap(),
        imported: Map<String, ImportedCardState> = emptyMap(),
        nearby: List<NearbyRound> = emptyList(),
        held: Map<String, Set<UUID>> = emptyMap(),
    ) = MemberImportContext(
        uploaderPlayerId = me,
        players = players,
        playerAliases = playerAliases,
        memberUdiscNames = memberUdiscNames,
        courseAliases = mapOf("holmenkollen discgolfpark" to holmenkollen, "ekeberg frisbeegolfbane" to ekeberg),
        eventLocationsByDate = events,
        importedCards = imported,
        nearbyRounds = nearby,
        heldCards = held,
    )

    private fun plan(card: UdiscCard, ctx: MemberImportContext = ctx(), name: String = "Seb D") =
        MemberImportPlanner.plan(listOf(card), name, ctx).single()

    @Test
    fun `your own score goes in, club players are asked, guests are left out`() {
        val plan = plan(card(finished("Seb D", 2), finished("Kari"), finished("Tante Grete")))

        assertEquals(MemberCardStatus.READY, plan.status)
        assertEquals(holmenkollen, plan.locationId)
        assertNull(plan.existingRoundId)
        assertEquals(List(9) { 2 }, plan.ownThrows)
        assertEquals(
            listOf("Kari" to OtherPlayerStatus.ASK, "Tante Grete" to OtherPlayerStatus.GUEST),
            plan.others.map { it.udiscName to it.status },
        )
        assertEquals(kari, plan.others.first().playerId)
    }

    @Test
    fun `club players are found by alias, exact name or another member's UDisc name, never by a guess`() {
        val plan = plan(card(finished("Seb D"), finished("kari"), finished("OLA"), finished("Pelle"), finished("Per O")))

        assertEquals(
            listOf(kari, ola, per, null),
            plan.others.map { it.playerId },
        )
        assertEquals(OtherPlayerStatus.GUEST, plan.others.last().status)
    }

    @Test
    fun `you can only upload rounds you played and finished`() {
        assertEquals(MemberCardStatus.NOT_ON_CARD, plan(card(finished("Kari"))).status)
        assertEquals(MemberCardStatus.DOUBLES, plan(card(finished("Seb D + Kari"))).status)
        assertEquals(MemberCardStatus.UNFINISHED, plan(card(row("Seb D", 3, 3, null, 3, 3, 3, 3, 3, 3))).status)
    }

    @Test
    fun `cards without a start time or full par are skipped`() {
        assertEquals(MemberCardStatus.INCOMPLETE_CARD, plan(card(finished("Seb D"), at = null)).status)
        assertEquals(MemberCardStatus.INCOMPLETE_CARD, plan(card(finished("Seb D"), pars = par3.dropLast(1) + null)).status)
    }

    @Test
    fun `a course nobody has linked is held for an admin, never guessed`() {
        // Similar to a known course, but nobody has linked it: an admin decides where it belongs.
        val plan = plan(card(finished("Seb D", 2), finished("Kari"), finished("Tante Grete"), course = "Holmenkollen Discgolf"))

        assertEquals(MemberCardStatus.NEW_COURSE, plan.status)
        assertNull(plan.locationId)
        assertEquals(List(9) { 2 }, plan.ownThrows)
        assertEquals(
            listOf(OtherPlayerStatus.ASK, OtherPlayerStatus.GUEST),
            plan.others.map { it.status },
        )
    }

    @Test
    fun `a round already waiting for its course isn't held twice`() {
        val c = card(finished("Seb D"), finished("Kari"), course = "Ny Bane DGP")

        assertEquals(MemberCardStatus.ALREADY_WAITING, plan(c, ctx(held = mapOf(c.key to setOf(me)))).status)

        // Kari uploaded the same card herself: she isn't asked about it again.
        val kariWaits = plan(c, ctx(held = mapOf(c.key to setOf(kari))))
        assertEquals(MemberCardStatus.NEW_COURSE, kariWaits.status)
        assertEquals(OtherPlayerStatus.ALREADY, kariWaits.others.single().status)
    }

    @Test
    fun `rounds on an event day at the event's course are left to admins`() {
        val day = start.toLocalDate()

        assertEquals(MemberCardStatus.EVENT_DAY, plan(card(finished("Seb D")), ctx(events = mapOf(day to listOf(holmenkollen)))).status)
        assertEquals(MemberCardStatus.EVENT_DAY, plan(card(finished("Seb D")), ctx(events = mapOf(day to listOf(null)))).status)
        assertEquals(MemberCardStatus.READY, plan(card(finished("Seb D")), ctx(events = mapOf(day to listOf(ekeberg)))).status)
    }

    @Test
    fun `a card already in an event is skipped`() {
        val c = card(finished("Seb D"))
        val imported = ImportedCardState(UUID.randomUUID(), eventId = UUID.randomUUID(), holeCount = 9, playerIds = emptySet(), askedPlayerIds = emptySet())

        assertEquals(MemberCardStatus.IN_EVENT, plan(c, ctx(imported = mapOf(c.key to imported))).status)
    }

    @Test
    fun `a card someone else uploaded is added to, without asking anyone twice`() {
        val c = card(finished("Seb D"), finished("Kari"), finished("Ola"), finished("Pelle"))
        val round = UUID.randomUUID()
        val imported = ImportedCardState(round, eventId = null, holeCount = 9, playerIds = setOf(ola), askedPlayerIds = setOf(kari))

        val plan = plan(c, ctx(imported = mapOf(c.key to imported)))

        assertEquals(MemberCardStatus.READY, plan.status)
        assertEquals(round, plan.existingRoundId)
        assertEquals(
            listOf(OtherPlayerStatus.ALREADY, OtherPlayerStatus.ALREADY, OtherPlayerStatus.ASK),
            plan.others.map { it.status },
        )
    }

    @Test
    fun `your score is never added twice`() {
        val c = card(finished("Seb D"))
        val imported = ImportedCardState(UUID.randomUUID(), null, 9, playerIds = setOf(me), askedPlayerIds = emptySet())

        assertEquals(MemberCardStatus.ALREADY_UPLOADED, plan(c, ctx(imported = mapOf(c.key to imported))).status)
    }

    @Test
    fun `the same round exported on another phone is recognised by course, time and player`() {
        val c = card(finished("Seb D"), finished("Kari"))
        fun nearby(minutes: Long, players: Set<UUID>) =
            NearbyRound(UUID.randomUUID(), holmenkollen, start.plusMinutes(minutes), players)

        assertEquals(MemberCardStatus.ALREADY_UPLOADED, plan(c, ctx(nearby = listOf(nearby(20, setOf(me))))).status)
        assertEquals(MemberCardStatus.READY, plan(c, ctx(nearby = listOf(nearby(45, setOf(me))))).status)

        // Kari's score is already in through her own card: she isn't asked.
        val withKari = plan(c, ctx(nearby = listOf(nearby(-10, setOf(kari)))))
        assertEquals(MemberCardStatus.READY, withKari.status)
        assertEquals(OtherPlayerStatus.ALREADY, withKari.others.single().status)
    }

    @Test
    fun `a card with a different number of holes than its round is skipped`() {
        val c = card(finished("Seb D"))
        val imported = ImportedCardState(UUID.randomUUID(), null, holeCount = 18, playerIds = emptySet(), askedPlayerIds = emptySet())

        assertEquals(MemberCardStatus.HOLE_MISMATCH, plan(c, ctx(imported = mapOf(c.key to imported))).status)
    }

    @Test
    fun `nobody is asked twice on one card, and nobody else can claim your score`() {
        // "Sebastian" is the uploader's own player name, written by a co-player who added them twice.
        val plan = plan(card(finished("Seb D"), finished("Kari"), finished("kari"), finished("Sebastian")))

        assertEquals(
            listOf(OtherPlayerStatus.ASK, OtherPlayerStatus.ALREADY, OtherPlayerStatus.ALREADY),
            plan.others.map { it.status },
        )
    }

    @Test
    fun `others who didn't finish, or played doubles, are left out`() {
        val plan = plan(card(finished("Seb D"), row("Kari", 3, null, 3, 3, 3, 3, 3, 3, 3), finished("Ola + Per")))

        assertEquals(listOf(OtherPlayerStatus.UNFINISHED, OtherPlayerStatus.DOUBLES), plan.others.map { it.status })
    }

    @Test
    fun `you can't pass yourself off as another club player in UDisc`() {
        val cards = listOf(card(finished("Seb D"), finished("Kari"), finished("Ola"), finished("Pelle"), finished("Sebastian")))
        val ctx = ctx()

        assertNull(MemberImportPlanner.uploaderNameProblem("Seb D", cards, ctx))
        assertNull(MemberImportPlanner.uploaderNameProblem("Sebastian", cards, ctx))
        assertNotNull(MemberImportPlanner.uploaderNameProblem("Kari", cards, ctx)) // admin alias for Kari
        assertNotNull(MemberImportPlanner.uploaderNameProblem("Ola", cards, ctx)) // Ola's player name
        assertNotNull(MemberImportPlanner.uploaderNameProblem("Pelle", cards, ctx)) // Per's own UDisc name
        assertNotNull(MemberImportPlanner.uploaderNameProblem("Nils", cards, ctx)) // not in the file
    }

    @Test
    fun `suggests the name you used before, then one Disckos knows as you, then the one on the most cards`() {
        val cards = listOf(
            card(finished("Seb"), finished("Kari"), key = "a"),
            card(finished("Seb"), finished("Ola"), key = "b"),
            card(finished("Seb D"), key = "c"),
        )
        val ctx = ctx()

        assertEquals("Ola", MemberImportPlanner.suggestUploaderName(cards, savedName = "ola", ctx))
        assertEquals("Seb D", MemberImportPlanner.suggestUploaderName(cards, savedName = null, ctx)) // alias "seb d" → me
        assertEquals("Seb", MemberImportPlanner.suggestUploaderName(cards.take(2), savedName = null, ctx))
        assertNull(MemberImportPlanner.suggestUploaderName(cards.take(1), savedName = null, ctx)) // Seb or Kari?
    }

    @Test
    fun `plans a real full-history export`() {
        val csv = requireNotNull(javaClass.getResource("/udisc-exports/2026-11-09-full-history.csv")).readText()
        val cards = UdiscCsvParser.parse(csv)
        val courses = cards.map { ImportNames.key(it.courseName) }.distinct().associateWith { UUID.randomUUID() }
        val ctx = MemberImportContext(
            uploaderPlayerId = me,
            players = mapOf(me to "Sebastian", kari to "TrippeL"),
            playerAliases = emptyMap(),
            memberUdiscNames = emptyMap(),
            courseAliases = courses,
            eventLocationsByDate = emptyMap(),
            importedCards = emptyMap(),
            nearbyRounds = emptyList(),
        )

        val name = MemberImportPlanner.suggestUploaderName(cards, null, ctx)
        assertEquals("Sebastian", name)
        val plans = MemberImportPlanner.plan(cards, name!!, ctx)

        val byStatus = plans.groupingBy { it.status }.eachCount()
        assertTrue(byStatus.getValue(MemberCardStatus.READY) >= 100, "$byStatus")
        // TrippeL is the only other club player in this context, and is asked on every card they finished.
        val asked = plans.flatMap { it.others }.filter { it.status == OtherPlayerStatus.ASK }
        assertTrue(asked.isNotEmpty() && asked.all { it.playerId == kari }, "$asked")
        // Everyone else is a guest: none of their scores would be stored.
        assertTrue(plans.flatMap { it.others }.none { it.playerId == null && it.status == OtherPlayerStatus.ASK })
    }
}
