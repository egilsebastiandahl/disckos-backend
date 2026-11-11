package no.disckos.backend.api

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import no.disckos.backend.application.memberimport.CourseDecisionResult
import no.disckos.backend.application.memberimport.CourseRequestService
import no.disckos.backend.application.memberimport.CourseRequestView
import no.disckos.backend.application.memberimport.HeldUploadReleaser
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class CreateCourseRequest(@field:NotBlank @field:Size(max = 100) val name: String)

data class LinkCourseRequest(val locationId: UUID)

data class RejectCourseRequest(@field:Size(max = 300) val reason: String? = null)

/**
 * UDisc courses members uploaded rounds on that Disckos doesn't know yet. An admin creates the
 * course, links it to an existing one, or rejects it; the held rounds then go through.
 */
@RestController
@RequestMapping("/admin/course-requests")
@PreAuthorize("hasRole('admin')")
class AdminCourseRequestController(
    private val requestService: CourseRequestService,
    private val releaser: HeldUploadReleaser,
) {
    @GetMapping
    fun pending(): List<CourseRequestView> = requestService.pending()

    /** Creates a new Disckos course under [CreateCourseRequest.name] and adds the waiting rounds to it. */
    @PostMapping("/{id}/create")
    fun create(@PathVariable id: UUID, @Valid @RequestBody body: CreateCourseRequest): CourseDecisionResult {
        val location = requestService.createCourse(id, body.name, me())
        val released = releaser.release(id)
        return CourseDecisionResult(location.id, location.name, released.released, released.notAdded)
    }

    /** The UDisc course is an existing Disckos course under another name: link it and add the waiting rounds. */
    @PostMapping("/{id}/link")
    fun link(@PathVariable id: UUID, @RequestBody body: LinkCourseRequest): CourseDecisionResult {
        val location = requestService.linkCourse(id, body.locationId, me())
        val released = releaser.release(id)
        return CourseDecisionResult(location.id, location.name, released.released, released.notAdded)
    }

    @PostMapping("/{id}/reject")
    fun reject(@PathVariable id: UUID, @Valid @RequestBody(required = false) body: RejectCourseRequest?): CourseDecisionResult =
        requestService.reject(id, body?.reason, me())

    /** The deciding admin's profile; null for the legacy username/password admin login. */
    private fun me(): UUID? =
        (SecurityContextHolder.getContext().authentication?.principal as? String)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}
