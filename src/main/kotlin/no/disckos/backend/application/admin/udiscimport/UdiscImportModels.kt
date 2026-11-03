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

/** How a standalone round's location was suggested. */
enum class CourseMatch { ALIAS, NAME, NONE }

enum class PlayerMatch { ALIAS, EXACT, GUESS, NONE }

// ---- Preview -------------------------------------------------------------------------

data class UdiscPreview(
    val cardsInFile: Int,
    /** Every scorecard in the file, newest first. */
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
    /** Events on the card's date, best match first. Empty when there was no event that day. */
    val eventOptions: List<UdiscEventOption>,
    /** The event this card most likely belongs to; null = suggest a standalone round. */
    val suggestedEventId: UUID?,
    /** Round within [suggestedEventId]; 1 when standalone. */
    val suggestedRoundOrdinal: Int,
    /** Location for a standalone round; null when no Disckos location matches the course. */
    val suggestedLocationId: UUID?,
    val courseMatch: CourseMatch,
    /** Where this card was imported before, if it was. */
    val importedRound: UdiscImportedRound?,
    /** Whether the card should be ticked for import by default. */
    val selected: Boolean,
    /** Card-level notes that don't depend on what the admin picks. */
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

data class UdiscImportedRound(
    val roundId: UUID,
    /** Null when the card was imported as a standalone round. */
    val eventId: UUID?,
    val playerIds: Set<UUID>,
)

// ---- Commit --------------------------------------------------------------------------

data class UdiscCommitInput(val cards: List<UdiscCommitCard>)

data class UdiscCommitCard(
    val key: String,
    /** Null = import as a standalone round. */
    val eventId: UUID?,
    /** Event cards: 1-based round within the event; one past the last existing round creates it. */
    val roundOrdinal: Int?,
    /** Standalone cards: an existing location, or null together with [newLocationName] to create one. */
    val locationId: UUID?,
    val newLocationName: String?,
    /** Standalone cards: when the round was played (Norwegian local time). */
    val startedAt: LocalDateTime?,
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
    /** Null for standalone rounds. */
    val eventId: UUID?,
    val roundId: UUID?,
    /** Null for standalone rounds. */
    val roundOrdinal: Int?,
    val createdRound: Boolean,
    val importedPlayers: List<String>,
    val skippedPlayers: List<String>,
    val error: String?,
)
