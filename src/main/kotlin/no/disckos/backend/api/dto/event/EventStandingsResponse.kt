package no.disckos.backend.api.dto.event

import java.util.UUID

data class EventStandingsResponse(
    val eventId: UUID,
    val individual: List<IndividualStandingEntry>,
    val team: List<TeamStandingEntry>
)

data class IndividualStandingEntry(
    val position: Int,
    val playerId: UUID,
    val playerName: String,
    val totalStrokes: Int,
    val totalPar: Int,
    val toPar: Int,
    val holesPlayed: Int
)

data class TeamStandingEntry(
    val position: Int,
    val teamId: UUID,
    val teamName: String,
    val members: List<StandingMember>,
    val totalStrokes: Int,
    val totalPar: Int,
    val toPar: Int,
    val holesPlayed: Int
)

data class StandingMember(
    val playerId: UUID,
    val playerName: String
)
