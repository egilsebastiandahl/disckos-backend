package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.udiscimport.ImportNames
import no.disckos.backend.application.admin.udiscimport.UdiscCard
import no.disckos.backend.application.admin.udiscimport.UdiscPlayerRow
import no.disckos.backend.domain.CourseRequestEntity
import no.disckos.backend.domain.CourseRequestStatus
import no.disckos.backend.domain.HeldUploadEntity
import no.disckos.backend.domain.HeldUploadScoreEntity
import no.disckos.backend.domain.HeldUploadStatus
import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.domain.UdiscCourseAliasEntity
import no.disckos.backend.repository.CourseRequestRepository
import no.disckos.backend.repository.HeldUploadRepository
import no.disckos.backend.repository.HeldUploadScoreRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

/** A member's own round on a course waiting for a decision (or turned away), for their list. */
data class HeldUploadView(
    val id: UUID,
    val courseName: String,
    val layoutName: String,
    val startedAt: LocalDateTime,
    val holes: Int,
    val strokes: Int?,
    val toPar: Int?,
    val status: HeldUploadStatus,
    /** Why it wasn't added, when discarded. */
    val reason: String?,
)

data class LocationOption(val id: UUID, val name: String)

/** A UDisc course waiting for an admin, with what's needed to decide. */
data class CourseRequestView(
    val id: UUID,
    val udiscCourse: String,
    val requestedBy: String?,
    val createdAt: OffsetDateTime?,
    /** Rounds waiting on it. */
    val rounds: Int,
    /** Who uploaded them. */
    val uploaders: List<String>,
    /** UDisc layouts seen on those rounds. */
    val layouts: List<String>,
    /** Hole counts seen on those rounds. */
    val holes: List<Int>,
    val firstPlayed: LocalDateTime?,
    val lastPlayed: LocalDateTime?,
    /** Existing Disckos courses with a similar name, best first: likely the same course. */
    val suggestions: List<LocationOption>,
)

data class CourseDecisionResult(
    val locationId: UUID?,
    val locationName: String?,
    /** Held rounds added to Disckos. */
    val released: Int,
    /** Held rounds that weren't, and why. */
    val notAdded: List<String>,
)

/** A held round put back together as a UDisc card, ready to be planned again. */
data class HeldCard(val heldId: UUID, val profileId: UUID, val playerId: UUID, val uploaderName: String, val card: UdiscCard)

/**
 * Rounds on UDisc courses Disckos doesn't know yet. A member's upload holds them here; an admin
 * then creates the course, links it to an existing one, or rejects it. Creating or linking
 * remembers the UDisc name as a course alias, so later uploads on it go straight through, and
 * [HeldUploadReleaser] plans the held rounds again with the normal rules.
 */
