package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.udiscimport.ImportNames
import no.disckos.backend.application.admin.udiscimport.UdiscCard
import no.disckos.backend.application.admin.udiscimport.UdiscPlayerRow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/** One player with a score in one round. */
data class RoundPlayer(val roundId: UUID, val playerId: UUID)

/** Why a card can or can't be uploaded, from the uploader's point of view. */
enum class MemberCardStatus {
    READY,
    /** The uploader isn't on the card: you can only upload rounds you played. */
    NOT_ON_CARD,
    /** The uploader played this as a doubles team, which isn't supported yet. */
    DOUBLES,
    /** The uploader didn't finish the round. */
    UNFINISHED,
    /** The export lacks a start time or par on some hole. */
    INCOMPLETE_CARD,
    /**
     * Disckos doesn't know this UDisc course yet. The round can be uploaded, but it's held until
     * an admin creates the course or links it to an existing one (see CourseRequestService).
     */
    NEW_COURSE,
    /** Uploaded before, and still waiting for an admin to decide about the course. */
    ALREADY_WAITING,
    /** Played on a Disckos event day at the event's course; admins import those into the event. */
    EVENT_DAY,
    /** Already imported into an event. */
    IN_EVENT,
    /** The uploader's score for this round is already in Disckos, via this card or someone else's. */
    ALREADY_UPLOADED,
    /** The card is in Disckos already, but with a different number of holes. */
    HOLE_MISMATCH,
}

/** What happens to someone else on the uploader's card. */
enum class OtherPlayerStatus {
    /** A club player: their score waits for them, or an admin, to confirm it. */
    ASK,
    /** Not a club player. Never stored. */
    GUEST,
    /** Already has this round in Disckos, or was already asked about it. */
    ALREADY,
    UNFINISHED,
    DOUBLES,
}

data class OtherPlayerPlan(
    val udiscName: String,
    val playerId: UUID?,
    val playerName: String?,
    val throws: List<Int?>,
    val status: OtherPlayerStatus,
)

data class MemberCardPlan(
    val card: UdiscCard,
    val status: MemberCardStatus,
    /** The Disckos course, once known. */
    val locationId: UUID? = null,
    /** The round this card was imported to before, to add to; null = a new round. */
    val existingRoundId: UUID? = null,
    /** The uploader's throws, when [status] is READY or NEW_COURSE. */
    val ownThrows: List<Int>? = null,
    val others: List<OtherPlayerPlan> = emptyList(),
)

/** What Disckos already has for a card that was imported before. */
data class ImportedCardState(
    val roundId: UUID,
    /** Set when the card went into an event. */
    val eventId: UUID?,
    val holeCount: Int,
    /** Players with scores in the round. */
    val playerIds: Set<UUID>,
    /** Players asked to confirm a score in the round, whatever they answered. */
    val askedPlayerIds: Set<UUID>,
)

/** A standalone round already in Disckos, for spotting the same round uploaded from another phone. */
data class NearbyRound(val roundId: UUID, val locationId: UUID, val playedAt: LocalDateTime, val playerIds: Set<UUID>)

/** Everything the planner needs to know about Disckos. Built by [MemberImportContextLoader]. */
class MemberImportContext(
    val uploaderPlayerId: UUID,
    /** Every player's name by id. */
    val players: Map<UUID, String>,
    /** Admin-curated UDisc name (normalized) → player. */
    val playerAliases: Map<String, UUID>,
    /** Other members' own UDisc names (normalized) → their player. */
    val memberUdiscNames: Map<String, UUID>,
    /** Admin-curated UDisc course (normalized) → location. */
    val courseAliases: Map<String, UUID>,
    /** Disckos events by local date: each event's location, null when it has none. */
    val eventLocationsByDate: Map<LocalDate, List<UUID?>>,
    val importedCards: Map<String, ImportedCardState>,
    val nearbyRounds: List<NearbyRound>,
    /** Card key → players whose own upload of that card waits for a course decision. */
    val heldCards: Map<String, Set<UUID>> = emptyMap(),
)

