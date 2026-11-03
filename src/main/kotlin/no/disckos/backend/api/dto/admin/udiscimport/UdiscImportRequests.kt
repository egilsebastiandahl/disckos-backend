package no.disckos.backend.api.dto.admin.udiscimport

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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

    @field:NotNull
    val eventId: UUID,

    @field:Min(1)
    val roundOrdinal: Int,

    @field:NotBlank
    val courseName: String,

    @field:NotEmpty
    val pars: List<Int>,

    @field:NotEmpty
    @field:Valid
    val players: List<UdiscCommitPlayerRequest>,
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
