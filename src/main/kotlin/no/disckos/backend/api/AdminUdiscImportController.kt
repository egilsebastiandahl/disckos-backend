package no.disckos.backend.api

import jakarta.validation.Valid
import no.disckos.backend.api.dto.admin.udiscimport.UdiscCommitRequest
import no.disckos.backend.api.dto.admin.udiscimport.UdiscCommitResponse
import no.disckos.backend.api.dto.admin.udiscimport.UdiscPreviewRequest
import no.disckos.backend.api.dto.admin.udiscimport.UdiscPreviewResponse
import no.disckos.backend.application.admin.udiscimport.UdiscImportCommitHandler
import no.disckos.backend.application.admin.udiscimport.UdiscImportPreviewHandler
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Import of UDisc "Export to CSV" files.
 *
 * Two steps: `preview` parses the file and suggests where every scorecard belongs
 * (writes nothing); `commit` writes the scorecards the admin confirmed.
 */
@RestController
@RequestMapping("/admin/import/udisc")
@PreAuthorize("hasRole('admin')")
class AdminUdiscImportController(
    private val previewHandler: UdiscImportPreviewHandler,
    private val commitHandler: UdiscImportCommitHandler,
) {
    @PostMapping("/preview")
    fun preview(@Valid @RequestBody request: UdiscPreviewRequest): UdiscPreviewResponse =
        previewHandler.handle(request.csv).toResponse()

    @PostMapping("/commit")
    fun commit(@Valid @RequestBody request: UdiscCommitRequest): UdiscCommitResponse =
        UdiscCommitResponse(results = commitHandler.handle(request.toInput()).map { it.toResponse() })
}