@Component
class CourseRequestService(
    private val requestRepository: CourseRequestRepository,
    private val heldRepository: HeldUploadRepository,
    private val scoreRepository: HeldUploadScoreRepository,
    private val locationRepository: LocationRepository,
    private val courseAliasRepository: UdiscCourseAliasRepository,
    private val profileRepository: ProfileRepository,
    private val cardWriter: MemberCardWriter,
) {
    // ---- Members ------------------------------------------------------------------------

    /** Holds a NEW_COURSE card: the uploader's score and the club players' to ask, never guests'. */
    @Transactional
    fun hold(plan: MemberCardPlan, uploader: Uploader, uploaderName: String): MemberCardResult {
        require(plan.status == MemberCardStatus.NEW_COURSE) { "Card ${plan.card.key} is not on a new course: ${plan.status}" }
        val card = plan.card
        val key = ImportNames.key(card.courseName)
        val request = requestRepository.findByCourseKeyAndStatus(key, CourseRequestStatus.pending)
            ?: requestRepository.save(CourseRequestEntity(udiscCourse = card.courseName.trim(), courseKey = key, requestedBy = uploader.profileId))

        heldRepository.findByCardKeyAndPlayerId(card.key, uploader.playerId)?.let { earlier ->
            if (earlier.status == HeldUploadStatus.pending) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Runden venter allerede på at banen blir godkjent.")
            }
            // Turned away before; this is a fresh try.
            heldRepository.delete(earlier)
            heldRepository.flush()
        }

        val held = heldRepository.save(
            HeldUploadEntity(
                courseRequestId = request.id,
                profileId = uploader.profileId,
                playerId = uploader.playerId,
                uploaderUdiscName = uploaderName,
                cardKey = card.key,
                layoutName = card.layoutName,
                startedAt = card.startedAt!!,
                pars = card.pars.joinToString(","),
            ),
        )
        val rows = listOf(uploaderName to plan.ownThrows!!) +
            plan.others.filter { it.status == OtherPlayerStatus.ASK }.map { it.udiscName to it.throws.map { t -> t!! } }
        scoreRepository.saveAll(rows.map { (name, throws) -> HeldUploadScoreEntity(heldUploadId = held.id, udiscName = name, throws = throws.joinToString(",")) })

        return MemberCardResult(card.key, null, false, emptyList(), error = null, heldForCourse = request.udiscCourse)
    }

    @Transactional(readOnly = true)
    fun mine(profileId: UUID): List<HeldUploadView> {
        val held = heldRepository.findByProfileIdOrderByCreatedAtDesc(profileId)
        if (held.isEmpty()) return emptyList()
        val courses = requestRepository.findAllById(held.map { it.courseRequestId }.toSet()).associate { it.id to it.udiscCourse }
        val scores = scoreRepository.findByHeldUploadIdIn(held.map { it.id }).groupBy { it.heldUploadId }
        return held.map { h ->
            val own = scores[h.id].orEmpty().firstOrNull { ImportNames.key(it.udiscName) == ImportNames.key(h.uploaderUdiscName) }?.throwsList
            val pars = h.parList
            HeldUploadView(
                id = h.id,
                courseName = courses[h.courseRequestId] ?: "Ukjent bane",
                layoutName = h.layoutName,
                startedAt = h.startedAt,
                holes = pars.size,
                strokes = own?.sum(),
                toPar = own?.let { it.sum() - pars.sum() },
                status = h.status,
                reason = h.reason,
            )
        }
    }

    /** Takes back a waiting round, or clears away one that was turned away. */
    @Transactional
    fun removeMine(heldId: UUID, profileId: UUID) {
        val held = heldRepository.findById(heldId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke runden.") }
        if (held.profileId != profileId) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Du kan bare fjerne dine egne runder.")
        heldRepository.delete(held)
        heldRepository.flush()
        // A course nobody is waiting for any more needs no decision.
        requestRepository.findById(held.courseRequestId).orElse(null)
            ?.takeIf { it.status == CourseRequestStatus.pending }
            ?.takeIf { heldRepository.findByCourseRequestIdAndStatus(it.id, HeldUploadStatus.pending).isEmpty() }
            ?.let(requestRepository::delete)
    }

    // ---- Admins -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun pending(): List<CourseRequestView> {
        val requests = requestRepository.findByStatusOrderByCreatedAtAsc(CourseRequestStatus.pending)
        if (requests.isEmpty()) return emptyList()
        val held = heldRepository.findByCourseRequestIdIn(requests.map { it.id })
            .filter { it.status == HeldUploadStatus.pending }
            .groupBy { it.courseRequestId }
        val profileNames = profileRepository.findAllById(
            (requests.mapNotNull { it.requestedBy } + held.values.flatten().map { it.profileId }).toSet(),
        ).associate { it.id to (it.displayName ?: it.username ?: "Ukjent") }
        val locations = locationRepository.findAll()

        return requests.map { r ->
            val rounds = held[r.id].orEmpty()
            CourseRequestView(
                id = r.id,
                udiscCourse = r.udiscCourse,
                requestedBy = r.requestedBy?.let { profileNames[it] },
                createdAt = r.createdAt,
                rounds = rounds.size,
                uploaders = rounds.map { profileNames[it.profileId] ?: "Ukjent" }.distinct(),
                layouts = rounds.map { it.layoutName }.filter(String::isNotBlank).distinct(),
                holes = rounds.map { it.parList.size }.distinct().sorted(),
                firstPlayed = rounds.minOfOrNull { it.startedAt },
                lastPlayed = rounds.maxOfOrNull { it.startedAt },
                suggestions = locations
                    .map { it to ImportNames.courseSimilarity(r.udiscCourse, it.name) }
                    .filter { it.second >= 0.5 }
                    .sortedByDescending { it.second }
                    .take(3)
                    .map { (l, _) -> LocationOption(l.id, l.name) },
            )
        }
    }

    /** Creates a new Disckos course for the request. Release the held rounds afterwards, outside this transaction. */
    @Transactional
    fun createCourse(requestId: UUID, name: String, adminProfileId: UUID?): LocationEntity {
        val request = pendingRequest(requestId)
        val wanted = name.trim().replace(Regex("""\s+"""), " ")
        if (wanted.length !in 2..100) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Navnet må være mellom 2 og 100 tegn.")
        locationRepository.findAll().firstOrNull { ImportNames.key(it.name) == ImportNames.key(wanted) }?.let {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Det finnes allerede en bane som heter «${it.name}». Koble til den i stedet.")
        }
        val location = locationRepository.save(
            LocationEntity(name = wanted, description = "Opprettet fra en medlemsopplasting", lat = 0.0, lon = 0.0),
        )
        rememberCourse(request.courseKey, location.id)
        decide(request, CourseRequestStatus.created, location.id, adminProfileId)
        return location
    }

    /** Links the request's UDisc course to an existing Disckos course. Release the held rounds afterwards. */
    @Transactional
    fun linkCourse(requestId: UUID, locationId: UUID, adminProfileId: UUID?): LocationEntity {
        val request = pendingRequest(requestId)
        val location = locationRepository.findById(locationId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke banen.") }
        rememberCourse(request.courseKey, location.id)
        decide(request, CourseRequestStatus.linked, location.id, adminProfileId)
        return location
    }

    /** Turns the course away. Its rounds stay visible to their uploaders as not added, with why. */
    @Transactional
    fun reject(requestId: UUID, reason: String?, adminProfileId: UUID?): CourseDecisionResult {
        val request = pendingRequest(requestId)
        decide(request, CourseRequestStatus.rejected, null, adminProfileId)
        val why = "Banen «${request.udiscCourse}» ble ikke godkjent" + (reason?.trim()?.takeIf(String::isNotEmpty)?.let { ": $it" } ?: ".")
        val held = heldRepository.findByCourseRequestIdAndStatus(request.id, HeldUploadStatus.pending)
        held.forEach {
            it.status = HeldUploadStatus.discarded
            it.reason = why
        }
        heldRepository.saveAll(held)
        return CourseDecisionResult(null, null, released = 0, notAdded = held.map { "${it.startedAt.toLocalDate()}: $why" })
    }

    // ---- Releasing (used by HeldUploadReleaser) -----------------------------------------

    /** The waiting rounds of a decided request, as UDisc cards on its course. */
    @Transactional(readOnly = true)
    fun heldCards(requestId: UUID): List<HeldCard> {
        val request = requestRepository.findById(requestId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke forespørselen.") }
        val held = heldRepository.findByCourseRequestIdAndStatus(request.id, HeldUploadStatus.pending).sortedBy { it.createdAt }
        val scores = scoreRepository.findByHeldUploadIdIn(held.map { it.id }).groupBy { it.heldUploadId }
        return held.map { h ->
            HeldCard(
                heldId = h.id,
                profileId = h.profileId,
                playerId = h.playerId,
                uploaderName = h.uploaderUdiscName,
                card = UdiscCard(
                    key = h.cardKey,
                    courseName = request.udiscCourse,
                    layoutName = h.layoutName,
                    rawDate = h.startedAt.toString(),
                    startedAt = h.startedAt,
                    pars = h.parList,
                    players = scores[h.id].orEmpty().map { UdiscPlayerRow(it.udiscName, it.throwsList) },
                ),
            )
        }
    }

    /** Writes a released round and lets go of the held copy, together. */
    @Transactional
    fun writeReleased(plan: MemberCardPlan, uploader: Uploader, heldId: UUID): MemberCardResult {
        val result = cardWriter.write(plan, uploader)
        heldRepository.deleteById(heldId)
        return result
    }

    @Transactional
    fun discard(heldId: UUID, reason: String) {
        val held = heldRepository.findById(heldId).orElse(null) ?: return
        held.status = HeldUploadStatus.discarded
        held.reason = reason
        heldRepository.save(held)
    }

    private fun pendingRequest(requestId: UUID): CourseRequestEntity {
        val request = requestRepository.findById(requestId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke forespørselen.") }
        if (request.status != CourseRequestStatus.pending) throw ResponseStatusException(HttpStatus.CONFLICT, "Banen er allerede behandlet.")
        return request
    }

    private fun decide(request: CourseRequestEntity, status: CourseRequestStatus, locationId: UUID?, adminProfileId: UUID?) {
        request.status = status
        request.locationId = locationId
        request.decidedBy = adminProfileId
        request.decidedAt = OffsetDateTime.now()
        requestRepository.save(request)
    }

    /** The same upsert the admin import does: this UDisc course is that Disckos course from now on. */
    private fun rememberCourse(courseKey: String, locationId: UUID) {
        val alias = courseAliasRepository.findByUdiscCourse(courseKey)
        if (alias == null) {
            courseAliasRepository.save(UdiscCourseAliasEntity(udiscCourse = courseKey, locationId = locationId))
        } else if (alias.locationId != locationId) {
            alias.locationId = locationId
            courseAliasRepository.save(alias)
        }
    }
}
