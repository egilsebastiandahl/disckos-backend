package no.disckos.backend.application.event

import no.disckos.backend.domain.EventEntity
import no.disckos.backend.repository.EventRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetEventsHandler(
    private val eventRepository: EventRepository
) {
    @Transactional(readOnly = true)
    fun handle(query: GetEventsQuery = GetEventsQuery()): List<EventEntity> {
        val direction = if (query.sortAscending) Sort.Direction.ASC else Sort.Direction.DESC
        return eventRepository.findAll(Sort.by(direction, "date"))
    }
}