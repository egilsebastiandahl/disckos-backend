package no.disckos.backend.application.event

import no.disckos.backend.api.dto.event.EventStandingsResponse
import no.disckos.backend.api.dto.event.IndividualStandingEntry
import no.disckos.backend.api.dto.event.StandingMember
import no.disckos.backend.api.dto.event.TeamStandingEntry
import no.disckos.backend.domain.EventType
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.TeamMemberScoreRepository
import no.disckos.backend.repository.TeamRepository
import no.disckos.backend.repository.TeamScoreRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class GetEventStandingsHandler(
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val teamScoreRepository: TeamScoreRepository,
    private val teamMemberScoreRepository: TeamMemberScoreRepository,
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository,
) {

    @Transactional(readOnly = true)
    fun handle(eventId: UUID): EventStandingsResponse {
        val rounds = roundRepository.findByEventId(eventId)
        if (rounds.isEmpty()) {
            return EventStandingsResponse(eventId, emptyList(), emptyList())
        }

        val roundIdToType = rounds.associate { it.id to it.eventType }
        val holes = holeRepository.findByRoundIdIn(rounds.map { it.id })
        val holeIdToPar = holes.associate { it.id to it.par }
        val holeIdToRoundId = holes.associate { it.id to it.roundId }
        val individualHoleIds = holes
            .filter { roundIdToType[it.roundId] == EventType.individual }
            .map { it.id }
        val teamHoleIds = holes
            .filter { roundIdToType[it.roundId] == EventType.team }
            .map { it.id }

        val individual = computeIndividualStandings(individualHoleIds, holeIdToPar)
        val team = computeTeamStandings(eventId, teamHoleIds, holeIdToPar, holeIdToRoundId)

        return EventStandingsResponse(eventId, individual, team)
    }

    private fun computeIndividualStandings(
        holeIds: List<UUID>,
        holeIdToPar: Map<UUID, Int>,
    ): List<IndividualStandingEntry> {
        if (holeIds.isEmpty()) return emptyList()

        val scores = playerScoreRepository.findByHoleIdIn(holeIds)
            .filter { it.throws > 0 }
        if (scores.isEmpty()) return emptyList()

        data class Acc(var strokes: Int = 0, var par: Int = 0, var holes: Int = 0)
        val byPlayer = mutableMapOf<UUID, Acc>()
        for (s in scores) {
            val par = holeIdToPar[s.holeId] ?: continue
            val acc = byPlayer.getOrPut(s.playerId) { Acc() }
            acc.strokes += s.throws
            acc.par += par
            acc.holes += 1
        }

        val playerNames = playerRepository.findAllById(byPlayer.keys)
            .associate { it.id!! to (it.name ?: "Spiller") }

        val ranked = byPlayer
            .map { (id, a) ->
                IndividualStandingEntry(
                    position = 0,
                    playerId = id,
                    playerName = playerNames[id] ?: "Spiller",
                    totalStrokes = a.strokes,
                    totalPar = a.par,
                    toPar = a.strokes - a.par,
                    holesPlayed = a.holes,
                )
            }
            .sortedBy { it.totalStrokes }

        return assignPositions(ranked) { it.totalStrokes }
    }

    private fun computeTeamStandings(
        eventId: UUID,
        holeIds: List<UUID>,
        holeIdToPar: Map<UUID, Int>,
        holeIdToRoundId: Map<UUID, UUID>,
    ): List<TeamStandingEntry> {
        if (holeIds.isEmpty()) return emptyList()

        val teamScores = teamScoreRepository.findByHoleIdIn(holeIds)
            .filter { it.teamThrows > 0 }
        if (teamScores.isEmpty()) return emptyList()

        data class Acc(var strokes: Int = 0, var par: Int = 0, var holes: Int = 0)
        val byTeam = mutableMapOf<UUID, Acc>()
        for (ts in teamScores) {
            val par = holeIdToPar[ts.holeId] ?: continue
            val acc = byTeam.getOrPut(ts.teamId) { Acc() }
            acc.strokes += ts.teamThrows
            acc.par += par
            acc.holes += 1
        }

        val teams = teamRepository.findAllById(byTeam.keys).associateBy { it.id }
        val teamMembers = teamMemberScoreRepository
            .findByTeamScoreIdIn(teamScores.map { it.id })
            .groupBy { ts ->
                teamScores.first { it.id == ts.teamScoreId }.teamId
            }
            .mapValues { (_, members) -> members.map { it.playerId }.distinct() }

        val allMemberIds = teamMembers.values.flatten().toSet()
        val memberNames = playerRepository.findAllById(allMemberIds)
            .associate { it.id!! to (it.name ?: "Spiller") }

        val ranked = byTeam
            .map { (teamId, a) ->
                val members = (teamMembers[teamId] ?: emptyList()).map { playerId ->
                    StandingMember(
                        playerId = playerId,
                        playerName = memberNames[playerId] ?: "Spiller",
                    )
                }
                TeamStandingEntry(
                    position = 0,
                    teamId = teamId,
                    teamName = teams[teamId]?.name ?: "Lag",
                    members = members.sortedBy { it.playerName },
                    totalStrokes = a.strokes,
                    totalPar = a.par,
                    toPar = a.strokes - a.par,
                    holesPlayed = a.holes,
                )
            }
            .sortedBy { it.totalStrokes }

        return assignPositions(ranked) { it.totalStrokes }
    }

    private inline fun <T> assignPositions(
        sorted: List<T>,
        sortKey: (T) -> Int,
    ): List<T> {
        if (sorted.isEmpty()) return emptyList()
        val result = mutableListOf<T>()
        var lastKey: Int? = null
        var lastPos = 0
        sorted.forEachIndexed { idx, entry ->
            val key = sortKey(entry)
            val position = if (key == lastKey) lastPos else idx + 1
            lastKey = key
            lastPos = position
            result += withPosition(entry, position)
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> withPosition(entry: T, position: Int): T = when (entry) {
        is IndividualStandingEntry -> entry.copy(position = position) as T
        is TeamStandingEntry -> entry.copy(position = position) as T
        else -> entry
    }
}
