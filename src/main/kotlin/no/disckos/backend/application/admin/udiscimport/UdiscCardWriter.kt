package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.ConfirmationStatus
import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.LocationLayoutEntity
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.domain.ScoringFormat
import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.domain.UdiscImportedCardEntity
import no.disckos.backend.domain.UdiscPlayerAliasEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.LocationLayoutRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.ScoreConfirmationRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import no.disckos.backend.repository.UdiscImportedCardRepository
import no.disckos.backend.repository.UdiscPlayerAliasRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Writes one UDisc scorecard, in its own transaction, either into an event round or as a
 * standalone round.
 *
 * A club round is usually several UDisc cards (one per group), so an event card either
 * creates the round or appends its players to the existing one. A standalone card gets a
 * round of its own. Imported cards are recorded in udisc_imported_cards: importing a card
 * again appends any newly linked players to the same round, and a card can't be counted
 * twice (once standalone, once in an event).
 */
@Component
class UdiscCardWriter(
    private val eventRepository: EventRepository,
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val playerRepository: PlayerRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val playerAliasRepository: UdiscPlayerAliasRepository,
    private val courseAliasRepository: UdiscCourseAliasRepository,
    private val importedCardRepository: UdiscImportedCardRepository,
    private val locationLayoutRepository: LocationLayoutRepository,
    private val confirmationRepository: ScoreConfirmationRepository,
) {
    /** Where a card's scores go: an existing round (append) or a round to create. */
    private class Target(
        val round: RoundEntity?,
        val holes: List<HoleEntity>,
        /** Location the UDisc course should be remembered as. */
        val locationId: UUID?,
        val newRound: () -> RoundEntity,
    )

    @Transactional
    fun write(card: ResolvedCard): UdiscCardResult {
        val previous = importedCardRepository.findById(card.key).orElse(null)
        val target = if (card.eventId != null) eventTarget(card, card.eventId, previous) else standaloneTarget(card, previous)

        val playerIds = card.players.map { it.playerId }.toSet()
        val missing = playerIds - playerRepository.findAllById(playerIds).mapNotNull { it.id }.toSet()
        if (missing.isNotEmpty()) throw badRequest("Ukjent spiller-id: ${missing.joinToString()}")

        val alreadyInRound = if (target.round == null) emptySet()
        else playerScoreRepository.findByHoleIdIn(target.holes.map { it.id }).map { it.playerId }.toSet()

        val toImport = mutableListOf<ResolvedPlayer>()
        val skipped = mutableListOf<String>()
        val seen = mutableSetOf<UUID>()
        for (p in card.players) {
            when {
                ImportNames.isDoubles(p.udiscName) -> skipped += "${p.udiscName} (double – støttes ikke ennå)"
                p.throws.all { it == null } -> skipped += "${p.udiscName} (ingen kast registrert)"
                // Only finished rounds count; the rest of the card can still be imported.
                p.throws.any { it == null } -> skipped += "${p.udiscName} (fullførte ikke runden)"
                p.playerId in alreadyInRound -> skipped += "${p.udiscName} (allerede i runden)"
                !seen.add(p.playerId) -> skipped += "${p.udiscName} (samme spiller to ganger på kortet)"
                else -> toImport += p
            }
        }

        // Remember the admin's decisions, even when everyone was already imported.
        target.locationId?.let { locationId ->
            rememberCourse(card.courseName, locationId)
            if (card.layoutName != null && card.layoutCountsForRank != null) {
                rememberLayout(locationId, card.layoutName, card.layoutCountsForRank)
            }
        }
        card.players.filterNot { ImportNames.isDoubles(it.udiscName) }.forEach { rememberPlayer(it.udiscName, it.playerId) }

        if (toImport.isEmpty()) {
            return result(card, roundId = target.round?.id, created = false, imported = emptyList(), skipped = skipped)
        }

        val round = target.round ?: roundRepository.save(target.newRound())
        // Rounds imported before migration 017, or created in Disckos, may not know their layout yet.
        // The round is managed here, so the change is flushed with the transaction.
        if (round.layoutName == null) round.layoutName = card.layoutName
        val holes = if (target.round != null) {
            target.holes
        } else {
            holeRepository.saveAll(
                card.pars.mapIndexed { i, par -> HoleEntity(roundId = round.id, holeNumber = i + 1, par = par) },
            ).sortedBy { it.holeNumber }
        }

        playerScoreRepository.saveAll(
            toImport.flatMap { p ->
                holes.mapIndexedNotNull { i, hole ->
                    p.throws[i]?.let { PlayerScoreEntity(holeId = hole.id, playerId = p.playerId, throws = it) }
                }
            },
        )
        if (previous == null) importedCardRepository.save(UdiscImportedCardEntity(cardKey = card.key, roundId = round.id))
        settleConfirmations(round.id, toImport.map { it.playerId })

        return result(card, round.id, created = target.round == null, imported = toImport.map { it.udiscName }, skipped = skipped)
    }

    private fun eventTarget(card: ResolvedCard, eventId: UUID, previous: UdiscImportedCardEntity?): Target {
        val event = eventRepository.findById(eventId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke eventet.") }
        if (event.teamEvent) {
            throw badRequest("«${event.title}» er et lag-event. UDisc-import støtter foreløpig bare individuelle runder.")
        }

        val ordinal = card.roundOrdinal ?: throw badRequest("Kortet mangler runde.")
        val rounds = roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)
        val existingRound = rounds.getOrNull(ordinal - 1)
        if (existingRound == null && ordinal != rounds.size + 1) {
            throw badRequest("Runde $ordinal finnes ikke ennå – importer runde ${rounds.size + 1} først.")
        }
        if (previous != null && previous.roundId != existingRound?.id) {
            throw badRequest(alreadyImportedElsewhere(previous))
        }

        val holes = existingRound?.let { holesOf(it, ordinal, card.pars.size) }.orEmpty()
        return Target(existingRound, holes, event.locationId) {
            RoundEntity(
                eventId = event.id,
                eventType = EventType.individual,
                scoringFormat = ScoringFormat.stroke,
                layoutName = card.layoutName,
            )
        }
    }

    private fun standaloneTarget(card: ResolvedCard, previous: UdiscImportedCardEntity?): Target {
        val locationId = card.locationId ?: throw badRequest("Kortet mangler lokasjon.")
        val playedAt = card.playedAt ?: throw badRequest("Kortet mangler tidspunkt.")

        if (previous != null) {
            val round = roundRepository.findById(previous.roundId).orElse(null)
            if (round != null) {
                if (round.eventId != null) throw badRequest(alreadyImportedElsewhere(previous))
                // Imported before: add any newly linked players to the same round.
                return Target(round, holesOf(round, null, card.pars.size), round.locationId ?: locationId) {
                    error("existing round is reused")
                }
            }
        }

        return Target(null, emptyList(), locationId) {
            RoundEntity(
                eventId = null,
                eventType = EventType.individual,
                scoringFormat = ScoringFormat.stroke,
                locationId = locationId,
                playedAt = playedAt,
                layoutName = card.layoutName,
            )
        }
    }

    private fun holesOf(round: RoundEntity, ordinal: Int?, expected: Int): List<HoleEntity> {
        if (round.eventType != EventType.individual) throw badRequest("Runden er en lagrunde.")
        val holes = holeRepository.findByRoundId(round.id).sortedBy { it.holeNumber }
        if (holes.size != expected) {
            val name = ordinal?.let { "Runde $it" } ?: "Runden"
            throw badRequest("$name har ${holes.size} hull, men UDisc-kortet har $expected.")
        }
        return holes
    }

    private fun alreadyImportedElsewhere(previous: UdiscImportedCardEntity): String {
        val round = roundRepository.findById(previous.roundId).orElse(null)
        val eventTitle = round?.eventId?.let { eventRepository.findById(it).orElse(null)?.title }
        return if (eventTitle != null) {
            "Kortet er allerede importert til «$eventTitle». Slett den runden først hvis det skal flyttes."
        } else {
            "Kortet er allerede importert som frittstående runde. Slett den under «Frittstående runder» først hvis det skal flyttes."
        }
    }

    /**
     * A member upload may have asked these players to confirm their score in this round. The
     * admin's import just added it, so those requests are settled (decided by nobody: the import).
     */
    private fun settleConfirmations(roundId: UUID, playerIds: List<UUID>) {
        val pending = confirmationRepository.findByRoundIdAndPlayerIdIn(roundId, playerIds)
            .filter { it.status == ConfirmationStatus.pending }
        if (pending.isEmpty()) return
        val now = OffsetDateTime.now()
        pending.forEach {
            it.status = ConfirmationStatus.confirmed
            it.decidedAt = now
        }
        confirmationRepository.saveAll(pending)
    }

    private fun rememberPlayer(udiscName: String, playerId: UUID) {
        val key = ImportNames.key(udiscName)
        if (key.isEmpty()) return
        val alias = playerAliasRepository.findByUdiscName(key)
        if (alias == null) {
            playerAliasRepository.save(UdiscPlayerAliasEntity(udiscName = key, playerId = playerId))
        } else if (alias.playerId != playerId) {
            alias.playerId = playerId
            playerAliasRepository.save(alias)
        }
    }

    private fun rememberCourse(courseName: String, locationId: UUID) {
        val key = ImportNames.key(courseName)
        if (key.isEmpty()) return
        val alias = courseAliasRepository.findByUdiscCourse(key)
        if (alias == null) {
            courseAliasRepository.save(UdiscCourseAliasEntity(udiscCourse = key, locationId = locationId))
        } else if (alias.locationId != locationId) {
            alias.locationId = locationId
            courseAliasRepository.save(alias)
        }
    }

    private fun rememberLayout(locationId: UUID, layoutName: String, countsForRank: Boolean) {
        val key = ImportNames.key(layoutName)
        if (key.isEmpty()) return
        val layout = locationLayoutRepository.findByLocationIdAndLayoutKey(locationId, key)
        if (layout == null) {
            locationLayoutRepository.save(
                LocationLayoutEntity(locationId = locationId, layoutKey = key, name = layoutName, countsForRank = countsForRank),
            )
        } else if (layout.countsForRank != countsForRank) {
            layout.countsForRank = countsForRank
            locationLayoutRepository.save(layout)
        }
    }

    private fun result(card: ResolvedCard, roundId: UUID?, created: Boolean, imported: List<String>, skipped: List<String>) =
        UdiscCardResult(
            key = card.key,
            eventId = card.eventId,
            roundId = roundId,
            roundOrdinal = card.roundOrdinal,
            createdRound = created,
            importedPlayers = imported,
            skippedPlayers = skipped,
            error = null,
        )

    private fun badRequest(message: String) = ResponseStatusException(HttpStatus.BAD_REQUEST, message)
}
