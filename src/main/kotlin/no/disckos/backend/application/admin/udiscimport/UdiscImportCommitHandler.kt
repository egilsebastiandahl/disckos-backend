package no.disckos.backend.application.admin.udiscimport

import no.disckos.backend.domain.LocationEntity
import no.disckos.backend.domain.Player
import no.disckos.backend.repository.LocationRepository
import no.disckos.backend.repository.PlayerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Writes the scorecards an admin confirmed in the preview.
 *
 * Deliberately *not* @Transactional: each card is written by [UdiscCardWriter] in its own
 * transaction, so one bad card (e.g. hole count mismatch) doesn't block the rest, and new
 * rounds get distinct created_at values (round order is created_at order, and Postgres'
 * now() is fixed for the whole transaction). Re-running a commit is safe: imported cards
 * are recorded, and players already in the target round are skipped.
 */
@Component
class UdiscImportCommitHandler(
    private val cardWriter: UdiscCardWriter,
    private val playerRepository: PlayerRepository,
    private val locationRepository: LocationRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val oslo: ZoneId = ZoneId.of("Europe/Oslo")

    fun handle(input: UdiscCommitInput): List<UdiscCardResult> {
        input.cards.forEach(::validate)

        val newPlayerIds = createRequestedPlayers(input)
        val newLocationIds = createRequestedLocations(input)
        val resolved = input.cards.map { card ->
            ResolvedCard(
                key = card.key,
                eventId = card.eventId,
                roundOrdinal = card.roundOrdinal,
                locationId = if (card.eventId != null) null
                else card.locationId ?: newLocationIds.getValue(ImportNames.key(card.newLocationName!!)),
                playedAt = card.startedAt?.atZone(oslo)?.toOffsetDateTime(),
                courseName = card.courseName,
                pars = card.pars,
                layoutName = card.layoutName?.trim()?.takeIf(String::isNotEmpty),
                layoutCountsForRank = card.layoutCountsForRank,
                players = card.players.map { p ->
                    ResolvedPlayer(
                        udiscName = p.udiscName,
                        playerId = p.playerId ?: newPlayerIds.getValue(ImportNames.key(p.newPlayerName!!)),
                        throws = p.throws,
                    )
                },
            )
        }

        // Event cards in round order, so "round 2 (new)" is created after "round 1 (new)".
        val results = arrayOfNulls<UdiscCardResult>(resolved.size)
        resolved.withIndex()
            .sortedWith(compareBy({ it.value.eventId == null }, { it.value.eventId }, { it.value.roundOrdinal }))
            .forEach { (i, card) -> results[i] = writeCard(card) }
        return results.map { it!! }
    }

    private fun writeCard(card: ResolvedCard): UdiscCardResult =
        try {
            cardWriter.write(card)
        } catch (ex: ResponseStatusException) {
            card.failed(ex.reason ?: ex.message)
        } catch (ex: IllegalArgumentException) {
            card.failed(ex.message ?: "Ugyldig kort")
        } catch (ex: Exception) {
            log.error("UDisc import of card {} failed", card.key, ex)
            card.failed("Uventet feil: ${ex.message ?: ex.javaClass.simpleName}")
        }

    private fun validate(card: UdiscCommitCard) {
        require(card.pars.isNotEmpty()) { "Kortet ${card.key} har ingen hull." }
        require(card.pars.all { it >= 1 }) { "Kortet ${card.key} har ugyldig par." }
        if (card.eventId != null) {
            require((card.roundOrdinal ?: 0) >= 1) { "Kortet ${card.key} mangler runde." }
        } else {
            require(card.startedAt != null) { "Kortet ${card.key} mangler tidspunkt." }
            require(card.locationId != null || !card.newLocationName.isNullOrBlank()) {
                "Kortet ${card.key} mangler lokasjon."
            }
        }
        card.players.forEach { p ->
            // The preview never offers these rows, so getting one means a stale or hand-made request.
            require(!ImportNames.isDoubles(p.udiscName)) { "«${p.udiscName}» er en double-runde. Det støttes ikke ennå." }
            require(p.throws.none { it == null }) { "«${p.udiscName}» fullførte ikke runden." }
            require(p.playerId != null || !p.newPlayerName.isNullOrBlank()) {
                "«${p.udiscName}» er ikke koblet til en spiller."
            }
            require(p.throws.size == card.pars.size) {
                "«${p.udiscName}» har ${p.throws.size} hull, kortet har ${card.pars.size}."
            }
            require(p.throws.all { it == null || it >= 1 }) { "«${p.udiscName}» har ugyldige kast." }
        }
    }

    /**
     * Creates each requested new player once, even if they appear on several cards.
     * Reuses a player with exactly the same name, so a double-submitted commit
     * doesn't create duplicates.
     */
    private fun createRequestedPlayers(input: UdiscCommitInput): Map<String, UUID> {
        val requested = input.cards.flatMap { it.players }
            .filter { it.playerId == null }
            .mapNotNull { it.newPlayerName?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy(ImportNames::key)
        if (requested.isEmpty()) return emptyMap()

        val existingByKey = playerRepository.findAll()
            .filter { it.id != null && !it.name.isNullOrBlank() }
            .associateBy { ImportNames.key(it.name!!) }

        return requested.associate { name ->
            val key = ImportNames.key(name)
            val id = existingByKey[key]?.id ?: playerRepository.save(
                Player().apply {
                    id = UUID.randomUUID()
                    this.name = name
                    createdAt = OffsetDateTime.now()
                    updatedAt = createdAt
                },
            ).id!!
            key to id
        }
    }

    /**
     * Same idea for courses that aren't Disckos locations yet. They're created without
     * coordinates (0, 0, which the admin location page lists as "uten koordinater").
     */
    private fun createRequestedLocations(input: UdiscCommitInput): Map<String, UUID> {
        val requested = input.cards
            .filter { it.eventId == null && it.locationId == null }
            .mapNotNull { it.newLocationName?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy(ImportNames::key)
        if (requested.isEmpty()) return emptyMap()

        val existingByKey = locationRepository.findAll().associateBy { ImportNames.key(it.name) }
        return requested.associate { name ->
            val key = ImportNames.key(name)
            val id = existingByKey[key]?.id ?: locationRepository.save(
                LocationEntity(name = name, description = "Opprettet fra UDisc-import", lat = 0.0, lon = 0.0),
            ).id
            key to id
        }
    }
}

data class ResolvedCard(
    val key: String,
    /** Null = standalone round. */
    val eventId: UUID?,
    val roundOrdinal: Int?,
    /** Standalone rounds only. */
    val locationId: UUID?,
    /** Standalone rounds only. */
    val playedAt: OffsetDateTime?,
    val courseName: String,
    val pars: List<Int>,
    val players: List<ResolvedPlayer>,
    val layoutName: String? = null,
    /** Null = leave the layout's rank setting as it is. */
    val layoutCountsForRank: Boolean? = null,
) {
    fun failed(message: String) = UdiscCardResult(
        key = key,
        eventId = eventId,
        roundId = null,
        roundOrdinal = roundOrdinal,
        createdRound = false,
        importedPlayers = emptyList(),
        skippedPlayers = emptyList(),
        error = message,
    )
}

data class ResolvedPlayer(val udiscName: String, val playerId: UUID, val throws: List<Int?>)
