package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.EventType
import no.disckos.backend.domain.HoleEntity
import no.disckos.backend.domain.PlayerScoreEntity
import no.disckos.backend.domain.RoundEntity
import no.disckos.backend.domain.ScoringFormat
import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.domain.UdiscPlayerAliasEntity
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import no.disckos.backend.repository.UdiscPlayerAliasRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Writes one UDisc scorecard into an event round, in its own transaction.
 *
 * A club round is usually several UDisc cards (one per group), so a card either creates
 * the round or appends its players to the existing one. Players already in the round are
 * skipped, which makes re-importing the same export harmless.
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
) {
    @Transactional
    fun write(card: ResolvedCard): UdiscCardResult {
        val event = eventRepository.findById(card.eventId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke eventet.") }
        if (event.teamEvent) {
            throw badRequest("«${event.title}» er et lag-event. UDisc-import støtter foreløpig bare individuelle runder.")
        }

        val playerIds = card.players.map { it.playerId }.toSet()
        val missing = playerIds - playerRepository.findAllById(playerIds).mapNotNull { it.id }.toSet()
        if (missing.isNotEmpty()) throw badRequest("Ukjent spiller-id: ${missing.joinToString()}")

        val rounds = roundRepository.findByEventIdOrderByCreatedAtAsc(event.id)
        val existingRound = rounds.getOrNull(card.roundOrdinal - 1)
        if (existingRound == null && card.roundOrdinal != rounds.size + 1) {
            throw badRequest("Runde ${card.roundOrdinal} finnes ikke ennå – importer runde ${rounds.size + 1} først.")
        }

        var existingHoles: List<HoleEntity> = emptyList()
        var alreadyInRound: Set<UUID> = emptySet()
        if (existingRound != null) {
            if (existingRound.eventType != EventType.individual) throw badRequest("Runde ${card.roundOrdinal} er en lagrunde.")
            existingHoles = holeRepository.findByRoundId(existingRound.id).sortedBy { it.holeNumber }
            if (existingHoles.size != card.pars.size) {
                throw badRequest(
                    "Runde ${card.roundOrdinal} har ${existingHoles.size} hull, men UDisc-kortet har ${card.pars.size}.",
                )
            }
            alreadyInRound = playerScoreRepository.findByHoleIdIn(existingHoles.map { it.id }).map { it.playerId }.toSet()
        }

        val toImport = mutableListOf<ResolvedPlayer>()
        val skipped = mutableListOf<String>()
        val seen = mutableSetOf<UUID>()
        for (p in card.players) {
            when {
                p.playerId in alreadyInRound -> skipped += "${p.udiscName} (allerede i runden)"
                !seen.add(p.playerId) -> skipped += "${p.udiscName} (samme spiller to ganger på kortet)"
                p.throws.all { it == null } -> skipped += "${p.udiscName} (ingen kast registrert)"
                else -> toImport += p
            }
        }

        // Remember the admin's decisions, even when everyone was already imported.
        event.locationId?.let { rememberCourse(card.courseName, it) }
        card.players.forEach { rememberPlayer(it.udiscName, it.playerId) }

        if (toImport.isEmpty()) {
            return result(card, roundId = existingRound?.id, created = false, imported = emptyList(), skipped = skipped)
        }

        val roundId: UUID
        val holes: List<HoleEntity>
        if (existingRound != null) {
            roundId = existingRound.id
            holes = existingHoles
        } else {
            roundId = roundRepository.save(
                RoundEntity(eventId = event.id, eventType = EventType.individual, scoringFormat = ScoringFormat.stroke),
            ).id
            holes = holeRepository.saveAll(
                card.pars.mapIndexed { i, par -> HoleEntity(roundId = roundId, holeNumber = i + 1, par = par) },
            ).sortedBy { it.holeNumber }
        }

        playerScoreRepository.saveAll(
            toImport.flatMap { p ->
                holes.mapIndexedNotNull { i, hole ->
                    p.throws[i]?.let { PlayerScoreEntity(holeId = hole.id, playerId = p.playerId, throws = it) }
                }
            },
        )

        return result(card, roundId, created = existingRound == null, imported = toImport.map { it.udiscName }, skipped = skipped)
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
