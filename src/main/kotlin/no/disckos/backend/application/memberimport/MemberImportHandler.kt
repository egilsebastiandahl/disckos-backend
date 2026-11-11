package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.udiscimport.UdiscCsvParser
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.ProfileRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

data class MemberPreviewOther(val udiscName: String, val playerName: String?, val status: OtherPlayerStatus)

data class MemberPreviewCard(
    val key: String,
    val courseName: String,
    val layoutName: String,
    val startedAt: LocalDateTime?,
    /** The Disckos course, when known. */
    val locationName: String?,
    val holes: Int,
    val par: Int?,
    val status: MemberCardStatus,
    /** The uploader's score to par, when they finished. */
    val ownToPar: Int?,
    val ownStrokes: Int?,
    val others: List<MemberPreviewOther>,
)

data class MemberImportPreview(
    /** The uploader's Disckos player. */
    val playerName: String,
    /** Individual names on the cards, most cards first: who the uploader might be. */
    val namesInFile: List<MemberImportPlanner.NameInFile>,
    /** The name the cards were planned for: the one asked for, or a suggestion. Null = the uploader must pick. */
    val uploaderName: String?,
    /** Why [uploaderName] can't be used; then [cards] is empty. */
    val uploaderNameProblem: String?,
    /** Newest first. */
    val cards: List<MemberPreviewCard>,
)

data class MemberImportCommitResult(val udiscName: String, val results: List<MemberCardResult>)

/**
 * Member UDisc uploads. Preview and commit both parse the file and plan it on the server
 * ([MemberImportPlanner]); commit takes nothing from the browser but the file, who the
 * uploader is in UDisc, and which of the ready cards to upload.
 */
@Component
class MemberImportHandler(
    private val contextLoader: MemberImportContextLoader,
    private val cardWriter: MemberCardWriter,
    private val courseRequestService: CourseRequestService,
    private val locationRepository: LocationRepository,
    private val profileRepository: ProfileRepository,
    private val rateLimiter: MemberImportRateLimiter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun preview(profileId: UUID, csv: String, uploaderName: String?): MemberImportPreview {
        rateLimiter.acquire(profileId)
        val uploader = uploaderOrThrow(profileId)
        val cards = UdiscCsvParser.parse(csv)
        val ctx = contextLoader.load(uploader, cards)

        val name = uploaderName?.trim()?.takeIf(String::isNotEmpty)
            ?: MemberImportPlanner.suggestUploaderName(cards, uploader.savedUdiscName, ctx)
        val problem = name?.let { MemberImportPlanner.uploaderNameProblem(it, cards, ctx) }
        val plans = if (name == null || problem != null) emptyList() else MemberImportPlanner.plan(cards, name, ctx)
        val locationNames = locationRepository.findAllById(plans.mapNotNull { it.locationId }.toSet()).associate { it.id to it.name }

        return MemberImportPreview(
            playerName = uploader.playerName,
            namesInFile = MemberImportPlanner.namesInFile(cards),
            uploaderName = name,
            uploaderNameProblem = problem,
            cards = plans.map { it.toPreview(locationNames) }.sortedByDescending { it.startedAt },
        )
    }

    /** Not transactional: each card is written in its own transaction, so one failure doesn't stop the rest. */
    fun commit(profileId: UUID, csv: String, uploaderName: String, cardKeys: Set<String>): MemberImportCommitResult {
        rateLimiter.acquire(profileId)
        val uploader = uploaderOrThrow(profileId)
        val name = uploaderName.trim()
        val cards = UdiscCsvParser.parse(csv)
        val ctx = contextLoader.load(uploader, cards)
        MemberImportPlanner.uploaderNameProblem(name, cards, ctx)?.let { throw ResponseStatusException(HttpStatus.BAD_REQUEST, it) }

        val plans = MemberImportPlanner.plan(cards, name, ctx).filter { it.card.key in cardKeys }
        val results = plans.map { plan ->
            when (plan.status) {
                MemberCardStatus.READY -> attempt(plan) { cardWriter.write(plan, uploader) }
                // A course Disckos doesn't know: held until an admin creates or links it.
                MemberCardStatus.NEW_COURSE -> attempt(plan) { courseRequestService.hold(plan, uploader, name) }
                else -> MemberCardResult(plan.card.key, null, false, emptyList(), "Kan ikke lastes opp (${plan.status}).")
            }
        }
        rememberUdiscName(profileId, name)
        return MemberImportCommitResult(name, results)
    }

    /** Runs one card's write (or hold); a failure becomes that card's error, not the whole upload's. */
    private fun attempt(plan: MemberCardPlan, action: () -> MemberCardResult): MemberCardResult =
        try {
            action()
        } catch (ex: ResponseStatusException) {
            MemberCardResult(plan.card.key, null, false, emptyList(), ex.reason ?: "Kunne ikke lastes opp.")
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            // A double submit (or two members uploading the same card at the same moment).
            MemberCardResult(plan.card.key, null, false, emptyList(), "Runden er allerede lastet opp.")
        } catch (ex: Exception) {
            log.error("Member upload of card {} failed", plan.card.key, ex)
            MemberCardResult(plan.card.key, null, false, emptyList(), "Uventet feil. Prøv igjen.")
        }

    /** The member's own setting, so the next upload knows who they are. */
    private fun rememberUdiscName(profileId: UUID, name: String) {
        val profile = profileRepository.findById(profileId).orElse(null) ?: return
        if (profile.udiscName != name) {
            profile.udiscName = name
            profileRepository.save(profile)
        }
    }

    private fun uploaderOrThrow(profileId: UUID): Uploader =
        contextLoader.uploader(profileId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Koble kontoen din til spilleren din før du laster opp runder.")

    private fun MemberCardPlan.toPreview(locationNames: Map<UUID, String>): MemberPreviewCard {
        val mine = ownThrows
        val par = card.pars.takeIf { pars -> pars.none { it == null } }?.sumOf { it!! }
        return MemberPreviewCard(
            key = card.key,
            courseName = card.courseName,
            layoutName = card.layoutName,
            startedAt = card.startedAt,
            locationName = locationId?.let { locationNames[it] },
            holes = card.holeCount,
            par = par,
            status = status,
            ownToPar = if (mine != null && par != null) mine.sum() - par else null,
            ownStrokes = mine?.sum(),
            others = others.map { MemberPreviewOther(it.udiscName, it.playerName, it.status) },
        )
    }
}
