package no.disckos.backend.application.memberimport

import no.disckos.backend.application.admin.udiscimport.ImportNames
import no.disckos.backend.application.admin.udiscimport.UdiscCard
import no.disckos.backend.repository.EventRepository
import no.disckos.backend.repository.HoleRepository
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.PlayerRepository
import no.disckos.backend.repository.PlayerScoreRepository
import no.disckos.backend.repository.ProfileRepository
import no.disckos.backend.repository.RoundRepository
import no.disckos.backend.repository.ScoreConfirmationRepository
import no.disckos.backend.repository.UdiscCourseAliasRepository
import no.disckos.backend.repository.UdiscImportedCardRepository
import no.disckos.backend.repository.UdiscPlayerAliasRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.util.UUID

/** The uploader, as far as member uploads care. */
data class Uploader(val profileId: UUID, val playerId: UUID, val playerName: String, val savedUdiscName: String?)

/** Reads what [MemberImportPlanner] needs to know about Disckos, in a fixed number of queries. */
@Component
class MemberImportContextLoader(
    private val profileRepository: ProfileRepository,
    private val playerRepository: PlayerRepository,
    private val locationRepository: LocationRepository,
    private val eventRepository: EventRepository,
    private val roundRepository: RoundRepository,
    private val holeRepository: HoleRepository,
    private val playerScoreRepository: PlayerScoreRepository,
    private val playerAliasRepository: UdiscPlayerAliasRepository,
    private val courseAliasRepository: UdiscCourseAliasRepository,
    private val importedCardRepository: UdiscImportedCardRepository,
    private val confirmationRepository: ScoreConfirmationRepository,
) {
    private val oslo: ZoneId = ZoneId.of("Europe/Oslo")

    /** Null when the profile isn't linked to a player: only players can upload rounds. */
    @Transactional(readOnly = true)
    fun uploader(profileId: UUID): Uploader? {
        val profile = profileRepository.findById(profileId).orElse(null) ?: return null
        val player = profile.player ?: return null
        return Uploader(profileId, player.id!!, player.name ?: "Ukjent", profile.udiscName)
    }

    @Transactional(readOnly = true)
    fun load(uploader: Uploader, cards: List<UdiscCard>): MemberImportContext {
        val players = playerRepository.findAll()
            .filter { it.id != null && !it.name.isNullOrBlank() }
            .associate { it.id!! to it.name!! }
        val locationIds = locationRepository.findAll().map { it.id }.toSet()
        val courseAliases = courseAliasRepository.findAll()
            .filter { it.locationId in locationIds }
            .associate { it.udiscCourse to it.locationId }
        val memberUdiscNames = profileRepository.findAll()
            .filter { it.id != uploader.profileId && !it.udiscName.isNullOrBlank() && it.player != null }
            .associate { ImportNames.key(it.udiscName!!) to it.player!!.id!! }

        return MemberImportContext(
            uploaderPlayerId = uploader.playerId,
            players = players,
            playerAliases = playerAliasRepository.findAll().associate { it.udiscName to it.playerId },
            memberUdiscNames = memberUdiscNames,
            courseAliases = courseAliases,
            eventLocationsByDate = eventRepository.findAll()
                .groupBy({ it.date.atZoneSameInstant(oslo).toLocalDate() }, { it.locationId }),
            importedCards = importedCards(cards.map { it.key }),
            nearbyRounds = nearbyRounds(cards, courseAliases),
        )
    }

    private fun importedCards(keys: List<String>): Map<String, ImportedCardState> {
        val records = importedCardRepository.findAllById(keys)
        if (records.isEmpty()) return emptyMap()
        val rounds = roundRepository.findAllById(records.map { it.roundId }).associateBy { it.id }
        val roundIds = rounds.keys
        val holeCounts = holeRepository.countHolesByRoundIds(roundIds).associate { it.roundId to it.holeCount.toInt() }
        val players = playerScoreRepository.findRoundPlayers(roundIds).groupBy({ it.roundId }, { it.playerId })
        val asked = confirmationRepository.findByRoundIdIn(roundIds).groupBy({ it.roundId }, { it.playerId })
        return records.mapNotNull { record ->
            val round = rounds[record.roundId] ?: return@mapNotNull null
            record.cardKey to ImportedCardState(
                roundId = round.id,
                eventId = round.eventId,
                holeCount = holeCounts[round.id] ?: 0,
                playerIds = players[round.id].orEmpty().toSet(),
                askedPlayerIds = asked[round.id].orEmpty().toSet(),
            )
        }.toMap()
    }

    /** Standalone rounds on the file's courses around the file's start times. */
    private fun nearbyRounds(cards: List<UdiscCard>, courseAliases: Map<String, UUID>): List<NearbyRound> {
        val locations = cards.mapNotNull { courseAliases[ImportNames.key(it.courseName)] }.toSet()
        val starts = cards.mapNotNull { it.startedAt }
        if (locations.isEmpty() || starts.isEmpty()) return emptyList()

        val window = MemberImportPlanner.SAME_ROUND_WINDOW
        val from = starts.min().minus(window).atZone(oslo).toOffsetDateTime()
        val to = starts.max().plus(window).atZone(oslo).toOffsetDateTime()
        val rounds = roundRepository.findByEventIdIsNullAndLocationIdInAndPlayedAtBetween(locations, from, to)
            .filter { it.locationId != null && it.playedAt != null }
        if (rounds.isEmpty()) return emptyList()

        val players = playerScoreRepository.findRoundPlayers(rounds.map { it.id }).groupBy({ it.roundId }, { it.playerId })
        return rounds.map { r ->
            NearbyRound(r.id, r.locationId!!, r.playedAt!!.atZoneSameInstant(oslo).toLocalDateTime(), players[r.id].orEmpty().toSet())
        }
    }
}