/**
 * Decides what a member's UDisc upload may do. Pure: the same file and the same Disckos state
 * give the same plan, which is why commit replans from the file instead of trusting the browser.
 *
 * The rules:
 * - only cards the uploader played and finished; on a course Disckos doesn't know yet the
 *   card is held until an admin creates or links the course, then planned again;
 * - never event rounds (admins import those into the event);
 * - the uploader's own score goes in; other club players are asked to confirm theirs;
 * - guests are left out entirely;
 * - nothing that's already in Disckos goes in twice, even from a card exported on another phone
 *   (same course, start within [SAME_ROUND_WINDOW], same player).
 */
object MemberImportPlanner {

    val SAME_ROUND_WINDOW: Duration = Duration.ofMinutes(30)
    private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")
    private val TEAM_SEPARATOR = Regex("""\s\+\s""")

    data class NameInFile(val name: String, val cards: Int)

    /** Individual (not doubles) names on the cards, most cards first. */
    fun namesInFile(cards: List<UdiscCard>): List<NameInFile> =
        cards.flatMap { card -> card.players.map { it.name.trim() }.filterNot(ImportNames::isDoubles).distinctBy(ImportNames::key) }
            .groupBy(ImportNames::key)
            .map { (_, names) -> NameInFile(names.first(), names.size) }
            .sortedWith(compareByDescending<NameInFile> { it.cards }.thenBy { it.name.lowercase() })

    /**
     * The uploader's likely UDisc name: the one they used last time, then one Disckos already
     * knows as them, then the name on the most cards (a full-history export has the
     * uploader on every card). Null when it's a guess between equals.
     */
    fun suggestUploaderName(cards: List<UdiscCard>, savedName: String?, ctx: MemberImportContext): String? {
        val names = namesInFile(cards)
        if (names.isEmpty()) return null
        savedName?.let { saved -> names.firstOrNull { ImportNames.key(it.name) == ImportNames.key(saved) }?.let { return it.name } }
        names.firstOrNull { playerFor(it.name, ctx) == ctx.uploaderPlayerId }?.let { return it.name }
        val top = names.first()
        return if (names.size == 1 || names[1].cards < top.cards) top.name else null
    }

    /** Null when the uploader may be [name] on these cards; otherwise why not (Norwegian, for the page). */
    fun uploaderNameProblem(name: String, cards: List<UdiscCard>, ctx: MemberImportContext): String? {
        val key = ImportNames.key(name)
        if (key.isEmpty()) return "Velg navnet ditt i UDisc."
        if (ImportNames.isDoubles(name)) return "«$name» er et double-lag, ikke én spiller."
        if (namesInFile(cards).none { ImportNames.key(it.name) == key }) return "Fant ikke «$name» i filen."
        ctx.playerAliases[key]?.takeIf { it != ctx.uploaderPlayerId && it in ctx.players }?.let {
            return "«$name» er koblet til ${ctx.players[it]} i Disckos. Er det feil, si fra til en admin."
        }
        val namedLikeThat = ctx.players.filterValues { ImportNames.key(it) == key }.keys
        if (namedLikeThat.isNotEmpty() && ctx.uploaderPlayerId !in namedLikeThat) {
            return "«$name» er navnet til en annen spiller i Disckos."
        }
        ctx.memberUdiscNames[key]?.takeIf { it != ctx.uploaderPlayerId }?.let {
            return "Et annet medlem bruker allerede «$name» som UDisc-navn."
        }
        return null
    }

    /** Plans every card for an uploader called [uploaderName] in UDisc. Check [uploaderNameProblem] first. */
    fun plan(cards: List<UdiscCard>, uploaderName: String, ctx: MemberImportContext): List<MemberCardPlan> =
        cards.map { planCard(it, ImportNames.key(uploaderName), ctx) }

