package no.disckos.backend.api.dto.admin.udiscimport

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

data class UdiscPreviewResponse(
    val cardsInFile: Int,
    val cards: List<UdiscPreviewCardResponse>,
)

data class UdiscPreviewCardResponse(
    val key: String,
    val courseName: String,
    val layoutName: String,
    /** Local Norwegian time, ISO-8601 without offset. */
    val startedAt: LocalDateTime?,
    val pars: List<Int>,
    val players: List<UdiscPreviewPlayerResponse>,
    /** Events on the card's date, best match first; empty when there was none. */
    val eventOptions: List<UdiscEventOptionResponse>,
    /** Null = suggest a standalone round. */
    val suggestedEventId: UUID?,
    val suggestedRoundOrdinal: Int,
    val suggestedLocationId: UUID?,
    /** alias | name | none */
    val courseMatch: String,
    val importedRound: UdiscImportedRoundResponse?,
    val selected: Boolean,
    val warnings: List<String>,
)

data class UdiscPreviewPlayerResponse(
    val udiscName: String,
    val throws: List<Int?>,
    val suggestedPlayerId: UUID?,
    /** alias | exact | guess | none */
    val match: String,
)

data class UdiscEventOptionResponse(
    val id: UUID,
    val title: String,
    val date: OffsetDateTime,
    val locationName: String?,
    /** alias | name | date_only | other_location */
    val locationMatch: String,
    val teamEvent: Boolean,
    val plannedRounds: Int,
    val rounds: List<UdiscRoundOptionResponse>,
)

data class UdiscRoundOptionResponse(
    val ordinal: Int,
    val roundId: UUID,
    val pars: List<Int>,
    val playerIds: List<UUID>,
)

data class UdiscImportedRoundResponse(
    val roundId: UUID,
    /** Null when the card was imported as a standalone round. */
    val eventId: UUID?,
    val playerIds: List<UUID>,
)

data class UdiscCommitResponse(
    val results: List<UdiscCardResultResponse>,
)

data class UdiscCardResultResponse(
    val key: String,
    val eventId: UUID?,
    val roundId: UUID?,
    val roundOrdinal: Int?,
    val createdRound: Boolean,
    val importedPlayers: List<String>,
    val skippedPlayers: List<String>,
    val error: String?,
)
