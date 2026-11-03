package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import no.disckos.backend.repository.UdiscImportedCardRepository
import no.disckos.backend.repository.UdiscPlayerAliasRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.util.UUID

/**
 * Dry run of a UDisc import: parses the CSV and suggests, for every scorecard, whether it
 * belongs to a Disckos event (same day, matching course) or should become a standalone
 * round, plus which Disckos player each UDisc name is. Writes nothing.
 */
@Component
class UdiscImportPreviewHandler(
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository,
    private val playerRepository: PlayerRepository,
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val playerAliasRepository: UdiscPlayerAliasRepository,
    private val courseAliasRepository: UdiscCourseAliasRepository,
    private val importedCardRepository: UdiscImportedCardRepository,
) {
    private val oslo: ZoneId = ZoneId.of("Europe/Oslo")

    @Transactional(readOnly = true)
    fun handle(csv: String): UdiscPreview {
        val cards = UdiscCsvParser.parse(csv)
        if (cards.isEmpty()) return UdiscPreview(cardsInFile = 0, cards = emptyList())

        val eventsByDate = eventRepository.findAll().groupBy { it.date.atZoneSameInstant(oslo).toLocalDate() }
        val locations = locationRepository.findAll().associateBy { it.id }
        val courseAliases = courseAliasRepository.findAll().associate { it.udiscCourse to it.locationId }
        val players = playerRepository.findAll().filter { it.id != null && !it.name.isNullOrBlank() }
        val playerAliases = playerAliasRepository.findAll().associate { it.udiscName to it.playerId }
        val importedRounds = loadImportedRounds(cards.map { it.key })

        val eventOptionsByCard = cards.associate { card ->
            val sameDay = card.startedAt?.let { eventsByDate[it.toLocalDate()] }.orEmpty()
            card.key to sameDay
                .map { event -> event to locationMatch(card, event, locations, courseAliases) }
                // Individual events first (team import isn't supported), then best location evidence.
                .sortedWith(compareBy<Pair<EventEntity, LocationMatch>> { it.first.teamEvent }.thenByDescending { it.second.rank })
        }
        val roundsByEvent = loadEventRounds(eventOptionsByCard.values.flatten().map { it.first.id }.distinct())

        // Suggest the event only when the course looks right too — a round at another course on
        // an event day is more likely a casual round.
        val suggestedEvent = cards.associate { card ->
            card.key to eventOptionsByCard.getValue(card.key).firstOrNull()
                ?.takeIf { (event, match) -> !event.teamEvent && match.rank >= LocationMatch.NAME.rank }
                ?.first
        }

        // Several cards in one file can belong to the same event (two rounds the same day):
        // suggest round 1, 2, … in start-time order.
        val ordinals = cards.filter { suggestedEvent[it.key] != null }
            .groupBy { suggestedEvent.getValue(it.key)!!.id }
            .values
            .flatMap { group -> group.sortedBy { it.startedAt }.mapIndexed { i, card -> card.key to i + 1 } }
            .toMap()

        val previewCards = cards.map { card ->
            val options = eventOptionsByCard.getValue(card.key)
            val event = suggestedEvent[card.key]
            val ordinal = ordinals[card.key] ?: 1
            val previewPlayers = card.players.map { matchPlayer(it.name, it.throws, players, playerAliases) }
            val (locationId, courseMatch) = matchCourse(card.courseName, locations, courseAliases)
            val imported = importedRounds[card.key]

            UdiscPreviewCard(
                key = card.key,
                courseName = card.courseName,
                layoutName = card.layoutName,
                startedAt = card.startedAt,
                pars = card.pars.map { it ?: DEFAULT_PAR },
                players = previewPlayers,
                eventOptions = options.map { (e, m) -> e.toOption(m, locations, roundsByEvent[e.id].orEmpty()) },
                suggestedEventId = event?.id,
                suggestedRoundOrdinal = ordinal,
                suggestedLocationId = locationId,
                courseMatch = courseMatch,
                importedRound = imported,
                selected = defaultSelected(card, event, ordinal, roundsByEvent, previewPlayers, locationId, imported),
                warnings = cardWarnings(card, options.size),
            )
        }

        return UdiscPreview(cardsInFile = cards.size, cards = previewCards.sortedByDescending { it.startedAt })
    }

    private fun defaultSelected(
        card: UdiscCard,
        event: EventEntity?,
        ordinal: Int,
        roundsByEvent: Map<UUID, List<UdiscRoundOption>>,
        players: List<UdiscPreviewPlayer>,
        locationId: UUID?,
        imported: UdiscImportedRound?,
    ): Boolean {
        val matchedIds = players.mapNotNull { it.suggestedPlayerId }
        if (matchedIds.isEmpty()) return false
        if (imported != null && matchedIds.all { it in imported.playerIds }) return false
        if (event == null) return locationId != null && imported?.eventId == null

        val eventRounds = roundsByEvent[event.id].orEmpty()
        val targetRound = eventRounds.getOrNull(ordinal - 1)
        return ordinal <= maxOf(event.rounds, eventRounds.size) &&
            (targetRound == null || targetRound.pars.size == card.holeCount) &&
            (targetRound == null || !matchedIds.all { it in targetRound.playerIds })
    }

    private fun locationMatch(
        card: UdiscCard,
        event: EventEntity,
        locations: Map<UUID, LocationEntity>,
        courseAliases: Map<String, UUID>,
    ): LocationMatch {
        val locationId = event.locationId ?: return LocationMatch.DATE_ONLY
        val aliased = courseAliases[ImportNames.key(card.courseName)]
        val location = locations[locationId]
        return when {
            aliased == locationId -> LocationMatch.ALIAS
            aliased != null -> LocationMatch.OTHER_LOCATION
            location != null && ImportNames.courseSimilarity(card.courseName, location.name) >= 0.5 -> LocationMatch.NAME
            else -> LocationMatch.DATE_ONLY
        }
    }

    /** The Disckos location a standalone round on this course would most likely go to. */
    private fun matchCourse(
        courseName: String,
        locations: Map<UUID, LocationEntity>,
        courseAliases: Map<String, UUID>,
    ): Pair<UUID?, CourseMatch> {
        courseAliases[ImportNames.key(courseName)]?.takeIf { it in locations }?.let { return it to CourseMatch.ALIAS }
        val scored = locations.values
            .map { it to ImportNames.courseSimilarity(courseName, it.name) }
            .filter { it.second >= 0.5 }
        val best = scored.maxOfOrNull { it.second } ?: return null to CourseMatch.NONE
        // Only suggest when one location is clearly the best.
        val top = scored.filter { it.second == best }
        return if (top.size == 1) top.single().first.id to CourseMatch.NAME else null to CourseMatch.NONE
    }

    private fun matchPlayer(
        udiscName: String,
        throws: List<Int?>,
        players: List<Player>,
        aliases: Map<String, UUID>,
    ): UdiscPreviewPlayer {
        val key = ImportNames.key(udiscName)
        val aliased = aliases[key]?.takeIf { id -> players.any { it.id == id } }
        val (id, match) = when {
            aliased != null -> aliased to PlayerMatch.ALIAS
            else -> players.singleOrNull { ImportNames.key(it.name!!) == key }?.let { it.id to PlayerMatch.EXACT }
                ?: players.singleOrNull { ImportNames.looksLikePlayer(udiscName, it.name!!) }?.let { it.id to PlayerMatch.GUESS }
                ?: (null to PlayerMatch.NONE)
        }
        return UdiscPreviewPlayer(udiscName = udiscName, throws = throws, suggestedPlayerId = id, match = match)
    }

    private fun cardWarnings(card: UdiscCard, eventCount: Int): List<String> = buildList {
        val missingPar = card.pars.withIndex().filter { it.value == null }.map { it.index + 1 }
        if (missingPar.isNotEmpty()) {
            add("Eksporten mangler par for hull ${missingPar.joinToString()} – bruker par $DEFAULT_PAR.")
        }
        if (eventCount > 1) add("Det finnes $eventCount eventer denne dagen – sjekk at riktig event er valgt.")
    }

    private fun loadEventRounds(eventIds: List<UUID>): Map<UUID, List<UdiscRoundOption>> {
        val roundsByEvent = eventIds.associateWith { roundRepository.findByEventIdOrderByCreatedAtAsc(it) }
        val roundIds = roundsByEvent.values.flatten().map { it.id }
        if (roundIds.isEmpty()) return roundsByEvent.mapValues { emptyList() }
        val holesByRound = holeRepository.findByRoundIdIn(roundIds).groupBy { it.roundId }
        val playersByRound = playersByRound(holesByRound)
        return roundsByEvent.mapValues { (_, rounds) ->
            rounds.mapIndexed { i, round ->
                UdiscRoundOption(
                    ordinal = i + 1,
                    roundId = round.id,
                    pars = holesByRound[round.id].orEmpty().sortedBy { it.holeNumber }.map { it.par },
                    playerIds = playersByRound[round.id].orEmpty(),
                )
            }
        }
    }

    private fun loadImportedRounds(cardKeys: List<String>): Map<String, UdiscImportedRound> {
        val records = importedCardRepository.findAllById(cardKeys)
        if (records.isEmpty()) return emptyMap()
        val rounds = roundRepository.findAllById(records.map { it.roundId }).associateBy { it.id }
        if (rounds.isEmpty()) return emptyMap()
        val playersByRound = playersByRound(holeRepository.findByRoundIdIn(rounds.keys).groupBy { it.roundId })
        return records.mapNotNull { record ->
            rounds[record.roundId]?.let { round ->
                record.cardKey to UdiscImportedRound(round.id, round.eventId, playersByRound[round.id].orEmpty())
            }
        }.toMap()
    }

    private fun playersByRound(holesByRound: Map<UUID, List<HoleEntity>>): Map<UUID, Set<UUID>> {
        val holes = holesByRound.values.flatten()
        if (holes.isEmpty()) return emptyMap()
        val roundOfHole = holes.associate { it.id to it.roundId }
        return playerScoreRepository.findByHoleIdIn(holes.map { it.id })
            .groupBy({ roundOfHole.getValue(it.holeId) }, { it.playerId })
            .mapValues { it.value.toSet() }
    }

    private fun EventEntity.toOption(
        match: LocationMatch,
        locations: Map<UUID, LocationEntity>,
        rounds: List<UdiscRoundOption>,
    ) = UdiscEventOption(
        id = id,
        title = title,
        date = date,
        locationName = locationId?.let { locations[it]?.name },
        locationMatch = match,
        teamEvent = teamEvent,
        plannedRounds = this.rounds,
        rounds = rounds,
    )

    companion object {
        const val DEFAULT_PAR = 3
    }
}
