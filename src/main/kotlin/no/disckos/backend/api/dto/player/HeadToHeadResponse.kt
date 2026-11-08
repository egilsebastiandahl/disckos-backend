package no.disckos.backend.api.dto.player

import java.time.OffsetDateTime
import java.util.UUID

/** Rounds two players both finished, from [playerId]'s point of view. */
data class HeadToHeadResponse(
    val playerId: UUID,
    val opponentId: UUID,
    val wins: Int,
    val losses: Int,
    val ties: Int,
    /** Newest first. */
    val rounds: List<HeadToHeadRoundResponse>
)

data class HeadToHeadRoundResponse(
    val roundId: UUID,
    /** The event's date, or a standalone round's own start time. */
    val playedAt: OffsetDateTime,
    val locationId: UUID,
    val locationName: String?,
    val holes: Int,
    val toPar: Int,
    val opponentToPar: Int
)
