package no.disckos.backend.api.dto.admin.udiscimport

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/** A full UDisc export with several years of rounds is typically well under 1 MB. */
const val MAX_UDISC_CSV_CHARS = 5_000_000

data class UdiscPreviewRequest(
    @field:NotBlank
    @field:Size(max = MAX_UDISC_CSV_CHARS)
    val csv: String,
)

data class UdiscCommitRequest(
    @field:NotEmpty
    @field:Valid
    val cards: List<UdiscCommitCardRequest>,
)

data class UdiscCommitCardRequest(
    @field:NotBlank
    val key: String,

    /** Null = import as a standalone round (not part of an event). */
    val eventId: UUID? = null,

    /** Event cards: 1-based round within the event. */
    @field:Min(1)
    val roundOrdinal: Int? = null,

    /** Standalone cards: an existing location, or null together with [newLocationName]. */
    val locationId: UUID? = null,

    val newLocationName: String? = null,

    /** Standalone cards: when the round was played, Norwegian local time. */
    val startedAt: LocalDateTime? = null,

    @field:NotBlank
    val courseName: String,

    @field:NotEmpty
    val pars: List<Int>,

    @field:NotEmpty
    @field:Valid
    val players: List<UdiscCommitPlayerRequest>,

    /** The card's UDisc layout; stored on the round. */
    val layoutName: String? = null,

    /** Whether rounds on this layout count in the rank ladder of the card's location; null = unchanged. */
    val layoutCountsForRank: Boolean? = null,
)

data class UdiscCommitPlayerRequest(
    @field:NotBlank
    val udiscName: String,

    /** Existing player, or null together with [newPlayerName] to create one. */
    val playerId: UUID? = null,

    val newPlayerName: String? = null,

    /** Throws per hole; null = hole not played. */
    @field:NotEmpty
    val throws: List<Int?>,
)
