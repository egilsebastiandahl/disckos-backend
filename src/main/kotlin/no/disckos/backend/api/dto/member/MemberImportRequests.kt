package no.disckos.backend.api.dto.member

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import no.disckos.backend.api.dto.admin.udiscimport.MAX_UDISC_CSV_CHARS

data class MemberImportPreviewRequest(
    @field:NotBlank
    @field:Size(max = MAX_UDISC_CSV_CHARS)
    val csv: String,

    /** Who the uploader is on the cards; null = let the server suggest. */
    @field:Size(max = 200)
    val uploaderName: String? = null,
)

/** Only choices: the server parses and plans the file again and takes nothing else from the browser. */
data class MemberImportCommitRequest(
    @field:NotBlank
    @field:Size(max = MAX_UDISC_CSV_CHARS)
    val csv: String,

    @field:NotBlank
    @field:Size(max = 200)
    val uploaderName: String,

    @field:NotEmpty
    @field:Size(max = 500)
    val cardKeys: List<String>,
)
