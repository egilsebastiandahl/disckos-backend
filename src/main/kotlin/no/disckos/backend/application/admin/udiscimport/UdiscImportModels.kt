package no.disckos.backend.application.admin.udiscimport

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

/** How confidently a UDisc course name matches an event's location. Higher rank = better. */
enum class LocationMatch(val rank: Int) {
    /** An admin linked this UDisc course to the event's location in an earlier import. */
    ALIAS(3),
    /** The UDisc course name looks like the location name. */
    NAME(2),
    /** Same day, but the names don't resemble each other — needs a human look. */
    DATE_ONLY(1),
    /** This UDisc course is already linked to a *different* location. */
    OTHER_LOCATION(0),
}

enum class PlayerMatch { ALIAS, EXACT, GUESS, NONE }

// ---- Preview -------------------------------------------------------------------------

data class UdiscPreview(
    /** Number of scorecards found in the file, including those that matched no event. */
    val cardsInFile: Int,
    /** Only cards played on the same day as a Disckos event, newest first. */
    val cards: List<UdiscPreviewCard>,
)

data class UdiscPreviewCard(
    val key: String,
    val courseName: String,
    val layoutName: String,
    val startedAt: LocalDateTime?,
    /** Par per hole; holes without a par in the export default to 3 (see [warnings]). */
    val pars: List<Int>,
    val players: List<UdiscPreviewPlayer>,
    /** Every event on the card's date, best match first. */
    val eventOptions: List<UdiscEventOption>,
    val suggestedEventId: UUID,
    val suggestedRoundOrdinal: Int,
    /** Whether the card should be ticked for import by default. */
    val selected: Boolean,
    /** Card-level notes that don't depend on which event the admin picks. */
    val warnings: List<String>,
)

data class UdiscPreviewPlayer(
    val udiscName: String,
    val throws: List<Int?>,
    val suggestedPlayerId: UUID?,
    val match: PlayerMatch,
)

data class UdiscEventOption(
    val id: UUID,
    val title: String,
    val date: OffsetDateTime,
    val locationName: String?,
    val locationMatch: LocationMatch,
    val teamEvent: Boolean,
    val plannedRounds: Int,
    /** Existing rounds in creation order; round N is `rounds[N - 1]`. */
    val rounds: List<UdiscRoundOption>,
)

data class UdiscRoundOption(
    val ordinal: Int,
    val roundId: UUID,
    val pars: List<Int>,
    /** Players that already have scores in this round. */
    val playerIds: Set<UUID>,
)

// ---- Commit --------------------------------------------------------------------------

data class UdiscCommitInput(val cards: List<UdiscCommitCard>)

data class UdiscCommitCard(
    val key: String,
    val eventId: UUID,
    /** 1-based round within the event. One past the last existing round creates a new round. */
    val roundOrdinal: Int,
    val courseName: String,
    val pars: List<Int>,
    val players: List<UdiscCommitPlayer>,
)

data class UdiscCommitPlayer(
    val udiscName: String,
    /** Existing Disckos player, or null together with [newPlayerName] to create one. */
    val playerId: UUID?,
    val newPlayerName: String?,
    val throws: List<Int?>,
)

data class UdiscCardResult(
    val key: String,
    val eventId: UUID,
    val roundId: UUID?,
    val roundOrdinal: Int,
    val createdRound: Boolean,
    val importedPlayers: List<String>,
    val skippedPlayers: List<String>,
    val error: String?,
)
