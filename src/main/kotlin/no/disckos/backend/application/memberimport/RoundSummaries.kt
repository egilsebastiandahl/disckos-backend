package no.disckos.backend.application.memberimport

import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.RoundRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

/** Where and when a member-uploaded round was played, for lists. */
data class RoundSummary(
    val roundId: UUID,
    val locationName: String?,
    val layoutName: String?,
    val playedAt: OffsetDateTime?,
    /** Par per hole, hole 1 first. */
    val pars: List<Int>,
)

@Component
class RoundSummaries(
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val locationRepository: LocationRepository,
) {
    /** Call inside a transaction. Rounds that no longer exist are left out. */
    fun load(roundIds: Collection<UUID>): Map<UUID, RoundSummary> {
        if (roundIds.isEmpty()) return emptyMap()
        val rounds = roundRepository.findAllById(roundIds.toSet())
        val locations = locationRepository.findAllById(rounds.mapNotNull { it.locationId }.toSet()).associate { it.id to it.name }
        val pars = holeRepository.findByRoundIdIn(rounds.map { it.id })
            .groupBy { it.roundId }
            .mapValues { (_, holes) -> holes.sortedBy { it.holeNumber }.map { it.par } }
        return rounds.associate { r ->
            r.id to RoundSummary(r.id, r.locationId?.let { locations[it] }, r.layoutName, r.playedAt, pars[r.id].orEmpty())
        }
    }
}
