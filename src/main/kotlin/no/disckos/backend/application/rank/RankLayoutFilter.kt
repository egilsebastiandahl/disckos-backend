package no.disckos.backend.application.rank

import no.disckos.backend.application.admin.udiscimport.ImportNames
import no.disckos.backend.repository.LocationLayoutRepository
import org.springframework.stereotype.Component

/**
 * Leaves out rounds played on a layout that an admin has said doesn't count in the rank
 * ladder for its course (location_layouts, migration 017). Rounds without a layout, i.e.
 * rounds created in Disckos, and layouts nobody has decided on always count.
 *
 * Only ranks use this. Head-to-head compares players within the same round, so the
 * layout doesn't matter there.
 */
@Component
class RankLayoutFilter(private val locationLayoutRepository: LocationLayoutRepository) {

    fun ranked(rounds: List<PlayerCourseRound>): List<PlayerCourseRound> {
        if (rounds.none { it.layoutName != null }) return rounds
        val excluded = locationLayoutRepository.findByCountsForRankFalse()
            .map { it.locationId to it.layoutKey }
            .toSet()
        if (excluded.isEmpty()) return rounds
        return rounds.filter { r -> r.layoutName == null || (r.locationId to ImportNames.key(r.layoutName)) !in excluded }
    }
}