    private fun planCard(card: UdiscCard, uploaderKey: String, ctx: MemberImportContext): MemberCardPlan {
        val mine = card.players.firstOrNull { !ImportNames.isDoubles(it.name) && ImportNames.key(it.name) == uploaderKey }
        if (mine == null) {
            val inDoubles = card.players.any { row ->
                ImportNames.isDoubles(row.name) && row.name.split(TEAM_SEPARATOR).any { ImportNames.key(it) == uploaderKey }
            }
            return MemberCardPlan(card, if (inDoubles) MemberCardStatus.DOUBLES else MemberCardStatus.NOT_ON_CARD)
        }
        if (mine.throws.any { it == null }) return MemberCardPlan(card, MemberCardStatus.UNFINISHED)
        val startedAt = card.startedAt
        if (startedAt == null || card.pars.any { it == null }) return MemberCardPlan(card, MemberCardStatus.INCOMPLETE_CARD)

        val locationId = ctx.courseAliases[ImportNames.key(card.courseName)]
        if (locationId == null) {
            // A course Disckos doesn't know: held for an admin, who decides where it belongs.
            // Event and duplicate checks need a course, so they run when the round is released.
            val held = ctx.heldCards[card.key].orEmpty()
            if (ctx.uploaderPlayerId in held) return MemberCardPlan(card, MemberCardStatus.ALREADY_WAITING)
            return MemberCardPlan(
                card = card,
                status = MemberCardStatus.NEW_COURSE,
                ownThrows = mine.throws.map { it!! },
                others = othersOn(card, mine, ctx, alreadyIn = held),
            )
        }
        val imported = ctx.importedCards[card.key]
        if (imported?.eventId != null) return MemberCardPlan(card, MemberCardStatus.IN_EVENT, locationId)
        if (imported == null) {
            val eventsThatDay = ctx.eventLocationsByDate[startedAt.toLocalDate()].orEmpty()
            if (eventsThatDay.any { it == null || it == locationId }) return MemberCardPlan(card, MemberCardStatus.EVENT_DAY, locationId)
        }
        if (imported != null && imported.holeCount != card.holeCount) {
            return MemberCardPlan(card, MemberCardStatus.HOLE_MISMATCH, locationId)
        }

        val inRound = imported?.playerIds.orEmpty()
        val asked = imported?.askedPlayerIds.orEmpty()
        val nearby = playersNearby(locationId, startedAt, imported?.roundId, ctx)
        if (ctx.uploaderPlayerId in inRound || ctx.uploaderPlayerId in nearby) {
            return MemberCardPlan(card, MemberCardStatus.ALREADY_UPLOADED, locationId, imported?.roundId)
        }

        return MemberCardPlan(
            card = card,
            status = MemberCardStatus.READY,
            locationId = locationId,
            existingRoundId = imported?.roundId,
            ownThrows = mine.throws.map { it!! },
            others = othersOn(card, mine, ctx, alreadyIn = inRound + asked + nearby),
        )
    }

    /** Everyone on the card but the uploader: club players to ask, and who's left out and why. */
    private fun othersOn(card: UdiscCard, mine: UdiscPlayerRow, ctx: MemberImportContext, alreadyIn: Set<UUID>): List<OtherPlayerPlan> {
        val seen = mutableSetOf(ctx.uploaderPlayerId)
        return card.players.filter { it !== mine }.map { row ->
            val playerId = if (ImportNames.isDoubles(row.name)) null else playerFor(row.name, ctx)
            val status = when {
                ImportNames.isDoubles(row.name) -> OtherPlayerStatus.DOUBLES
                playerId == null -> OtherPlayerStatus.GUEST
                row.throws.any { it == null } -> OtherPlayerStatus.UNFINISHED
                !seen.add(playerId) || playerId in alreadyIn -> OtherPlayerStatus.ALREADY
                else -> OtherPlayerStatus.ASK
            }
            OtherPlayerPlan(row.name, playerId, playerId?.let { ctx.players[it] }, row.throws, status)
        }
    }

    /**
     * A club player on someone's card: by the admin-curated alias, an exact player name, or the
     * UDisc name another member uses. No fuzzy guesses: a wrong match would ask the wrong person.
     */
    private fun playerFor(udiscName: String, ctx: MemberImportContext): UUID? {
        val key = ImportNames.key(udiscName)
        if (key.isEmpty()) return null
        return ctx.playerAliases[key]?.takeIf { it in ctx.players }
            ?: ctx.players.entries.singleOrNull { ImportNames.key(it.value) == key }?.key
            ?: ctx.memberUdiscNames[key]?.takeIf { it in ctx.players }
    }

    private fun playersNearby(locationId: UUID, startedAt: LocalDateTime, except: UUID?, ctx: MemberImportContext): Set<UUID> {
        val start = startedAt.atZone(OSLO).toInstant()
        return ctx.nearbyRounds
            .filter { r ->
                r.roundId != except && r.locationId == locationId &&
                    Duration.between(r.playedAt.atZone(OSLO).toInstant(), start).abs() <= SAME_ROUND_WINDOW
            }
            .flatMapTo(mutableSetOf()) { it.playerIds }
    }
}
