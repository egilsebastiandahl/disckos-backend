package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.domain.ScoringFormat
import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.domain.UdiscImportedCardEntity
import no.disckos.backend.domain.UdiscPlayerAliasEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import no.disckos.backend.repository.UdiscImportedCardRepository
import no.disckos.backend.repository.UdiscPlayerAliasRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UdiscImportPreviewHandlerTest {

    private val eventRepository = mock<EventRepository>()
    private val locationRepository = mock<LocationRepository>()
    private val playerRepository = mock<PlayerRepository>()
    private val roundRepository = mock<RoundRepository>()
    private val holeRepository = mock<HoleRepository>()
    private val playerScoreRepository = mock<PlayerScoreRepository>()
    private val playerAliasRepository = mock<UdiscPlayerAliasRepository>()
    private val courseAliasRepository = mock<UdiscCourseAliasRepository>()
    private val importedCardRepository = mock<UdiscImportedCardRepository>()

    private val handler = UdiscImportPreviewHandler(
        eventRepository, locationRepository, playerRepository, roundRepository,
        holeRepository, playerScoreRepository, playerAliasRepository, courseAliasRepository, importedCardRepository,
    )

    private val ekeberg = LocationEntity(name = "Ekeberg Discgolfpark", description = "", lat = 0.0, lon = 0.0)
    private val vollen = LocationEntity(name = "Vollen", description = "", lat = 0.0, lon = 0.0)
    private val tuesday = EventEntity(
        date = OffsetDateTime.parse("2026-10-28T17:00:00Z"),
        title = "Tirsdagsrunde",
        description = "",
        locationId = ekeberg.id,
        teamEvent = false,
        rounds = 1,
    )
    private val seb = player("Sebastian Dahl")
    private val ola = player("Ola Nordmann")

    // One card on the event day, one casual round two days later.
    private val csv = """
        PlayerName,CourseName,LayoutName,StartDate,EndDate,Total,+/-,RoundRating,Hole1,Hole2,Hole3
        Par,Ekebergsletta Discgolfpark,Main,2026-10-28 1804+0100,2026-10-28 1950+0100,9,,,3,3,3
        Seb D,Ekebergsletta Discgolfpark,Main,2026-10-28 1804+0100,2026-10-28 1950+0100,9,,,3,3,3
        ola nordmann,Ekebergsletta Discgolfpark,Main,2026-10-28 1804+0100,2026-10-28 1950+0100,10,,,3,4,3
        Gjest,Ekebergsletta Discgolfpark,Main,2026-10-28 1804+0100,2026-10-28 1950+0100,12,,,4,4,4
        Par,Ekebergsletta Discgolfpark,Main,2026-10-30 1200+0100,2026-10-30 1300+0100,9,,,3,3,3
        Seb D,Ekebergsletta Discgolfpark,Main,2026-10-30 1200+0100,2026-10-30 1300+0100,9,,,3,3,3
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        whenever(eventRepository.findAll()).thenReturn(listOf(tuesday))
        whenever(locationRepository.findAll()).thenReturn(listOf(ekeberg, vollen))
        whenever(playerRepository.findAll()).thenReturn(listOf(seb, ola))
    }

    private fun UdiscPreview.on(day: String) = cards.single { it.startedAt?.toLocalDate() == LocalDate.parse(day) }

    @Test
    fun `suggests the event for a card played on the event day at its course`() {
        val preview = handler.handle(csv)

        assertEquals(2, preview.cardsInFile)
        val card = preview.on("2026-10-28")
        assertEquals(tuesday.id, card.suggestedEventId)
        assertEquals(LocationMatch.NAME, card.eventOptions.single().locationMatch)
        assertEquals(1, card.suggestedRoundOrdinal)
        assertTrue(card.selected)
    }

    @Test
    fun `suggests a standalone round on the matching location when there's no event that day`() {
        val card = handler.handle(csv).on("2026-10-30")

        assertTrue(card.eventOptions.isEmpty())
        assertNull(card.suggestedEventId)
        assertEquals(ekeberg.id, card.suggestedLocationId)
        assertEquals(CourseMatch.NAME, card.courseMatch)
        assertTrue(card.selected)
    }

    @Test
    fun `a round at another course on an event day is suggested as standalone`() {
        whenever(eventRepository.findAll()).thenReturn(listOf(tuesday.also { it.locationId = vollen.id }))

        val card = handler.handle(csv).on("2026-10-28")

        assertEquals(LocationMatch.DATE_ONLY, card.eventOptions.single().locationMatch, "event still offered")
        assertNull(card.suggestedEventId)
        assertEquals(ekeberg.id, card.suggestedLocationId)
    }

    @Test
    fun `a course that matches no location leaves the location open and the card unticked`() {
        whenever(locationRepository.findAll()).thenReturn(listOf(vollen))
        whenever(eventRepository.findAll()).thenReturn(emptyList())

        val card = handler.handle(csv).on("2026-10-30")

        assertNull(card.suggestedLocationId)
        assertEquals(CourseMatch.NONE, card.courseMatch)
        assertFalse(card.selected)
    }

    @Test
    fun `matches players by exact name and by nickname, leaves unknowns open`() {
        val players = handler.handle(csv).on("2026-10-28").players

        assertEquals(listOf(PlayerMatch.GUESS, PlayerMatch.EXACT, PlayerMatch.NONE), players.map { it.match })
        assertEquals(listOf(seb.id, ola.id, null), players.map { it.suggestedPlayerId })
    }

    @Test
    fun `remembered aliases win`() {
        whenever(playerAliasRepository.findAll()).thenReturn(listOf(UdiscPlayerAliasEntity(udiscName = "gjest", playerId = ola.id!!)))
        whenever(courseAliasRepository.findAll()).thenReturn(listOf(UdiscCourseAliasEntity(udiscCourse = "ekebergsletta discgolfpark", locationId = ekeberg.id)))

        val preview = handler.handle(csv)
        val card = preview.on("2026-10-28")

        assertEquals(PlayerMatch.ALIAS, card.players[2].match)
        assertEquals(ola.id, card.players[2].suggestedPlayerId)
        assertEquals(LocationMatch.ALIAS, card.eventOptions.single().locationMatch)
        assertEquals(CourseMatch.ALIAS, preview.on("2026-10-30").courseMatch)
    }

    @Test
    fun `a course linked to another location is flagged and suggested as standalone there`() {
        whenever(courseAliasRepository.findAll()).thenReturn(listOf(UdiscCourseAliasEntity(udiscCourse = "ekebergsletta discgolfpark", locationId = vollen.id)))

        val card = handler.handle(csv).on("2026-10-28")

        assertEquals(LocationMatch.OTHER_LOCATION, card.eventOptions.single().locationMatch)
        assertNull(card.suggestedEventId)
        assertEquals(vollen.id, card.suggestedLocationId)
    }

    @Test
    fun `an event card whose players are already in the round is not preselected`() {
        val round = RoundEntity(eventId = tuesday.id, eventType = EventType.individual, scoringFormat = ScoringFormat.stroke)
        val holes = (1..3).map { HoleEntity(roundId = round.id, holeNumber = it, par = 3) }
        whenever(roundRepository.findByEventIdOrderByCreatedAtAsc(tuesday.id)).thenReturn(listOf(round))
        whenever(holeRepository.findByRoundIdIn(any())).thenReturn(holes)
        whenever(playerScoreRepository.findByHoleIdIn(any())).thenReturn(
            listOf(seb, ola).map { PlayerScoreEntity(holeId = holes[0].id, playerId = it.id!!, throws = 3) },
        )

        val card = handler.handle(csv).on("2026-10-28")

        assertEquals(setOf(seb.id, ola.id), card.eventOptions.single().rounds.single().playerIds)
        assertFalse(card.selected)
    }

    @Test
    fun `a standalone card imported before reports its round and is not preselected`() {
        val key = "Ekebergsletta Discgolfpark|Main|2026-10-30 1200+0100"
        val round = RoundEntity(
            eventId = null, eventType = EventType.individual, scoringFormat = ScoringFormat.stroke,
            locationId = ekeberg.id, playedAt = OffsetDateTime.parse("2026-10-30T12:00:00+01:00"),
        )
        val hole = HoleEntity(roundId = round.id, holeNumber = 1, par = 3)
        whenever(importedCardRepository.findAllById(any())).thenReturn(listOf(UdiscImportedCardEntity(key, round.id)))
        whenever(roundRepository.findAllById(any())).thenReturn(listOf(round))
        whenever(holeRepository.findByRoundIdIn(any())).thenReturn(listOf(hole))
        whenever(playerScoreRepository.findByHoleIdIn(any()))
            .thenReturn(listOf(PlayerScoreEntity(holeId = hole.id, playerId = seb.id!!, throws = 3)))

        val card = handler.handle(csv).on("2026-10-30")

        assertEquals(round.id, card.importedRound?.roundId)
        assertNull(card.importedRound?.eventId)
        assertEquals(setOf(seb.id!!), card.importedRound?.playerIds)
        assertFalse(card.selected)
    }

    @Test
    fun `matches a real UDisc export to its event`() {
        val muselunden = LocationEntity(name = "Muselunden", description = "", lat = 0.0, lon = 0.0)
        val saturday = EventEntity(
            date = OffsetDateTime.parse("2026-10-31T13:00:00+01:00"),
            title = "Høstcupen",
            description = "",
            locationId = muselunden.id,
            teamEvent = false,
            rounds = 1,
        )
        whenever(eventRepository.findAll()).thenReturn(listOf(tuesday, saturday))
        whenever(locationRepository.findAll()).thenReturn(listOf(ekeberg, muselunden))

        val csv = requireNotNull(javaClass.getResource("/udisc-exports/2026-10-31-muselunden.csv")).readText()
        val card = handler.handle(csv).cards.single()

        assertEquals(saturday.id, card.suggestedEventId)
        assertEquals(LocationMatch.NAME, card.eventOptions.single().locationMatch)
        assertEquals(12, card.pars.size)
        assertEquals(seb.id, card.players.single().suggestedPlayerId, "\"Sebastian\" → Sebastian Dahl")
        assertEquals(PlayerMatch.GUESS, card.players.single().match)
        assertTrue(card.selected)
    }

    private fun player(name: String) = Player().apply {
        id = UUID.randomUUID()
        this.name = name
    }
}
