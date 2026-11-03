package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import no.disckos.backend.repository.UdiscPlayerAliasRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.util.UUID

/**
 * Dry run of a UDisc import: parses the CSV, keeps the scorecards played on the same day
 * as a Disckos event, and suggests event, round and player links for each. Writes nothing.
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
) {
    private val oslo: ZoneId = ZoneId.of("Europe/Oslo")

    @Transactional(readOnly = true)
    fun handle(csv: String): UdiscPreview {
        val cards = UdiscCsvParser.parse(csv)
        val eventsByDate = eventRepository.findAll().groupBy { it.date.atZoneSameInstant(oslo).toLocalDate() }

        // An export holds the scorekeeper's whole history; only same-day-as-an-event cards matter.
        val sameDay = cards.mapNotNull { card ->
            val events = card.startedAt?.let { eventsByDate[it.toLocalDate()] }
            if (events.isNullOrEmpty()) null else card to events
        }
        if (sameDay.isEmpty()) return UdiscPreview(cardsInFile = cards.size, cards = emptyList())

        val locations = locationRepository.findAll().associateBy { it.id }
        val courseAliases = courseAliasRepository.findAll().associate { it.udiscCourse to it.locationId }
        val players = playerRepository.findAll().filter { it.id != null && !it.name.isNullOrBlank() }
        val playerAliases = playerAliasRepository.findAll().associate { it.udiscName to it.playerId }
        val roundsByEvent = loadRounds(sameDay.flatMap { (_, events) -> events.map { it.id } }.distinct())

        val ranked = sameDay.map { (card, events) ->
            val options = events
                .map { event -> event to locationMatch(card, event, locations, courseAliases) }
                // Individual events first (team import isn't supported), then best location evidence.
                .sortedWith(compareBy<Pair<EventEntity, LocationMatch>> { it.first.teamEvent }.thenByDescending { it.second.rank })
            card to options
        }

        // Several cards in one file can belong to the same event (two rounds the same day):
        // suggest round 1, 2, … in start-time order.
        val ordinals = ranked
            .groupBy { (_, options) -> options.first().first.id }
            .values
            .flatMap { group -> group.sortedBy { it.first.startedAt }.mapIndexed { i, (card, _) -> card.key to i + 1 } }
            .toMap()

        val previewCards = ranked.map { (card, options) ->
            val (event, match) = options.first()
            val ordinal = ordinals.getValue(card.key)
            val eventRounds = roundsByEvent[event.id].orEmpty()
            val previewPlayers = card.players.map { matchPlayer(it.name, it.throws, players, playerAliases) }

            val targetRound = eventRounds.getOrNull(ordinal - 1)
            val matchedIds = previewPlayers.mapNotNull { it.suggestedPlayerId }
            val alreadyImported = targetRound != null && matchedIds.isNotEmpty() && matchedIds.all { it in targetRound.playerIds }
            val selected = !event.teamEvent &&
                match.rank >= LocationMatch.NAME.rank &&
                ordinal <= maxOf(event.rounds, eventRounds.size) &&
                (targetRound == null || targetRound.pars.size == card.holeCount) &&
                !alreadyImported

            UdiscPreviewCard(
                key = card.key,
                courseName = card.courseName,
                layoutName = card.layoutName,
                startedAt = card.startedAt,
                pars = card.pars.map { it ?: DEFAULT_PAR },
                players = previewPlayers,
                eventOptions = options.map { (e, m) -> e.toOption(m, locations, roundsByEvent[e.id].orEmpty()) },
                suggestedEventId = event.id,
                suggestedRoundOrdinal = ordinal,
                selected = selected,
                warnings = cardWarnings(card, options.size),
            )
        }

        return UdiscPreview(cardsInFile = cards.size, cards = previewCards.sortedByDescending { it.startedAt })
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

    private fun loadRounds(eventIds: List<UUID>): Map<UUID, List<UdiscRoundOption>> {
        val rounds = eventIds.flatMap { roundRepository.findByEventIdOrderByCreatedAtAsc(it) }
        if (rounds.isEmpty()) return emptyMap()
        val holes = holeRepository.findByRoundIdIn(rounds.map { it.id })
        val roundOfHole = holes.associate { it.id to it.roundId }
        val holesByRound = holes.groupBy { it.roundId }
        val playersByRound = (if (holes.isEmpty()) emptyList() else playerScoreRepository.findByHoleIdIn(holes.map { it.id }))
            .groupBy({ roundOfHole.getValue(it.holeId) }, { it.playerId })
        return rounds.groupBy { it.eventId }.mapValues { (_, eventRounds) ->
            eventRounds.mapIndexed { i, round ->
                UdiscRoundOption(
                    ordinal = i + 1,
                    roundId = round.id,
                    pars = holesByRound[round.id].orEmpty().sortedBy { it.holeNumber }.map { it.par },
                    playerIds = playersByRound[round.id].orEmpty().toSet(),
                )
            }
        }
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
